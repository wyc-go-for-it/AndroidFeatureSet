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

int AudioEngine::start() {
    std::lock_guard<std::mutex> lockGuard(m_Lock);

    // Create the playback stream.
    StreamBuilder playbackBuilder = makeStreamBuilder();
    AAudioStreamBuilder_setFormat(playbackBuilder.get(), AAUDIO_FORMAT_PCM_FLOAT);
    AAudioStreamBuilder_setChannelCount(playbackBuilder.get(), kChannelCountStereo);
    AAudioStreamBuilder_setPerformanceMode(playbackBuilder.get(), AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    AAudioStreamBuilder_setSharingMode(playbackBuilder.get(), AAUDIO_SHARING_MODE_EXCLUSIVE);
    AAudioStreamBuilder_setDataCallback(playbackBuilder.get(), ::playbackDataCallback, this);
    AAudioStreamBuilder_setErrorCallback(playbackBuilder.get(), ::errorCallback, this);

    aaudio_result_t result = AAudioStreamBuilder_openStream(playbackBuilder.get(), &mPlaybackStream);

    if (result != AAUDIO_OK){
        LOGE("Error opening playback stream %s",AAudio_convertResultToText(result));
        return -1;
    }

    int32_t sampleRate = AAudioStream_getSampleRate(mPlaybackStream);

    result = AAudioStream_requestStart(mPlaybackStream);
    if (result != AAUDIO_OK){
        LOGE("Error starting playback stream %s",AAudio_convertResultToText(result));
        closeStream(&mPlaybackStream);
        return -1;
    }

    LOGD("sampleRate:%d",sampleRate);

    // Create the recording stream.
    StreamBuilder recordingBuilder = makeStreamBuilder();
    AAudioStreamBuilder_setDirection(recordingBuilder.get(), AAUDIO_DIRECTION_INPUT);
    AAudioStreamBuilder_setPerformanceMode(recordingBuilder.get(), AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    AAudioStreamBuilder_setSharingMode(recordingBuilder.get(), AAUDIO_SHARING_MODE_EXCLUSIVE);
    AAudioStreamBuilder_setFormat(recordingBuilder.get(), AAUDIO_FORMAT_PCM_FLOAT);
    AAudioStreamBuilder_setSampleRate(recordingBuilder.get(), sampleRate);
    AAudioStreamBuilder_setChannelCount(recordingBuilder.get(), kChannelCountMono);
    AAudioStreamBuilder_setDataCallback(recordingBuilder.get(), ::recordingDataCallback, this);
    AAudioStreamBuilder_setErrorCallback(recordingBuilder.get(), ::errorCallback, this);

    result = AAudioStreamBuilder_openStream(recordingBuilder.get(), &mRecordingStream);

    if (result != AAUDIO_OK){
        LOGE("Error opening recording stream %s",AAudio_convertResultToText(result));
        closeStream(&mRecordingStream);
        return -1;
    }

    result = AAudioStream_requestStart(mRecordingStream);
    if (result != AAUDIO_OK){
        LOGE("Error starting recording stream %s",AAudio_convertResultToText(result));
        return -1;
    }

    return 0;
}

int AudioEngine::stop() {
    int code = stopPlayingStream();
    code += stopRecordingStream();
    return code;
}

void AudioEngine::restart(){
    int code = stop();
    if (code == 0)
        code += start();
    else
        LOGE("restart error");
}

aaudio_data_callback_result_t AudioEngine::recordingCallback(const float *audioData,int32_t numFrames) {
    if (mIsRecording) {
        int32_t framesWritten = mSoundRecording.write(audioData, numFrames);
        if (framesWritten == 0) mIsRecording = false;
        if (numFrames == 0) mIsRecording = false;
    }
    return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

aaudio_data_callback_result_t AudioEngine::playbackCallback(float *audioData, int32_t numFrames) {

    memset(audioData,0,numFrames * kChannelCountStereo);

    if (mIsPlaying) {
        int32_t framesRead = mSoundRecording.read(audioData, numFrames);
        convertArrayMonoToStereo(audioData, framesRead);
        //if (framesRead < numFrames) mIsPlaying = false;
    }

    return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

void AudioEngine::setRecording(bool isRecording) {
    mIsRecording = isRecording;
}

void AudioEngine::setPlaying(bool isPlaying) {
    mIsPlaying = isPlaying;
}

int AudioEngine::stopStream(AAudioStream *stream) {
    std::lock_guard<std::mutex> lockGuard(m_Lock);
    if (stream != nullptr) {
        aaudio_result_t result = AAudioStream_requestStop(stream);
        if (result != AAUDIO_OK) {
            LOGE("Error stop stream %s",AAudio_convertResultToText(result));
            return -1;
        }
    }
    return 0;
}

int AudioEngine::closeStream(AAudioStream **stream) {
    std::lock_guard<std::mutex> lockGuard(m_Lock);

    if (*stream != nullptr) {
        aaudio_result_t result = AAudioStream_close(*stream);
        if (result != AAUDIO_OK) {
            LOGE("Error closing stream %s",AAudio_convertResultToText(result));
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
    LOGD("AudioEngine has destroyed");
    stop();
}
