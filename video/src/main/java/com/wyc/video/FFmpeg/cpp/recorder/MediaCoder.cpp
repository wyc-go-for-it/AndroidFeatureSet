//
// Created by Administrator on 2022/8/18.
//

#include "MediaCoder.h"
#include <utility>
#include "android/log.h"

static const char *TAG="MediaCoder";
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

MediaCoder::MediaCoder(std::string file,int width,int height,int frameRatio)
    :mFileName(std::move(file)),mWidth(width),mHeight(height),mFrameRatio(frameRatio){

    LOGD("MediaCoder construction");
}

MediaCoder::~MediaCoder() {
    LOGD("MediaCoder destruction");
    release();
}

void MediaCoder::init() {
    if (hasInit)return;

    const char *fileName = mFileName.c_str();
    const AVCodecID id = AVCodecID::AV_CODEC_ID_H264;
    const AVCodec *codec;

    int code = avformat_alloc_output_context2(&mFormatContext, nullptr, "mp4", fileName);
    if (code < 0){
        LOGE("Could not alloc format:%s",av_err2str(code));
        goto end;
    }

    mStream = avformat_new_stream(mFormatContext, nullptr);
    if (mStream == nullptr) {
        LOGE("Could not allocate stream");
        goto end;
    }
    mStream->time_base = {1,mFrameRatio};

    if (!(mFormatContext->oformat->flags & AVFMT_NOFILE)) {
        code = avio_open(&mFormatContext->pb,fileName, AVIO_FLAG_WRITE);
        if (code < 0) {
            LOGE("Could not open '%s': %s\n", fileName,av_err2str(code));
            goto end;
        }
    }

    codec = avcodec_find_encoder(id);
    if (codec == nullptr){
        LOGE("find %d encoder error",id);
        goto end;
    }

    mCodecContext = avcodec_alloc_context3(codec);
    if (mCodecContext == nullptr){
        LOGE("alloc context error");
        goto end;
    }

    mCodecContext->bit_rate = mWidth * mHeight * 4;
    mCodecContext->width = mWidth;
    mCodecContext->height = mHeight;
    mCodecContext->framerate = {mFrameRatio, 1};
    mCodecContext->time_base = {1,mFrameRatio};
    mCodecContext->gop_size = 10;
    mCodecContext->max_b_frames = 1;
    mCodecContext->pix_fmt = AV_PIX_FMT_NV21;

    if (codec->id == AV_CODEC_ID_H264)
        av_opt_set(mCodecContext->priv_data, "preset", "slow", 0);

    code = avcodec_open2(mCodecContext, codec, nullptr);
    if (code < 0) {
        LOGE("Could not open codec: %s", av_err2str(code));
        goto end;
    }

    mPacket = av_packet_alloc();
    if (mPacket == nullptr){
        LOGE("alloc packet error");
        goto end;
    }

    mFrame = av_frame_alloc();
    if (mFrame == nullptr){
        LOGE("alloc frame error");
        goto end;
    }

    mFrame->format = mCodecContext->pix_fmt;
    mFrame->width  = mCodecContext->width;
    mFrame->height = mCodecContext->height;

    code = av_frame_get_buffer(mFrame,0);
    if (code < 0){
        LOGE("frame get buffer error:%s",av_err2str(code));
        goto end;
    }
    hasInit = true;

    end:{
        release();
        hasInit = false;
    }
}

bool MediaCoder::encode(const uint8_t *data, __int64_t presentationTime) {
    if (hasInit){
        int code = av_frame_make_writable(mFrame);
        if (code < 0){
            LOGE("frame make writable error:%s",av_err2str(code));
            return false;
        }



        mFrame->pts = presentationTime;
        return encode();
    }
    return false;
}

bool MediaCoder::encode() {
    LOGD("send frame %ld encoded",mFrame->pts);

    int code = avcodec_send_frame(mCodecContext,mFrame);
    if (code < 0){
        LOGE("send frame for encoding error");
        return false;
    }

    while (code >= 0){
        code = avcodec_receive_packet(mCodecContext, mPacket);
        if (code == AVERROR(EAGAIN) || code == AVERROR_EOF)
            return true;
        else if (code < 0) {
            LOGE("Error during encoding:%s",av_err2str(code));
            return false;
        }

        if (!writeFrame()){
            return false;
        }

        //av_packet_unref(mPacket);
    }

    return true;
}

bool MediaCoder::writeFrame() {
    av_packet_rescale_ts(mPacket, mCodecContext->time_base, mStream->time_base);
    mPacket->stream_index = mStream->index;

    log_packet(mFormatContext, mPacket);

    int code = av_interleaved_write_frame(mFormatContext, mPacket);
    /* pkt is now blank (av_interleaved_write_frame() takes ownership of
     * its contents and resets pkt), so that no unreferencing is necessary.
     * This would be different if one used av_write_frame(). */
    if (code < 0) {
        LOGE("Error while writing output packet: %s", av_err2str(code));
        return false;
    }
    return true;
}

 void MediaCoder::log_packet(const AVFormatContext *fmt_ctx, const AVPacket *pkt)
{
    AVRational *time_base = &fmt_ctx->streams[pkt->stream_index]->time_base;
    LOGD("pts:%s pts_time:%s dts:%s dts_time:%s duration:%s duration_time:%s stream_index:%d\n",
           av_ts2str(pkt->pts), av_ts2timestr(pkt->pts, time_base),
           av_ts2str(pkt->dts), av_ts2timestr(pkt->dts, time_base),
           av_ts2str(pkt->duration), av_ts2timestr(pkt->duration, time_base),
           pkt->stream_index);
}
