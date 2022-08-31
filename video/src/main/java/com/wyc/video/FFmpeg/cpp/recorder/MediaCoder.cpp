//
// Created by Administrator on 2022/8/18.
//

#include "MediaCoder.h"
#include <utility>

MediaCoder::MediaCoder(std::string file,int width,int height,int frameRatio)
    :mFileName(std::move(file)),mWidth(width),mHeight(height),mFrameRatio(frameRatio){
    LOGD("MediaCoder construction file:%s,width:%d,height:%d,frameRatio:%d",mFileName.c_str(),width,height,frameRatio);
}

MediaCoder::~MediaCoder() {
    LOGD("MediaCoder destruction queue:%d",m_queue.size());
    release();
}

void MediaCoder::init() {
    if (hasInit)return;

    const char *fileName = mFileName.c_str();

    int code = avformat_alloc_output_context2(&mFormatContext, nullptr, "mp4", fileName);
    if (code < 0){
        LOGE("Could not alloc format:%s",av_err2str(code));
        release();
        return;
    }


    mStream = avformat_new_stream(mFormatContext, nullptr);
    if (mStream == nullptr) {
        LOGE("Could not allocate stream");
        release();
        return;
    }
    mStream->time_base = {1,mFrameRatio};

    if (!(mFormatContext->oformat->flags & AVFMT_NOFILE)) {
        code = avio_open(&mFormatContext->pb,fileName, AVIO_FLAG_WRITE | AVIO_FLAG_READ);
        if (code < 0) {
            LOGE("Could not open '%s': %s\n", fileName,av_err2str(code));
            release();
            return;
        }
    }
    const AVCodecID id = AVCodecID::AV_CODEC_ID_H264;
    const AVCodec *codec = avcodec_find_encoder(id);
    if (codec == nullptr){
        LOGE("find %d encoder error",id);
        release();
        return;
    }

    mCodecContext = avcodec_alloc_context3(codec);
    if (mCodecContext == nullptr){
        LOGE("alloc context error");
        release();
        return;
    }

    mCodecContext->bit_rate = mWidth * mHeight * 4;
    mCodecContext->width = mWidth;
    mCodecContext->height = mHeight;
    mCodecContext->framerate = {mFrameRatio, 1};
    mCodecContext->time_base = {1,mFrameRatio};
    mCodecContext->gop_size = 5;
    mCodecContext->max_b_frames = 10;
    mCodecContext->pix_fmt = AV_PIX_FMT_YUV420P;

    if (codec->id == AV_CODEC_ID_H264)
        av_opt_set(mCodecContext->priv_data, "preset", "faster", 0);

    code = avcodec_open2(mCodecContext, codec, nullptr);
    if (code < 0) {
        LOGE("Could not open codec: %s", av_err2str(code));
        release();
        return;
    }

    code = avcodec_parameters_from_context(mStream->codecpar, mCodecContext);
    if (code < 0) {
        LOGE("avcodec_parameters_from_context error: %s", av_err2str(code));
        release();
        return;
    }

    mPacket = av_packet_alloc();
    if (mPacket == nullptr){
        LOGE("alloc packet error");
        release();
        return;
    }

    mFrame = av_frame_alloc();
    if (mFrame == nullptr){
        LOGE("alloc frame error");
        release();
        return;
    }

    mFrame->format = mCodecContext->pix_fmt;
    mFrame->width  = mCodecContext->width;
    mFrame->height = mCodecContext->height;

    code = av_frame_get_buffer(mFrame,0);
    if (code < 0){
        LOGE("frame get buffer error:%s",av_err2str(code));
        release();
        return;
    }
    hasInit = true;
}

bool MediaCoder::encode(const NativeImage& data, __int64_t presentationTime) {
    if (hasInit){
        int code = av_frame_make_writable(mFrame);
        if (code < 0){
            LOGE("frame make writable error:%s",av_err2str(code));
            return false;
        }
        mFrame->data[0] = data.getPlanePtr0();
        mFrame->data[1] = data.getPlanePtr1();
        mFrame->data[2] = data.getPlanePtr2();

        mFrame->linesize[0] = data.getPLineSize0();
        mFrame->linesize[1] = data.getPLineSize1();
        mFrame->linesize[2] = data.getPLineSize2();

        mFrame->pts = presentationTime;

        return encode();
    }
    return false;
}



bool MediaCoder::encode() {
    LOGD("send frame %p pts %ld encoded",mFrame->data[0],mFrame->pts);

    int code = avcodec_send_frame(mCodecContext,mFrame);
    if (code < 0){
        LOGE("send frame for encoding error:%s",av_err2str(code));
        return false;
    }

    while (code >= 0){
        code = avcodec_receive_packet(mCodecContext, mPacket);
        if (code == AVERROR(EAGAIN) || code == AVERROR_EOF) {
            LOGE("continue:%s",av_err2str(code));
            return true;
        }else if (code < 0) {
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
     /*pkt is now blank (av_interleaved_write_frame() takes ownership of
     * its contents and resets pkt), so that no unreferencing is necessary.
     * This would be different if one used av_write_frame().*/
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

void MediaCoder::start() {
    init();
    if (hasInit){
        m_encodeThread = thread([this]()->void{
            int ret = avformat_write_header(mFormatContext,nullptr);
            if (ret < 0){
                LOGE("avformat_write_header error:%s",av_err2str(ret));
                return;
            }
            LOGD("MediaCoder start encode video");

            hasStarted = true;
            NativeImage image;
            __int64_t time = 0;
            __int64_t interval = 1;

            while (hasStarted){
                bool code = m_queue.take(image,100);
                if (code){
                    encode(image,time += interval);
                }
            }
            if (hasStopped){
                LOGD("MediaCoder stop encode video");
                av_write_trailer(mFormatContext);
            }
       });
    }
}

void MediaCoder::stop() {
    if (hasInit){
        hasStarted = false;
        hasStopped = true;

        m_queue.notify_all();

        if (m_encodeThread.joinable()){
            LOGD("wait encode thread...");
            m_encodeThread.join();
            LOGD("encode thread exit...");
        }
    }
    release();
}

void MediaCoder::addData(NativeImage& data) {
    if (hasInit)
        m_queue.push(data);
}

