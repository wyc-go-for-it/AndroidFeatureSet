//
// Created by Administrator on 2022/9/1.
//

#ifndef ANDROIDFEATURESET_VIDEOHANDLE_H
#define ANDROIDFEATURESET_VIDEOHANDLE_H

#include "VideoHandle.h"
#include "../utils/LogUtil.h"
#include "../utils/MacroUtil.h"
#include "../utils/ImageDef.h"

extern "C" {
    #include "libavcodec/avcodec.h"
    #include "libavformat/avformat.h"
    #include "libavutil/opt.h"
    #include "libavutil/error.h"
    #include "libavutil/timestamp.h"
}

static void log_packet(const AVFormatContext *fmt_ctx, const AVPacket *pkt)
{
    AVRational *time_base = &fmt_ctx->streams[pkt->stream_index]->time_base;
    LOGD("pts:%s pts_time:%s dts:%s dts_time:%s duration:%s duration_time:%s stream_index:%d\n",
         av_ts2str(pkt->pts), av_ts2timestr(pkt->pts, time_base),
         av_ts2str(pkt->dts), av_ts2timestr(pkt->dts, time_base),
         av_ts2str(pkt->duration), av_ts2timestr(pkt->duration, time_base),
         pkt->stream_index);
}

class VideoHandle final {
    friend class MediaCoder;
private:
    DISABLE_COPY_ASSIGN(VideoHandle)
    VideoHandle(int width,int height,int frameRatio):mWidth(width),mHeight(height),mFrameRatio(frameRatio){
    }

    ~VideoHandle(){
        release();
    }

    void release(){
        if (mPacket != nullptr){
            av_packet_free(&mPacket);
            mPacket = nullptr;
        }

        if (mFrame != nullptr){
            av_frame_free(&mFrame);
            mFrame = nullptr;
        }

        if (mCodecContext != nullptr){
            avcodec_free_context(&mCodecContext);
            mCodecContext = nullptr;
        }

        if (mStream != nullptr){
            mStream = nullptr;
        }
        LOGD("VideoHandle has released");
    }
    bool initVideo(AVFormatContext *s,const char *fileName);
    bool encode(AVFormatContext *s,const NativeImage& data, __int64_t presentationTime) {
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

        return encode(s);
    }
    bool encode(AVFormatContext *s) {
        LOGD("send frame %p pts %ld encoded",mFrame->data[0],mFrame->pts);

        int code = avcodec_send_frame(mCodecContext,mFrame);
        if (code < 0){
            LOGE("send frame for encoding error:%s",av_err2str(code));
            return false;
        }

        while (code >= 0){
            code = avcodec_receive_packet(mCodecContext,mPacket);
            if (code == AVERROR(EAGAIN) || code == AVERROR_EOF) {
                LOGE("receive_packet continue:%s",av_err2str(code));
                return true;
            }else if (code < 0) {
                LOGE("Error during encoding:%s",av_err2str(code));
                return false;
            }

            if (!writeFrame(s)){
                return false;
            }

            //av_packet_unref(mPacket);
        }

        return true;
    }
    bool writeFrame(AVFormatContext *s) {
        av_packet_rescale_ts(mPacket, mCodecContext->time_base,mStream->time_base);
        mPacket->stream_index = mStream->index;

        log_packet(s,mPacket);

        int code = av_interleaved_write_frame(s,mPacket);
        /*pkt is now blank (av_interleaved_write_frame() takes ownership of
        * its contents and resets pkt), so that no unreferencing is necessary.
        * This would be different if one used av_write_frame().*/
        if (code < 0) {
            LOGE("Error while writing output packet: %s", av_err2str(code));
            return false;
        }

        return true;
    }

public:
    const int mWidth,mHeight,mFrameRatio;

private:
    AVCodecContext *mCodecContext = nullptr;
    AVFrame *mFrame = nullptr;
    AVPacket *mPacket = nullptr;
    AVStream *mStream = nullptr;
};

#endif //ANDROIDFEATURESET_VIDEOHANDLE_H
