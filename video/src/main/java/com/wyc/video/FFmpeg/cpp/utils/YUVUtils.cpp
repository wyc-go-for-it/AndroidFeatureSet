#include <cstdio>
#include <cstring>
#include <ctime>
#include <unistd.h>
#include "android/log.h"
#include "jni.h"
#include "android/log.h"

#ifdef __cplusplus
extern "C" {
#endif

static const char *TAG = "YUVUtils";
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

void JNI_ThrowByName(JNIEnv *env, const char *name, const char *msg) {
    jclass cls = env->FindClass(name);
    if (cls != nullptr) {
        env->ThrowNew(cls, msg);
    }
    env->DeleteLocalRef(cls);
}

JNIEXPORT jintArray JNICALL
yuv420ToARGB(JNIEnv *env, jclass cls, jbyteArray src, jint s_w, jint s_h, jintArray pixels) {
/*
NV21

YYYY
YYYY
VUVU

*/

    jint srcLen = env->GetArrayLength(src);
    if (s_w * s_h * 1.5 > srcLen) {
        JNI_ThrowByName(env, "java/lang/IllegalArgumentException", "src is too small.");
        return nullptr;
    }

    if (pixels != nullptr) {
        int pixelsLen = env->GetArrayLength(pixels);
        if (pixelsLen < s_w * s_h) {
            JNI_ThrowByName(env, "java/lang/IllegalArgumentException", "src is too small.");
            return nullptr;
        }
    } else {
        pixels = env->NewIntArray(s_w * s_h);
    }

    jint r, g, b;
    jint y = 0, v = 0, u = 0;
    jint xOffset, yOffset;

    jint *jpixels = env->GetIntArrayElements(pixels, nullptr);
    jbyte *jSrc = env->GetByteArrayElements(src, nullptr);

    for (int j = 0; j < s_h; j++) {
        xOffset = j * s_w;
        yOffset = s_h * s_w + (j >> 1) * s_w;
        for (int i = 0; i < s_w; i++) {
            y = (jSrc[xOffset + i] & 0xff);

            if ((i & 1) == 0) {
                v = (0xff & jSrc[yOffset++]) - 128;
                u = (0xff & jSrc[yOffset++]) - 128;
            }

            r = y + v + ((v * 103) >> 8);
            g = y - ((u * 88) >> 8) - ((v * 183) >> 8);
            b = y + u + ((u * 198) >> 8);

            if (r < 0)
                r = 0;
            else if (r > 255)
                r = 255;
            if (g < 0)
                g = 0;
            else if (g > 255)
                g = 255;
            if (b < 0)
                b = 0;
            else if (b > 255)
                b = 255;

            jpixels[xOffset + i] = 0xff000000 | (r << 16) | (g << 8) | b;

/*                int y1192 = 1192 * y;
                r = (y1192 + 1634 * v);
                g = (y1192 - 833 * v - 400 * u);
                b = (y1192 + 2066 * u);
                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;


                pixels[xOffset + i] = 0xff000000 | ((r << 6) & 0xff0000)
                        | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);*/
        }
    }
    env->ReleaseByteArrayElements(src, jSrc, 0);
    env->ReleaseIntArrayElements(pixels, jpixels, 0);

    return pixels;
}


JNIEXPORT jbyteArray JNICALL
rotateYUV_420_270(JNIEnv *env, jclass cls, jbyteArray src, jint s_w, jint s_h, jbyteArray dst) {
/*
YYYY
YYYY
VUVU
*/

    jint srcLen = env->GetArrayLength(src);

    if (dst == nullptr)dst = env->NewByteArray(srcLen);


    jbyte  *jSrc = env->GetByteArrayElements(src, nullptr);
    jbyte  *jDst = env->GetByteArrayElements(dst,nullptr);

    int srcOffset;
    for (int i = 0; i < s_h; i++) {
        srcOffset = i * s_w;
        for (int j = 0; j < s_w; j++) {
            jDst[(s_w - 1 - j) * s_h + i] = jSrc[srcOffset + j];
        }
    }

    int tOffset = s_w * s_h, offset;
    for (int i = s_h, k = 0; i < s_h + s_h / 2; i++, k++) {
        srcOffset = i * s_w;
        for (int j = s_w; j > 0; j -= 2) {
            offset = tOffset + ((s_w - j) >> 1) * s_h - (s_h - i) * 2;
            jDst[offset] = jSrc[srcOffset + j - 2];
            jDst[offset + 1] = jSrc[srcOffset + j - 1];
        }
    }

    env->ReleaseByteArrayElements(src,jSrc,0);
    env->ReleaseByteArrayElements(dst,jDst,0);

    return dst;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *unused) {
    LOGD("onLoad");
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGD("GetEnv error");
        return JNI_ERR;
    }

    const JNINativeMethod nativeMethods[] = {
            {"nativeYuv420ToARGB", "([BII[I)[I", reinterpret_cast<void *>(yuv420ToARGB)},
            {"nativeRotateYUV_420_270","([BII[B)[B",reinterpret_cast<void *>(rotateYUV_420_270)}
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
