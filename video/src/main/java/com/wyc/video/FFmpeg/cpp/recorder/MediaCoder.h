
#ifndef ANDROIDFEATURESET_MEDIACODER_H
#define ANDROIDFEATURESET_MEDIACODER_H

#include <string>
#include "../utils/ImageDef.h"
#include "../utils/MacroUtil.h"
#include "../thread/SyncQueue.h"
extern "C" {
    #include "libavcodec/avcodec.h"
    #include "libavformat/avformat.h"
    #include "libavutil/opt.h"
    #include "libavutil/error.h"
    #include "libavutil/timestamp.h"
};

class MediaCoder final{
private:
    DISABLE_COPY_ASSIGN(MediaCoder);
    void release(){
        hasInit = false;

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

        if (mFormatContext != nullptr){
            if (!(mFormatContext->oformat->flags & AVFMT_NOFILE)){
                avio_closep(&mFormatContext->pb);
            }
            avformat_free_context(mFormatContext);
            mFormatContext = nullptr;
        }

    }

    bool encode();
    bool writeFrame();
    static void log_packet(const AVFormatContext *fmt_ctx, const AVPacket *pkt);
    void init();
    bool encode(const NativeImage data,__int64_t presentationTime);
public:
    MediaCoder():MediaCoder("",0,0,0){

    }
    MediaCoder(std::string file,int width,int height,int frameRatio);
    ~MediaCoder();
    void start();
private:
    SyncQueue<NativeImage> m_queue;
    bool hasInit = false;

    volatile bool hasStarted = false;
    volatile bool hasPaused = false;
    volatile bool hasStopped = true;

    const std::string mFileName;
    const int mWidth,mHeight,mFrameRatio;
    AVCodecContext *mCodecContext = nullptr;
    AVFrame *mFrame = nullptr;
    AVPacket *mPacket = nullptr;
    AVFormatContext *mFormatContext = nullptr;
    AVStream *mStream = nullptr;
};


#endif //ANDROIDFEATURESET_MEDIACODER_H
