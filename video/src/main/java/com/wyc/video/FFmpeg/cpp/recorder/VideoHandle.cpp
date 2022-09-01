#include "VideoHandle.h"
bool VideoHandle::initVideo(AVFormatContext *s,const char *fileName) {
    mStream = avformat_new_stream(s, nullptr);
    if (mStream == nullptr) {
        LOGE("Could not allocate stream");
        release();
        return false;
    }
    mStream->time_base = {1,mFrameRatio};
    int code;
    if (!(s->oformat->flags & AVFMT_NOFILE)) {
        code = avio_open(&s->pb,fileName, AVIO_FLAG_WRITE | AVIO_FLAG_READ);
        if (code < 0) {
            LOGE("Could not open '%s': %s\n", fileName,av_err2str(code));
            release();
            return false;
        }
    }
    const AVCodecID id = AVCodecID::AV_CODEC_ID_H264;
    const AVCodec *codec = avcodec_find_encoder(id);
    if (codec == nullptr){
        LOGE("find %d encoder error",id);
        release();
        return false;
    }

    mCodecContext = avcodec_alloc_context3(codec);
    if (mCodecContext == nullptr){
        LOGE("alloc context error");
        release();
        return false;
    }

    mCodecContext->bit_rate = mWidth * mHeight * 4;
    mCodecContext->width = mWidth;
    mCodecContext->height = mHeight;
    mCodecContext->framerate = {mFrameRatio, 1};
    mCodecContext->time_base = {1,mFrameRatio};
    mCodecContext->gop_size = 10;
    mCodecContext->max_b_frames = 1;
    mCodecContext->pix_fmt = AV_PIX_FMT_YUV420P;

    if (codec->id == AV_CODEC_ID_H264){
        av_opt_set(mCodecContext->priv_data, "preset", "superfast", 0);
    }

    code = avcodec_open2(mCodecContext, codec, nullptr);
    if (code < 0) {
        LOGE("Could not open codec: %s", av_err2str(code));
        release();
        return false;
    }

    code = avcodec_parameters_from_context(mStream->codecpar, mCodecContext);
    if (code < 0) {
        LOGE("avcodec_parameters_from_context error: %s", av_err2str(code));
        release();
        return false;
    }

    mPacket = av_packet_alloc();
    if (mPacket == nullptr){
        LOGE("alloc packet error");
        release();
        return false;
    }

    mFrame = av_frame_alloc();
    if (mFrame == nullptr){
        LOGE("alloc frame error");
        release();
        return false;
    }

    mFrame->format = mCodecContext->pix_fmt;
    mFrame->width  = mCodecContext->width;
    mFrame->height = mCodecContext->height;

    code = av_frame_get_buffer(mFrame,0);
    if (code < 0){
        LOGE("frame get buffer error:%s",av_err2str(code));
        release();
        return false;
    }

    return true;
}
