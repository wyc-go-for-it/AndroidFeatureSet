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

#include <android/log.h>
#include "SoundRecording.h"
#include "../utils/LogUtil.h"

int32_t SoundRecording::write(const float *sourceData, int32_t numSamples) {

    int i = 0;
    for (; i < numSamples; ++i) {
        float d = sourceData[i];
        mData.push(d);
    }
    LOGE("write data %d,numSamples:%d,writeIndex:%d",i,numSamples,writeIndex());

    return numSamples;
}

int32_t SoundRecording::read(float *targetData, int32_t numSamples){

    int32_t framesRead = 0;
    float data;
    while (framesRead < numSamples){
        if (!mIsLooping && mData.isTail()){
            break;
        }
        if(mData.take(data)) {
            targetData[framesRead++] = data;
        }
    }
    LOGE("FUNCTION:%s,LINE:%d,numSamples:%d,readIndex:%d,writeIndex:%d,framesRead:%d,numSamples:%d",
         __FUNCTION__,__LINE__,numSamples,readIndex(),writeIndex(),framesRead,numSamples);
    return framesRead;
}