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

#ifndef WYC_SAMPLE_H
#define WYC_SAMPLE_H

#include <cstdint>
#include <array>
#include <atomic>

#include "Definitions.h"
#include "../utils/MacroUtil.h"
#include "../thread/LoopQueue.h"

constexpr int kMaxSamples = 480000; // 10s of audio data @ 48kHz

class SoundRecording final {
    DISABLE_COPY_ASSIGN(SoundRecording)
public:
    SoundRecording()=default;

    u_int32_t write(const float *sourceData, u_int32_t numSamples);
    u_int32_t read(float *targetData, u_int32_t numSamples);
    void clear() { mData.clear(); };
    void setLooping(bool isLooping) { mIsLooping = isLooping; };
    int32_t getLength() const { return mData.size(); };
    static const int32_t getMaxSamples() { return kMaxSamples; };
    int32_t writeIndex() const{
        return mData.curWriteIndex();
    }
    int32_t readIndex() const{
        return mData.curReadIndex();
    }
private:
    std::atomic<bool> mIsLooping { false };
    LoopQueue<float,kMaxSamples> mData;
};

#endif //WYC_SAMPLE_H
