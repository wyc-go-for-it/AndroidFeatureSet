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

#ifndef WYC_AUDIOENGINE_H
#define WYC_AUDIOENGINE_H

#include <cstdint>
#include <atomic>
#include <memory>
#include <aaudio/AAudio.h>
#include "../utils/MacroUtil.h"
#include "SoundRecording.h"
#include "Definitions.h"
#include <mutex>

class AudioEngine {

public:
    DISABLE_COPY_ASSIGN(AudioEngine)
    AudioEngine()=default;
    ~AudioEngine();
    int start();
    int stop();
    void restart();

    void setRecording(bool isRecording);
    void setPlaying(bool isPlaying);
    void setLooping(bool isOn);

    aaudio_data_callback_result_t recordingCallback(const float *audioData, int32_t numFrames);
    aaudio_data_callback_result_t playbackCallback(float *audioData, int32_t numFrames);

private:
    std::atomic<bool> mIsRecording = {false};
    std::atomic<bool> mIsPlaying = {false};
    AAudioStream* mPlaybackStream = nullptr;
    AAudioStream* mRecordingStream = nullptr;
    SoundRecording mSoundRecording;

    std::mutex m_Lock;

    int stopStream(AAudioStream *stream);
    int closeStream(AAudioStream **stream);

    int stopRecordingStream(){
        int code = 0;
        if(mRecordingStream != nullptr){
            code = stopStream(mRecordingStream);
            code += closeStream(&mRecordingStream);
            mRecordingStream = nullptr;
        }
        return code;
    }
    int stopPlayingStream(){
        int code = 0;
        if (mPlaybackStream != nullptr){
            code = stopStream(mPlaybackStream);
            code += closeStream(&mPlaybackStream);
            mPlaybackStream = nullptr;
        }
        return code;
    }
    static void convertArrayMonoToStereo(float *data, int32_t numFrames) {
        for (int i = numFrames - 1; i >= 0; i--) {
            data[i*2] = data[i];
            data[(i*2)+1] = data[i];
        }
    }

};

#endif
