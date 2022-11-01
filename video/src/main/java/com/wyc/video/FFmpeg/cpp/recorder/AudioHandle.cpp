//
// Created by Administrator on 2022/11/1.
//

#include "AudioHandle.h"

bool AudioHandle::initAudio(AVFormatContext *s, const char *fileName) {
    mStream = avformat_new_stream(s, nullptr);
    if (mStream == nullptr) {
        LOGE("Could not allocate stream");
        release();
        return false;
    }

    int code;
    if (!(s->oformat->flags & AVFMT_NOFILE)) {
        code = avio_open(&s->pb,fileName, AVIO_FLAG_WRITE | AVIO_FLAG_READ);
        if (code < 0) {
            LOGE("Could not open '%s': %s\n", fileName,av_err2str(code));
            release();
            return false;
        }
    }

    const AVCodecID id = AVCodecID::AV_CODEC_ID_AAC;
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


    mCodecContext->codec_type = AVMEDIA_TYPE_AUDIO;

    mCodecContext->sample_fmt = AV_SAMPLE_FMT_FLTP;
    mCodecContext->sample_rate = 48000;
    auto ch = (AVChannelLayout)AV_CHANNEL_LAYOUT_STEREO;
    av_channel_layout_copy(&mCodecContext->ch_layout,&ch);

    mStream->time_base = (AVRational){ 1, mCodecContext->sample_rate };


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

    mFrame->format = mCodecContext->sample_fmt;
    av_channel_layout_copy(&mFrame->ch_layout, &ch);
    mFrame->sample_rate = mCodecContext->sample_rate;
    mFrame->nb_samples = mCodecContext->frame_size;

    code = av_frame_get_buffer(mFrame,0);
    if (code < 0){
        LOGE("frame get buffer error:%s",av_err2str(code));
        release();
        return false;
    }

    swr_ctx = swr_alloc();
    if (!swr_ctx) {
        LOGE("Could not allocate resampler context");
        release();
        return false;
    }

    av_opt_set_chlayout  (swr_ctx, "in_chlayout",       &mCodecContext->ch_layout,      0);
    av_opt_set_int       (swr_ctx, "in_sample_rate",     mCodecContext->sample_rate,    0);
    av_opt_set_sample_fmt(swr_ctx, "in_sample_fmt",      AV_SAMPLE_FMT_S16, 0);
    av_opt_set_chlayout  (swr_ctx, "out_chlayout",      &mCodecContext->ch_layout,      0);
    av_opt_set_int       (swr_ctx, "out_sample_rate",    mCodecContext->sample_rate,    0);
    av_opt_set_sample_fmt(swr_ctx, "out_sample_fmt",     mCodecContext->sample_fmt,     0);

    if ((code = swr_init(swr_ctx)) < 0) {
        LOGE("Failed to initialize the resampling context error:%s",av_err2str(code));
        release();
        return false;
    }

    return true;
}
