#include "MediaCoder.h"
#include <utility>

MediaCoder::MediaCoder(std::string file,int width,int height,int frameRatio)
    :mFileName(std::move(file)),m_videoHandle(new VideoHandle(width,height,frameRatio)),
    m_audioHandle(new AudioHandle()){
    LOGD("MediaCoder construction file:%s,width:%d,height:%d,frameRatio:%d",mFileName.c_str(),width,height,frameRatio);
}

MediaCoder::~MediaCoder() {
    LOGD("%s has executed,queue:%d",__FUNCTION__,m_queue.size());
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

    if (!m_videoHandle->initVideo(mFormatContext,fileName)){
        return;
    }
    if (!m_audioHandle->initAudio(mFormatContext,fileName)){
        return;
    }

    hasInit = true;
}


void MediaCoder::start() {
    init();
    if (hasInit){

        m_audioHandle->setDataCallback([this](const float *data,int numSamples)->bool {
            return m_audioHandle->encode(mFormatContext, (uint8_t *) data, numSamples * sizeof(float ));
        });

        /*m_encodeThread = thread([this]()->void{
            int ret = avformat_write_header(mFormatContext,nullptr);
            if (ret < 0){
                LOGE("avformat_write_header error:%s",av_err2str(ret));
                return;
            }
            LOGD("MediaCoder start encode video");

            encoding = true;
            NativeImage image;
            __int64_t time = 0;
            __int64_t interval = 1;

            while (encoding){
                bool code = m_queue.take(image);
                if (code){
                    m_videoHandle->encode(mFormatContext,image,time += interval);
                }
            }
            LOGD("MediaCoder stop encode video");
            if (time > 0){
                av_write_trailer(mFormatContext);
            } else{
                remove(mFileName.c_str());
            }
       });*/
    }
}

void MediaCoder::stop() {
    if (hasInit){
        m_audioHandle->stop();

        if (!encoding){
            remove(mFileName.c_str());
        }else
            encoding = false;

        m_queue.exit();
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

void MediaCoder::addData(NativeImage &&data) {
    if (hasInit)
        m_queue.push(std::move(data));
}

