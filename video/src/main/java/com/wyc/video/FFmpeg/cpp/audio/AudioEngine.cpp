/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "AudioEngine.h"
#include <aaudio/AAudio.h>
#include "../utils/LogUtil.h"
#include <thread>


aaudio_data_callback_result_t recordingDataCallback(
        AAudioStream __unused *stream,
        void *userData,
        void *audioData,
        int32_t numFrames) {
    return ((AudioEngine *) userData)->recordingCallback(static_cast<float *>(audioData), numFrames);
}

aaudio_data_callback_result_t playbackDataCallback(
        AAudioStream __unused *stream,
        void *userData,
        void *audioData,
        int32_t numFrames) {

    return ((AudioEngine *) userData)->playbackCallback(static_cast<float *>(audioData), numFrames);
}

void errorCallback(AAudioStream __unused *stream,
                   void *userData,
                   aaudio_result_t error){
    if (error == AAUDIO_ERROR_DISCONNECTED){
        std::function<void(void)> restartFunction = std::bind(&AudioEngine::restart,
                                                              static_cast<AudioEngine *>(userData));
        new std::thread(restartFunction);
    }
}

using StreamBuilder = std::unique_ptr<AAudioStreamBuilder, decltype(&AAudioStreamBuilder_delete)>;

StreamBuilder makeStreamBuilder(){

    AAudioStreamBuilder *builder = nullptr;
    aaudio_result_t result = AAudio_createStreamBuilder(&builder);
    if (result != AAUDIO_OK) {
        LOGE("Failed to create stream builder %s (%d)",AAudio_convertResultToText(result), result);
        return StreamBuilder(nullptr, &AAudioStreamBuilder_delete);
    }
    return StreamBuilder(builder, &AAudioStreamBuilder_delete);
}

int AudioEngine::open(){
    int32_t sampleRate = openPlayingStream();

    LOGD("sampleRate:%d",sampleRate);

    openRecordingStream();

    return 0;
}

int AudioEngine::openPlayingStream(){
    std::lock_guard<std::recursive_mutex> lockGuard(m_Lock);

    // Create the playback stream.
    const StreamBuilder playbackBuilder = makeStreamBuilder();
    AAudioStreamBuilder_setFormat(playbackBuilder.get(), AAUDIO_FORMAT_PCM_FLOAT);
    AAudioStreamBuilder_setChannelCount(playbackBuilder.get(), kChannelCountStereo);
    AAudioStreamBuilder_setPerformanceMode(playbackBuilder.get(), AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    AAudioStreamBuilder_setSharingMode(playbackBuilder.get(), AAUDIO_SHARING_MODE_EXCLUSIVE);
    AAudioStreamBuilder_setDataCallback(playbackBuilder.get(), ::playbackDataCallback, this);
    AAudioStreamBuilder_setErrorCallback(playbackBuilder.get(), ::errorCallback, this);

   const aaudio_result_t result = AAudioStreamBuilder_openStream(playbackBuilder.get(), &mPlaybackStream);

    if (result != AAUDIO_OK){
        LOGE("Error opening playback stream %s",AAudio_convertResultToText(result));
        closeStream(&mPlaybackStream,"PlaybackStream");
        return -1;
    }

    return (mSampleRate = AAudioStream_getSampleRate(mPlaybackStream));
}

int AudioEngine::openRecordingStream(){
    std::lock_guard<std::recursive_mutex> lockGuard(m_Lock);

    // Create the recording stream.
    StreamBuilder recordingBuilder = makeStreamBuilder();
    AAudioStreamBuilder_setDirection(recordingBuilder.get(), AAUDIO_DIRECTION_INPUT);
    AAudioStreamBuilder_setPerformanceMode(recordingBuilder.get(), AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    AAudioStreamBuilder_setSharingMode(recordingBuilder.get(), AAUDIO_SHARING_MODE_EXCLUSIVE);
    AAudioStreamBuilder_setFormat(recordingBuilder.get(), AAUDIO_FORMAT_PCM_FLOAT);
    AAudioStreamBuilder_setSampleRate(recordingBuilder.get(), mSampleRate);
    AAudioStreamBuilder_setChannelCount(recordingBuilder.get(), kChannelCountMono);
    AAudioStreamBuilder_setDataCallback(recordingBuilder.get(), ::recordingDataCallback, this);
    AAudioStreamBuilder_setErrorCallback(recordingBuilder.get(), ::errorCallback, this);

    const aaudio_result_t result = AAudioStreamBuilder_openStream(recordingBuilder.get(), &mRecordingStream);

    if (result != AAUDIO_OK){
        LOGE("Error opening recording stream %s",AAudio_convertResultToText(result));
        closeStream(&mRecordingStream,"RecordingStream");
        return -1;
    }
    return 0;
}

int AudioEngine::start() {
    int code = startPlayingStream();
    code += startRecordingStream();
    return code;
}
int AudioEngine::startRecordingStream(){
    if (nullptr != mRecordingStream ){
        return startStream(mRecordingStream,"RecordingStream");
    }
    return  -1;
}

int AudioEngine::startPlayingStream() {
    if (mPlaybackStream != nullptr){
        return startStream(mPlaybackStream,"PlaybackStream");
    }
    return -1;
}

int AudioEngine::pausePlayback(){
    return pausePlayingStream();
}
int AudioEngine::resumePlayback(){
    return startPlayingStream();
}

int AudioEngine::pausePlayingStream(){
    std::lock_guard<std::recursive_mutex> lockGuard(m_Lock);
    if (mPlaybackStream != nullptr) {
        aaudio_result_t result = AAudioStream_requestPause(mPlaybackStream);
        if (result != AAUDIO_OK) {
            LOGE("Error pause playback stream %s", AAudio_convertResultToText(result));
            closeStream(&mPlaybackStream, "PlaybackStream");
            return -1;
        }
        return 0;
    }
    return -1;
}

int AudioEngine::stop() {
    int code = stopPlayingStream();
    code += stopRecordingStream();
    return code;
}

int AudioEngine::stopRecordingStream(){
    int code = 0;
    if(mRecordingStream != nullptr){
        code = stopStream(mRecordingStream,"RecordingStream");
        code += closeStream(&mRecordingStream,"RecordingStream");
        mRecordingStream = nullptr;
    }
    return code;
}
int AudioEngine::stopPlayingStream(){
    int code = 0;
    if (mPlaybackStream != nullptr){
        code = stopStream(mPlaybackStream,"PlaybackStream");
        code += closeStream(&mPlaybackStream,"PlaybackStream");
        mPlaybackStream = nullptr;
    }
    return code;
}

void AudioEngine::restart(){
    LOGE("AudioEngine restart...");
    int code = stop();
    if (code == 0)
        start();
    else
        LOGE("restart error code:%d",code);
}

aaudio_data_callback_result_t AudioEngine::recordingCallback(const float *audioData,int32_t numFrames) {
    if (mIsRecording) {
        if (!invokeCallback(audioData,numFrames)){
            int32_t framesWritten = mSoundRecording.write(audioData, numFrames);
            if (framesWritten == 0 || numFrames == 0) mIsRecording = false;
        }
    }
    return mIsRecording ? AAUDIO_CALLBACK_RESULT_CONTINUE : AAUDIO_CALLBACK_RESULT_STOP;
}

aaudio_data_callback_result_t AudioEngine::playbackCallback(float *audioData, int32_t numFrames) {
    memset(audioData,0,numFrames * kChannelCountStereo);
    if (mIsPlaying) {
        int32_t framesRead = mSoundRecording.read(audioData, numFrames);
        convertArrayMonoToStereo(audioData, framesRead);
        if (framesRead < numFrames){
            mIsPlaying = false;
        }
    }
    return mIsPlaying ? AAUDIO_CALLBACK_RESULT_CONTINUE : AAUDIO_CALLBACK_RESULT_STOP;
}


void AudioEngine::setRecording(bool isRecording) {
    if (!mIsRecording && isRecording){
        mIsRecording = true;
        startRecordingStream();
    } else
        mIsRecording = isRecording;
}

void AudioEngine::setPlaying(bool isPlaying) {
    if (!mIsPlaying && isPlaying){
        mIsPlaying = true;
        resumePlayback();
    } else
        mIsPlaying = isPlaying;
}

int AudioEngine::startStream(AAudioStream *stream,const char * s) {
    std::lock_guard<std::recursive_mutex> lockGuard(m_Lock);
    if (stream != nullptr) {
        aaudio_result_t result = AAudioStream_requestStart(stream);
        if (result != AAUDIO_OK) {
            LOGE("Error start %s stream %s",s,AAudio_convertResultToText(result));
            return -1;
        }
    }
    return 0;
}

int AudioEngine::stopStream(AAudioStream *stream,const char * s) {
    std::lock_guard<std::recursive_mutex> lockGuard(m_Lock);
    if (stream != nullptr) {
        aaudio_stream_state_t state = AAudioStream_getState(stream);
        LOGD("stream_state :%s",AAudio_convertStreamStateToText(state));

        if (state != AAUDIO_STREAM_STATE_PAUSED && state != AAUDIO_STREAM_STATE_PAUSING){
            aaudio_result_t result = AAudioStream_requestStop(stream);
            if (result != AAUDIO_OK) {
                LOGE("Error stop %s stream %s",s,AAudio_convertResultToText(result));
                return -1;
            }
        }
    }
    return 0;
}

int AudioEngine::closeStream(AAudioStream **stream,const char * s) {
    std::lock_guard<std::recursive_mutex> lockGuard(m_Lock);
    if (*stream != nullptr) {
        aaudio_result_t result = AAudioStream_close(*stream);
        if (result != AAUDIO_OK) {
            LOGE("Error closing %s stream %s",s,AAudio_convertResultToText(result));
            return -1;
        }
        *stream = nullptr;
    }

    return 0;
}

void AudioEngine::setLooping(bool isOn) {
    LOGD("Looping %d",isOn);
    mSoundRecording.setLooping(isOn);
}

AudioEngine::~AudioEngine() {
    LOGD("%s has executed",__FUNCTION__);
    stop();
}
