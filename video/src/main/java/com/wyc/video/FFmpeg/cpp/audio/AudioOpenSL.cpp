//
// Created by Administrator on 2022-09-18.
//

#include "AudioOpenSL.h"

int AudioOpenSL::open() {
    std::lock_guard<std::recursive_mutex> lock(m_audioEngineLock);

    int code = createEngine();
    code += createBufferPlayer();
    code += createRecordingAudio();
    return code;
}

int AudioOpenSL::start() {
    std::lock_guard<std::recursive_mutex> lock(m_audioEngineLock);

    int code = setPlayCallback();
    code += setRecordingCallback();

    return code;
}

int AudioOpenSL::setPlayCallback(){
    if (m_bqPlayerBufferQueue == nullptr){
        LOGE("PlayerBufferQueue object is null");
        return -1;
    }

    // register callback on the buffer queue
    SLresult result = (*m_bqPlayerBufferQueue)->RegisterCallback(m_bqPlayerBufferQueue,AudioOpenSL::bqPlayerCallback,this);
    if (result != SL_RESULT_SUCCESS){
        release();
        LOGE("PlayerBufferQueue RegisterCallback error");
        return -1;
    }

    return 0;
}
int AudioOpenSL::setRecordingCallback() {
    if (m_recorderBufferQueue == nullptr){
        LOGE("RecorderBufferQueue object is null");
        return -1;
    }

    // register callback on the buffer queue
    SLresult result = (*m_recorderBufferQueue)->RegisterCallback(m_recorderBufferQueue,AudioOpenSL::bqRecorderCallback,this);
    if (result != SL_RESULT_SUCCESS){
        release();
        LOGE("RecorderBufferQueue RegisterCallback error");
        return -1;
    }
    return 0;
}


int AudioOpenSL::startRecordingAudio(){
    std::lock_guard<std::recursive_mutex> lock(m_audioEngineLock);

    if (m_recorderRecord == nullptr || m_recorderBufferQueue == nullptr){
        LOGE("RecorderRecord object is null");
        return -1;
    }

    SLresult result;

    // in case already recording, stop recording and clear buffer queue
    result = (*m_recorderRecord)->SetRecordState(m_recorderRecord, SL_RECORDSTATE_STOPPED);
    if (result != SL_RESULT_SUCCESS){
        LOGE("SetRecordState stop state error");
        return -1;
    }

    result = (*m_recorderBufferQueue)->Clear(m_recorderBufferQueue);
    if (result != SL_RESULT_SUCCESS){
        LOGE("Clear recorderBufferQueue error");
        return -1;
    }

    // start recording
    result = (*m_recorderRecord)->SetRecordState(m_recorderRecord, SL_RECORDSTATE_RECORDING);
    if (result != SL_RESULT_SUCCESS){
        LOGE("RecorderRecord record error");
        return -1;
    }

    // enqueue an empty buffer to be filled by the recorder
    // (for streaming recording, we would enqueue at least 2 empty buffers to start things off)
    result = (*m_recorderBufferQueue)->Enqueue(m_recorderBufferQueue, m_recordingBuffer , bufferSize);
    if (result != SL_RESULT_SUCCESS){
        LOGE("RecorderBufferQueue Enqueue error,buffer:%p,size:%d", m_recordingBuffer , bufferSize);
        return -1;
    }

    return 0;
}

int AudioOpenSL::pausePlayback() {
    std::lock_guard<std::recursive_mutex> lock(m_audioEngineLock);

    SLresult result = (*m_bqPlayerPlay)->SetPlayState(m_bqPlayerPlay, SL_PLAYSTATE_PAUSED);
    if (result != SL_RESULT_SUCCESS){
        LOGE("PlayerPlay pause error");
        return -1;
    }
    return 0;
}

int AudioOpenSL::resumePlayback() {
    std::lock_guard<std::recursive_mutex> lock(m_audioEngineLock);

    if (m_bqPlayerPlay == nullptr || m_bqPlayerBufferQueue == nullptr){
        LOGE("PlayerPlay object is null");
        return -1;
    }

    SLresult result = (*m_bqPlayerPlay)->SetPlayState(m_bqPlayerPlay, SL_PLAYSTATE_PLAYING);
    if (result != SL_RESULT_SUCCESS){
        LOGE("PlayerPlay play error");
        return -1;
    }

    result = (*m_bqPlayerBufferQueue)->Enqueue(m_bqPlayerBufferQueue, m_playBuffer, bufferSize);
    if (result != SL_RESULT_SUCCESS){
        LOGE("PlayerBufferQueue Enqueue error,buffer:%p,size:%d", m_playBuffer , bufferSize);
        return -1;
    }
    return 0;
}

int AudioOpenSL::stop() {
    std::lock_guard<std::recursive_mutex> lock(m_audioEngineLock);

    if (m_bqPlayerPlay != nullptr){
        (*m_bqPlayerPlay)->SetPlayState(m_bqPlayerPlay, SL_PLAYSTATE_STOPPED);
    }
    if (m_recorderRecord != nullptr){
        (*m_recorderRecord)->SetRecordState(m_recorderRecord, SL_RECORDSTATE_STOPPED);
    }

    release();
    return 0;
}

void AudioOpenSL::setRecording(bool isRecording) {
    if (isRecording){
        startRecordingAudio();
    }else{
        pauseRecordingAudio();
    }
    mIsRecording = isRecording;
}

void AudioOpenSL::setPlaying(bool isPlaying) {
    if ( isPlaying){
        resumePlayback();
    } else{
        pausePlayback();
    }
    mIsPlaying = isPlaying;
}

void AudioOpenSL::setLooping(bool isOn) {
    mSoundRecording.setLooping(isOn);
    LOGD("Loop:%d",isOn);
}

int AudioOpenSL::createEngine() {

    SLresult result = slCreateEngine(&m_engineObject,0, nullptr,0, nullptr, nullptr);
    if (result != SL_RESULT_SUCCESS){
        LOGE("slCreateEngine error");
        return -1;
    }

    result = (*m_engineObject)->Realize(m_engineObject,SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS){
        LOGE("Realize error");
        return -1;
    }

    result = (*m_engineObject)->GetInterface(m_engineObject,SL_IID_ENGINE,&m_engineEngine);
    if (result != SL_RESULT_SUCCESS){
        (*m_engineObject)->Destroy(m_engineObject);
        LOGE("GetInterface error");
        return -1;
    }

    const SLInterfaceID ids[1] = {SL_IID_ENVIRONMENTALREVERB};
    const SLboolean req[1] = {SL_BOOLEAN_FALSE};
    result = (*m_engineEngine)->CreateOutputMix(m_engineEngine,&m_outputMixObject,1,ids,req);
    if (result != SL_RESULT_SUCCESS){
        (*m_engineObject)->Destroy(m_engineObject);
        LOGE("CreateOutputMix error");
        return -1;
    }

    result = (*m_outputMixObject)->Realize(m_outputMixObject,SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS){
        (*m_engineObject)->Destroy(m_engineObject);
        LOGE("Realize error");
        return -1;
    }
    result = (*m_outputMixObject)->GetInterface(m_outputMixObject, SL_IID_ENVIRONMENTALREVERB,
                                              &m_outputMixEnvironmentalReverb);
    if (SL_RESULT_SUCCESS == result) {
        const SLEnvironmentalReverbSettings reverbSettings = SL_I3DL2_ENVIRONMENT_PRESET_STONECORRIDOR;
        result = (*m_outputMixEnvironmentalReverb)->SetEnvironmentalReverbProperties(
                m_outputMixEnvironmentalReverb, &reverbSettings);
        (void)result;
    }

    return 0;
}

int AudioOpenSL::createBufferPlayer() {
    if (m_engineEngine != nullptr){
        SLresult result;

        // configure audio source
        SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
        SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM, 1, SL_SAMPLINGRATE_44_1,
                                       SL_PCMSAMPLEFORMAT_FIXED_32, SL_PCMSAMPLEFORMAT_FIXED_32,
                                       SL_SPEAKER_FRONT_CENTER, SL_BYTEORDER_LITTLEENDIAN};
        /*
         * Enable Fast Audio when possible:  once we set the same rate to be the native, fast audio path
         * will be triggered
         */
        if(m_bqPlayerSampleRate) {
            format_pcm.samplesPerSec = m_bqPlayerSampleRate;       //sample rate in mili second
        }
        SLDataSource audioSrc = {&loc_bufq, &format_pcm};

        // configure audio sink
        SLDataLocator_OutputMix loc_outMix = {SL_DATALOCATOR_OUTPUTMIX, m_outputMixObject};
        SLDataSink audioSnk = {&loc_outMix, nullptr};

        const SLInterfaceID ids[3] = {SL_IID_BUFFERQUEUE, SL_IID_VOLUME, SL_IID_EFFECTSEND};
        const SLboolean req[3] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};

        result = (*m_engineEngine)->CreateAudioPlayer(m_engineEngine, &m_bqPlayerObject, &audioSrc, &audioSnk,
                                                    m_bqPlayerSampleRate? 2 : 3, ids, req);
        if (result != SL_RESULT_SUCCESS){
            release();
            LOGE("CreateAudioPlayer error");
            return -1;
        }

        // realize the player
        result = (*m_bqPlayerObject)->Realize(m_bqPlayerObject, SL_BOOLEAN_FALSE);
        if (result != SL_RESULT_SUCCESS){
            release();
            LOGE("Realize PlayerObject error");
            return -1;
        }

        // get the play interface
        result = (*m_bqPlayerObject)->GetInterface(m_bqPlayerObject, SL_IID_PLAY, &m_bqPlayerPlay);
        if (result != SL_RESULT_SUCCESS){
            release();
            LOGE("GetInterface PlayerPlay error");
            return -1;
        }

        // get the buffer queue interface
        result = (*m_bqPlayerObject)->GetInterface(m_bqPlayerObject, SL_IID_BUFFERQUEUE,&m_bqPlayerBufferQueue);
        if (result != SL_RESULT_SUCCESS){
            release();
            LOGE("GetInterface PlayerBufferQueue error");
            return -1;
        }

        // get the effect send interface
        /*result = (*m_bqPlayerObject)->GetInterface(m_bqPlayerObject, SL_IID_EFFECTSEND,&m_bqPlayerEffectSend);
        if (result != SL_RESULT_SUCCESS){
            release();
            LOGE("GetInterface PlayerEffectSend error");
            return -1;
        }*/

        // get the volume interface
        result = (*m_bqPlayerObject)->GetInterface(m_bqPlayerObject, SL_IID_VOLUME, &m_bqPlayerVolume);
        if (result != SL_RESULT_SUCCESS){
            release();
            LOGE("GetInterface PlayerVolume error");
            return -1;
        }

        return 0;
    }
    return -1;
}

int AudioOpenSL::createRecordingAudio() {
    if (m_engineEngine == nullptr){
        LOGE("engineEngine object is null");
        return -1;
    }

    SLresult result;

    // configure audio source
    SLDataLocator_IODevice loc_dev = {SL_DATALOCATOR_IODEVICE, SL_IODEVICE_AUDIOINPUT,
                                      SL_DEFAULTDEVICEID_AUDIOINPUT, nullptr};
    SLDataSource audioSrc = {&loc_dev, nullptr};

    // configure audio sink
    SLDataLocator_AndroidSimpleBufferQueue loc_bq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
    SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM, 1, SL_SAMPLINGRATE_44_1,
                                   SL_PCMSAMPLEFORMAT_FIXED_32, SL_PCMSAMPLEFORMAT_FIXED_32,
                                   SL_SPEAKER_FRONT_CENTER, SL_BYTEORDER_LITTLEENDIAN};
    SLDataSink audioSnk = {&loc_bq, &format_pcm};

    // create audio recorder
    // (requires the RECORD_AUDIO permission)
    const SLInterfaceID id[1] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
    const SLboolean req[1] = {SL_BOOLEAN_TRUE};
    result = (*m_engineEngine)->CreateAudioRecorder(m_engineEngine, &m_recorderObject, &audioSrc,&audioSnk, 1, id, req);
    if (result != SL_RESULT_SUCCESS){
        release();
        LOGE("CreateAudioRecorder error");
        return -1;
    }

    // realize the audio recorder
    result = (*m_recorderObject)->Realize(m_recorderObject, SL_BOOLEAN_FALSE);
    if (result != SL_RESULT_SUCCESS){
        release();
        LOGE("Realize AudioRecorder error");
        return -1;
    }

    // get the record interface
    result = (*m_recorderObject)->GetInterface(m_recorderObject, SL_IID_RECORD, &m_recorderRecord);
    if (result != SL_RESULT_SUCCESS){
        release();
        LOGE("GetInterface RecorderObject error");
        return -1;
    }

    // get the buffer queue interface
    result = (*m_recorderObject)->GetInterface(m_recorderObject, SL_IID_ANDROIDSIMPLEBUFFERQUEUE,&m_recorderBufferQueue);
    if (result != SL_RESULT_SUCCESS){
        release();
        LOGE("GetInterface RecorderBufferQueue error");
        return -1;
    }

    return 0;
}
