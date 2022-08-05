#include <cstdio>
#include <cstring>
#include "android/log.h"
#include "jni.h"
#include "Api.h"

//由于 FFmpeg 库是 C 语言实现的，告诉编译器按照 C 的规则进行编译
extern "C" {
#include "../include/libavcodec/version.h"
#include "../include/libavcodec/version.h"
#include "../include/libavcodec/avcodec.h"
#include "../include/libavformat/version.h"
#include "../include/libavutil/version.h"
#include "../include/libavfilter/version.h"
#include "../include/libswresample/version.h"
#include "../include/libswscale/version.h"
#include "../include/libavformat/avformat.h"
#include "../include/libavcodec/codec.h"
};

#ifdef __cplusplus
extern "C" {
#endif

static const char *TAG="FFmpegApi";
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

JNIEXPORT jstring JNICALL  GetFFmpegVersion(JNIEnv *env)
{
    char strBuffer[1024 * 4] = {0};
    strcat(strBuffer, "libavcodec : ");
    strcat(strBuffer, AV_STRINGIFY(LIBAVCODEC_VERSION));
    strcat(strBuffer, "\nlibavformat : ");
    strcat(strBuffer, AV_STRINGIFY(LIBAVFORMAT_VERSION));
    strcat(strBuffer, "\nlibavutil : ");
    strcat(strBuffer, AV_STRINGIFY(LIBAVUTIL_VERSION));
    strcat(strBuffer, "\nlibavfilter : ");
    strcat(strBuffer, AV_STRINGIFY(LIBAVFILTER_VERSION));
    strcat(strBuffer, "\nlibswresample : ");
    strcat(strBuffer, AV_STRINGIFY(LIBSWRESAMPLE_VERSION));
    strcat(strBuffer, "\nlibswscale : ");
    strcat(strBuffer, AV_STRINGIFY(LIBSWSCALE_VERSION));
/*    strcat(strBuffer, "\navcodec_configure : \n");
    strcat(strBuffer, avcodec_configuration());*/
    strcat(strBuffer, "\navcodec_license : ");
    strcat(strBuffer, avcodec_license());
    LOGD("GetFFmpegVersion\n%s", strBuffer);
    return env->NewStringUTF(strBuffer);
}

JNIEXPORT jstring JNICALL GetCodecNames(JNIEnv *env){
    char strBuffer[1024 * 4] = {0};
    void *i = nullptr;
    const AVCodec *first;
    while ((first = av_codec_iterate(&i))){
        if (av_codec_is_encoder(first)){
            strcat(strBuffer,"encoder:");
        }else if (av_codec_is_decoder(first)){
            strcat(strBuffer,"decoder:");
        }
        strcat(strBuffer, first->long_name);
        strcat(strBuffer,"\n");
    }
    return env->NewStringUTF(strBuffer);
}

JNIEXPORT jint JNI_OnLoad(JavaVM* vm,void * r){
    LOGD("JNI_OnLoad");
    JNIEnv* env = NULL;
    jint result = -1;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        LOGD("JNI_OnLoad failure");
        return result;
    };
    JNINativeMethod methods [] = {
            {"nativeGetFFmpegVersion","()Ljava/lang/String;",(void *)GetFFmpegVersion},
            {"nativeGetCodecNames","()Ljava/lang/String;", (void *)GetCodecNames}
    };
    jclass ffPlay = env->FindClass("com/wyc/video/FFmpegPlay/ffmpegApi/FFMediaPlayer");
    if (!ffPlay){
        LOGD("FindClass failure");
        return result;
    }
    if(env->RegisterNatives(ffPlay,methods,sizeof(methods)/ sizeof(methods[0])) < 0){
        LOGD("RegisterNatives failure ");
        return result;
    }
    return JNI_VERSION_1_6;
}

#ifdef __cplusplus
}
#endif