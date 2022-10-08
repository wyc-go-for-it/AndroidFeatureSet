//
// Created by Administrator on 2022-09-18.
//

#ifndef WYC_AUDIOOPENSL_H
#define WYC_AUDIOOPENSL_H


#include "IAudioEngine.h"
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <mutex>
#include "SoundRecording.h"

class AudioOpenSL: public IAudioEngine{
public:
    DISABLE_COPY_ASSIGN(AudioOpenSL)
    AudioOpenSL(): m_recordingBuffer(malloc(bufferSize)), m_playBuffer(malloc(bufferSize)){

    };
    ~AudioOpenSL(){
        LOGD("%s",__FUNCTION__ );
        release();
    }

    virtual int open();
    virtual int start();
    virtual int pausePlayback();
    virtual int resumePlayback();
    virtual int stop();

    virtual void setRecording(bool isRecording);
    virtual void setPlaying(bool isPlaying);
    virtual void setLooping(bool isOn);

    static void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context)
    {

        if (context != nullptr){
            AudioOpenSL *c = reinterpret_cast<AudioOpenSL *>(context);
            if (bq == c->m_bqPlayerBufferQueue){
                std::lock_guard<std::recursive_mutex> lock(c->m_audioEngineLock);

                u_int32_t frames = c->mSoundRecording.read(static_cast<float *>(c->m_playBuffer),c->bufferSize / 4);

                SLresult result = (*c->m_bqPlayerBufferQueue)->Enqueue(c->m_bqPlayerBufferQueue, c->m_playBuffer , c->bufferSize);
                LOGE("PlayerCallback Enqueue result:%d,frames:%d",result,frames);
            }
        }
    }

    static void bqRecorderCallback(SLAndroidSimpleBufferQueueItf bq, void *context)
    {
        if (context != nullptr){
            AudioOpenSL *c = reinterpret_cast<AudioOpenSL *>(context);
            if (bq == c->m_recorderBufferQueue){
                std::lock_guard<std::recursive_mutex> lock(c->m_audioEngineLock);

                c->mSoundRecording.write(static_cast<const float *>(c->m_recordingBuffer), c->bufferSize / 4);

                SLresult result = (*c->m_recorderBufferQueue)->Enqueue(c->m_recorderBufferQueue, c->m_recordingBuffer , c->bufferSize);
                LOGE("RecorderCallback Enqueue result:%d",result);
            }
        }
    }

private:
    void release(){
        // destroy buffer queue audio player object, and invalidate all associated interfaces
        if (m_bqPlayerObject != nullptr) {
            (*m_bqPlayerObject)->Destroy(m_bqPlayerObject);
            m_bqPlayerObject = nullptr;
            m_bqPlayerPlay = nullptr;
            m_bqPlayerBufferQueue = nullptr;
            m_bqPlayerEffectSend = nullptr;
            m_bqPlayerMuteSolo = nullptr;
            m_bqPlayerVolume = nullptr;
        }

        // destroy audio recorder object, and invalidate all associated interfaces
        if (m_recorderObject != nullptr) {
            (*m_recorderObject)->Destroy(m_recorderObject);
            m_recorderObject = nullptr;
            m_recorderRecord = nullptr;
            m_recorderBufferQueue = nullptr;
        }

        // destroy output mix object, and invalidate all associated interfaces
        if (m_outputMixObject != nullptr) {
            (*m_outputMixObject)->Destroy(m_outputMixObject);
            m_outputMixObject = nullptr;
            m_outputMixEnvironmentalReverb = nullptr;
        }

        // destroy engine object, and invalidate all associated interfaces
        if (m_engineObject != nullptr) {
            (*m_engineObject)->Destroy(m_engineObject);
            m_engineObject = nullptr;
            m_engineEngine = nullptr;
        }

        if (m_recordingBuffer != nullptr){
            free(m_recordingBuffer);
            m_recordingBuffer = nullptr;
        }

        if (m_playBuffer != nullptr){
            free(m_playBuffer);
            m_playBuffer = nullptr;
        }
    }

    int createEngine();
    int createBufferPlayer();
    int createRecordingAudio();

    int setPlayCallback();
    int setRecordingCallback();

    int startRecordingAudio();

    int pauseRecordingAudio(){
        if (m_recorderRecord != nullptr){
           SLresult result = (*m_recorderRecord)->SetRecordState(m_recorderRecord, SL_RECORDSTATE_PAUSED);
            if (result != SL_RESULT_SUCCESS){
                LOGE("RecorderRecord pause error");
                return -1;
            }
        }
        return 0;
    }

private:
    std::recursive_mutex m_audioEngineLock;
    static constexpr int bufferSize = 44100 * 1 * 4;

    // engine interfaces
    SLObjectItf m_engineObject = nullptr;
    SLEngineItf m_engineEngine = nullptr;

    // output mix interfaces
    SLObjectItf m_outputMixObject = nullptr;
    SLEnvironmentalReverbItf m_outputMixEnvironmentalReverb = nullptr;

    // recorder interfaces
    SLObjectItf m_recorderObject = nullptr;
    SLRecordItf m_recorderRecord = nullptr;
    SLAndroidSimpleBufferQueueItf m_recorderBufferQueue = nullptr;
    void * m_recordingBuffer = nullptr;

    // buffer queue player interfaces
    SLObjectItf m_bqPlayerObject = nullptr;
    SLPlayItf m_bqPlayerPlay = nullptr;
    SLAndroidSimpleBufferQueueItf m_bqPlayerBufferQueue = nullptr;
    SLEffectSendItf m_bqPlayerEffectSend = nullptr;
    SLMuteSoloItf m_bqPlayerMuteSolo = nullptr;
    SLVolumeItf m_bqPlayerVolume = nullptr;
    u_int32_t m_bqPlayerSampleRate = 48000 * 1000;
    void * m_playBuffer = nullptr;


    SoundRecording mSoundRecording;
};


#endif //WYC_AUDIOOPENSL_H
