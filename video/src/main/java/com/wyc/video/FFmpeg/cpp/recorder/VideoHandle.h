//
// Created by Administrator on 2022/9/1.
//

#ifndef ANDROIDFEATURESET_VIDEOHANDLE_H
#define ANDROIDFEATURESET_VIDEOHANDLE_H

#include "../utils/LogUtil.h"
#include "../utils/MacroUtil.h"
#include "../utils/ImageDef.h"
#include "IMediaHandle.h"

class VideoHandle final :IMediaHandle {
    friend class MediaCoder;
private:
    DISABLE_COPY_ASSIGN(VideoHandle)
    VideoHandle(int width,int height,int frameRatio):mWidth(width),mHeight(height),mFrameRatio(frameRatio){
    }

    ~VideoHandle(){
        LOGD("%s has released",typeid(*this).name());
    }

    bool initVideo(AVFormatContext *s,const char *fileName);
    bool encode(AVFormatContext *s,const NativeImage& data, __int64_t presentationTime) {
        int code = av_frame_make_writable(mFrame);
        if (code < 0){
            LOGE("frame make writable error:%s",av_err2str(code));
            return false;
        }
        mFrame->data[0] = data.getPlanePtr0();
        mFrame->data[1] = data.getPlanePtr1();
        mFrame->data[2] = data.getPlanePtr2();

        mFrame->linesize[0] = data.getPLineSize0();
        mFrame->linesize[1] = data.getPLineSize1();
        mFrame->linesize[2] = data.getPLineSize2();

        mFrame->pts = presentationTime;

        return encodeActually(s);
    }


public:
    const int mWidth,mHeight,mFrameRatio;

};

#endif //ANDROIDFEATURESET_VIDEOHANDLE_H
