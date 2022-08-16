#include <cstdio>
#include <cstring>
#include <ctime>
#include <unistd.h>
#include "android/log.h"
#include "jni.h"
#include "android/log.h"
#include "libyuv/libyuv.h"
#include <memory>

#ifdef __cplusplus
extern "C" {
#endif

static const char *TAG = "YUVUtils";
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

static uint8_t * NV21TOI420(uint8_t *nv21_data,jint size,jint width,jint height);

void JNI_ThrowByName(JNIEnv *env, const char *name, const char *msg) {
    jclass cls = env->FindClass(name);
    if (cls != nullptr) {
        env->ThrowNew(cls, msg);
    }
    env->DeleteLocalRef(cls);
}

auto deleter = [](uint8_t *p){
    LOGE("delete:%p",p);
    delete[] p;
};

/**
 * src NV21 数据
 * */
JNIEXPORT jintArray JNICALL yuvNV21ToARGB(JNIEnv *env, jclass cls, jbyteArray src_nv21, jint s_w, jint s_h, jintArray pixels) {


    jbyte *jSrc = env->GetByteArrayElements(src_nv21, nullptr);
    jint len = env->GetArrayLength(src_nv21);

    if (pixels == nullptr){
        pixels = env->NewIntArray(s_w * s_h);
    }

    jint *jpixels = env->GetIntArrayElements(pixels, nullptr);


    auto dst = std::unique_ptr<uint8_t,void(*)(uint8_t *)>(NV21TOI420(reinterpret_cast<uint8_t *>(jSrc), len, s_w, s_h),deleter);

    jint src_y_size = s_w * s_h;
    jint src_u_size = (s_w >> 1) * (s_h >> 1);
    auto *dst_i420_y_data = dst.get();
    uint8_t *dst_i420_u_data = dst_i420_y_data + src_y_size;
    uint8_t *dst_i420_v_data = dst_i420_u_data + src_u_size;

    libyuv::I420ToARGB(dst_i420_y_data, s_w, dst_i420_u_data, s_w >> 1, dst_i420_v_data, s_w >> 1,
                       reinterpret_cast<uint8_t *>(jpixels), s_w * 4, s_w, s_h);

    env->ReleaseByteArrayElements(src_nv21,jSrc,0);
    env->ReleaseIntArrayElements(pixels,jpixels,0);

    return pixels;
}

static uint8_t * NV21TOI420(uint8_t *nv21_data,jint size,jint width,jint height){

    auto *dst = new uint8_t[size];

    jint src_y_size = width * height;
    jint src_u_size = (width >> 1) * (height >> 1);

    auto *src_nv21_y_data = nv21_data;
    uint8_t *src_nv21_vu_data = src_nv21_y_data + src_y_size;

    auto *dst_i420_y_data = reinterpret_cast<uint8_t *>(dst);
    uint8_t *dst_i420_u_data = dst_i420_y_data + src_y_size;
    uint8_t *dst_i420_v_data = dst_i420_u_data + src_u_size;

    libyuv::NV21ToI420(src_nv21_y_data,width,src_nv21_vu_data,width,dst_i420_y_data,width,dst_i420_u_data,width >> 1,dst_i420_v_data,width >> 1,width,height);

    return dst;
}


JNIEXPORT jbyteArray JNICALL
rotateYUV_NV21_270(JNIEnv *env, jclass cls, jbyteArray src, jint s_w, jint s_h, jbyteArray dst) {
/*
YYYY
YYYY
VUVU
*/

    jint srcLen = env->GetArrayLength(src);

    if (dst == nullptr)dst = env->NewByteArray(srcLen);

    jbyte  *jSrc = env->GetByteArrayElements(src, nullptr);
    jbyte  *jDst = env->GetByteArrayElements(dst,nullptr);

    auto i420Dst = std::unique_ptr<uint8_t,void(*)(uint8_t *)>(NV21TOI420(reinterpret_cast<uint8_t *>(jSrc), srcLen, s_w, s_h),deleter);

    jint src_y_size = s_w * s_h;
    jint src_u_size = (s_w >> 1) * (s_h >> 1);
    auto *src_i420_y_data = i420Dst.get();
    uint8_t *src_i420_u_data = src_i420_y_data + src_y_size;
    uint8_t *src_i420_v_data = src_i420_u_data + src_u_size;

    auto *dst_i420_y_data = reinterpret_cast<uint8_t *>(jDst);
    uint8_t *dst_i420_u_data = dst_i420_y_data + src_y_size;
    uint8_t *dst_i420_v_data = dst_i420_u_data + src_u_size;

    libyuv::I420Rotate(src_i420_y_data,s_w,src_i420_u_data,s_w >> 1,src_i420_v_data,s_w >> 1,
                       dst_i420_y_data,s_h,dst_i420_u_data,s_h >> 1,dst_i420_v_data,s_h >> 1,
                       s_w,s_h,libyuv::RotationMode::kRotate270);

    auto *dst_nv21_y_data = reinterpret_cast<uint8_t *>(jSrc);
    uint8_t *dst_i420_vu_data = dst_nv21_y_data + src_y_size;
    libyuv::I420ToNV21(dst_i420_y_data,s_w,dst_i420_u_data,s_w >> 1,dst_i420_v_data,s_w >> 1,dst_nv21_y_data,s_w,dst_i420_vu_data,s_w,s_w,s_h);

    env->ReleaseByteArrayElements(src,jSrc,0);
    env->ReleaseByteArrayElements(dst,jDst,0);

    return src;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *unused) {
    LOGD("onLoad");
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGD("GetEnv error");
        return JNI_ERR;
    }

    const JNINativeMethod nativeMethods[] = {
            {"nativeYuv420ToARGB", "([BII[I)[I", reinterpret_cast<void *>(yuvNV21ToARGB)},
            {"nativeRotateYUV_420_270","([BII[B)[B",reinterpret_cast<void *>(rotateYUV_NV21_270)}
    };

    jclass utils = env->FindClass("com/wyc/video/YUVUtils");
    if (utils == nullptr) {
        LOGD("find YUVUtils class failure");
        return JNI_ERR;
    }
    if (env->RegisterNatives(utils, nativeMethods,
                             sizeof(nativeMethods) / sizeof(nativeMethods[0])) < 0) {
        LOGD("RegisterNatives failure ");
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}

#ifdef __cplusplus
}
#endif
