//
// Created by Administrator on 2022/8/26.
//

#ifndef ANDROIDFEATURESET_LOGUTIL_H
#define ANDROIDFEATURESET_LOGUTIL_H

#include<android/log.h>
#include <sys/time.h>

#define  LOG_TAG "WYC"

#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

#define FUN_BEGIN_TIME(FUN) {\
    LOGD("%s:%s func start", __FILE__,#FUN); \
    long long t0 = GetSysCurrentTime();

#define FUN_END_TIME(FUN) \
    long long t1 = GetSysCurrentTime(); \
    LOGD("%s:%s func cost time %ldms", __FILE__, #FUN, (long)(t1-t0));}

#define BEGIN_TIME(FUN) {\
    LOGD("%s func start", FUN); \
    long long t0 = GetSysCurrentTime();

#define END_TIME(FUN) \
    long long t1 = GetSysCurrentTime(); \
    LOGD("%s func cost time %ldms", FUN, (long)(t1-t0));}

static long long GetSysCurrentTime()
{
    struct timeval time;
    gettimeofday(&time, NULL);
    long long curTime = ((long long)(time.tv_sec))*1000+time.tv_usec/1000;
    return curTime;
}

#define GO_CHECK_GL_ERROR(...)   LOGE("CHECK_GL_ERROR %s glGetError = %d, line = %d, ",  __FUNCTION__, glGetError(), __LINE__)

#define LOG_DEBUG(...) LOGD("LOG_DEBUG %s line = %d ",  __FUNCTION__, __LINE__,__VA_ARGS__)

#endif //ANDROIDFEATURESET_LOGUTIL_H
