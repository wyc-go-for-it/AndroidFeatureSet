//
// Created by Administrator on 2022/11/1.
//

#ifndef ANDROIDFEATURESET_AUDIOHANDLE_H
#define ANDROIDFEATURESET_AUDIOHANDLE_H


#include "../utils/LogUtil.h"
#include "IMediaHandle.h"
#include "../audio/IAudioEngine.h"
#include "../audio/AudioEngine.h"
#include "../audio/AudioOpenSL.h"



class  AudioHandle final :IMediaHandle{
    friend class MediaCoder;
private:
    DISABLE_COPY_ASSIGN(AudioHandle)
    AudioHandle():m_audioEngine(new AudioOpenSL()){
        m_audioEngine->open();
        m_audioEngine->start();
        m_audioEngine->setRecording(true);
    };
    ~AudioHandle(){
        if (m_audioEngine != nullptr){
            m_audioEngine->stop();

            delete m_audioEngine;
            m_audioEngine = nullptr;
        }

        printClassName<AudioHandle>(this);
    }

    void stop(){
        if (m_audioEngine != nullptr){
            m_audioEngine->stop();
        }
    }

    void setDataCallback(std::function<bool(const float *,int32_t numFrames)> fun ){
        m_audioEngine->setDataCallback(fun);
    }

    bool encode(AVFormatContext *s,uint8_t * data,int  numSamples) {

        int code = av_frame_make_writable(mFrame);
        if (code < 0){
            LOGE("frame make writable error:%s",av_err2str(code));
            return false;
        }

        if (numSamples > mFrame->linesize[0]){
            int linesize = mFrame->linesize[0];
            int count = numSamples / linesize;
            int mod = numSamples % linesize;
            int cur = -1;
            while (cur++< count){
                memcpy(mFrame->data[0],data + cur * linesize,linesize);

                mFrame->pts =  av_rescale_q(samples_count, (AVRational){1, mCodecContext->sample_rate}, mCodecContext->time_base);
                samples_count += 1024;

                if (!encodeActually(s)){
                    return false;
                }
            }
            if (mod != 0){
                memcpy(mFrame->data[0],data + (count - 1) * linesize,mod);

                mFrame->pts =  av_rescale_q(samples_count, (AVRational){1, mCodecContext->sample_rate}, mCodecContext->time_base);
                samples_count += 1024;
            }
        }else{
            memcpy(mFrame->data[0],data,numSamples);

            mFrame->pts =  av_rescale_q(samples_count, (AVRational){1, mCodecContext->sample_rate}, mCodecContext->time_base);
            samples_count += 1024;

            return encodeActually(s);
        }
    }

    bool initAudio(AVFormatContext *s,const char *fileName);
    struct SwrContext *swr_ctx;
    IAudioEngine *m_audioEngine = nullptr;

    int samples_count = 1024;

};


#endif //ANDROIDFEATURESET_AUDIOHANDLE_H
