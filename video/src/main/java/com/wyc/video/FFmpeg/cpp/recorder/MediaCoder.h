
#ifndef ANDROIDFEATURESET_MEDIACODER_H
#define ANDROIDFEATURESET_MEDIACODER_H

#include <string>
#include "../utils/ImageDef.h"
#include "../utils/MacroUtil.h"
#include "../thread/SyncQueue.h"
#include "VideoHandle.h"

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
        encoding = false;
        hasInit = false;

        delete m_videoHandle;
        m_videoHandle = nullptr;

        if (mFormatContext != nullptr){
            if (!(mFormatContext->oformat->flags & AVFMT_NOFILE)){
                avio_closep(&mFormatContext->pb);
            }
            avformat_free_context(mFormatContext);
            mFormatContext = nullptr;
        }
        LOGD("MediaCoder has released resource");
    }

    void init();
public:
    MediaCoder(std::string file,int width,int height,int frameRatio);
    ~MediaCoder();
    int getVideoWidth() const{
        return m_videoHandle->mWidth;
    }
    int getVideoHeight() const{
        return m_videoHandle->mHeight;
    }
    void start();
    void stop();
    void addData(NativeImage& data);

private:
    SyncQueue<NativeImage> m_queue;
    volatile bool hasInit = false;

    volatile bool encoding = false;
    thread m_encodeThread;

    const std::string mFileName;

    AVFormatContext *mFormatContext = nullptr;

    VideoHandle * m_videoHandle;

};

#endif //ANDROIDFEATURESET_MEDIACODER_H
