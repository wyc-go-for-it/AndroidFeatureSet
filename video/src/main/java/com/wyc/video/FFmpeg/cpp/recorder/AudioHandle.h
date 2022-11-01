//
// Created by Administrator on 2022/11/1.
//

#ifndef ANDROIDFEATURESET_AUDIOHANDLE_H
#define ANDROIDFEATURESET_AUDIOHANDLE_H


#include "../utils/LogUtil.h"
#include "IMediaHandle.h"
#include "../audio/IAudioEngine.h"
#include "../audio/AudioEngine.h"

class  AudioHandle final :IMediaHandle{
    friend class MediaCoder;
private:
    DISABLE_COPY_ASSIGN(AudioHandle)
    AudioHandle():m_audioEngine(new AudioEngine()){
        m_audioEngine->setDataCallback([](const void *data,int  numSamples)->void {
            const float * d = reinterpret_cast<const float *>(data);
            int i = 0;
            for (; i < numSamples; ++i) {
                float f = d[i];
                LOGE("audio data %f",f);
            }

        });
        m_audioEngine->open();
        m_audioEngine->setRecording(true);
        m_audioEngine->start();
    };
    ~AudioHandle(){
        if (m_audioEngine != nullptr){
            m_audioEngine->stop();

            delete m_audioEngine;
            m_audioEngine = nullptr;
        }
        LOGD("%s has released",typeid(*this).name());
    }

    bool encode(AVFormatContext *s,uint8_t * data, __int64_t presentationTime) {
        int code = av_frame_make_writable(mFrame);
        if (code < 0){
            LOGE("frame make writable error:%s",av_err2str(code));
            return false;
        }
        mFrame->data[0] = data;

        mFrame->pts = presentationTime;

        return encodeActually(s);
    }

    bool initAudio(AVFormatContext *s,const char *fileName);
    struct SwrContext *swr_ctx;
    IAudioEngine *m_audioEngine = nullptr;
};


#endif //ANDROIDFEATURESET_AUDIOHANDLE_H
