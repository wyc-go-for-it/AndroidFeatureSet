//
// Created by Administrator on 2022-09-18.
//

#ifndef WYC_AUDIOENGINEBASE_H
#define WYC_AUDIOENGINEBASE_H
#include "../utils/MacroUtil.h"
#include "../utils/LogUtil.h"
#include <functional>
#include <atomic>

class IAudioEngine{
public:
    DISABLE_COPY_ASSIGN(IAudioEngine)
    IAudioEngine()=default;
    virtual ~IAudioEngine(){
        LOGD("%s",__FUNCTION__ );
    }
    virtual int open() = 0;
    virtual int start() = 0;
    virtual int pausePlayback() = 0;
    virtual int resumePlayback() = 0;
    virtual int stop() = 0;

    virtual void setRecording(bool isRecording) = 0;
    virtual void setPlaying(bool isPlaying) = 0;
    virtual void setLooping(bool isOn) = 0;

public:
    void setDataCallback(std::function<bool(const float *,int32_t numFrames)> fun){
        mDataCallback = fun;
    }

private:
    std::function<bool(const float *,int32_t numFrames)> mDataCallback = nullptr;

protected:
    std::atomic<bool> mIsRecording = {false};
    std::atomic<bool> mIsPlaying = {false};
    bool invokeCallback(const float * data,int32_t numFrames){
        if (mDataCallback != nullptr)return mDataCallback(data,numFrames);
        return false;
    }
};
#endif //WYC_AUDIOENGINEBASE_H
