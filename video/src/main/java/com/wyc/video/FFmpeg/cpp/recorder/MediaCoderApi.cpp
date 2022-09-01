//
// Created by Administrator on 2022/8/30.
//
#include "jni.h"
#include "MediaCoder.h"
#include "MediaCoderApi.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL initCoder(JNIEnv *env,jobject thiz,jstring file,int frameRadio,int width,int height){

/*    jclass cls = env->GetObjectClass(thiz);
    if (cls == nullptr){
        LOGE("can not find Class");
        return 0;
    }
    jfieldID field = env->GetFieldID(cls,"mNativeObj","J");
    if (field == nullptr){
        LOGE("%p does not have mNativeObj field",cls);
        return 0;
    }*/
    const char *f = env->GetStringUTFChars(file, nullptr);

    auto *m_coder = new MediaCoder(string(f),width,height,frameRadio);
    //env->SetLongField(thiz,field,(long )m_coder);

    env->ReleaseStringUTFChars(file,f);

    return (jlong )m_coder;
}

JNIEXPORT void JNICALL startCoder(JNIEnv *env,jobject obj,jlong nativeObj){
    reinterpret_cast<MediaCoder *>(nativeObj)->start();
}

JNIEXPORT void JNICALL stopCoder(JNIEnv *env,jobject thiz,jlong nativeObj){
    auto *c = reinterpret_cast<MediaCoder *>(nativeObj);
    c->stop();
    delete c;
}
JNIEXPORT void JNICALL releaseCoder(JNIEnv *env,jobject obj,jlong nativeObj){
    delete reinterpret_cast<MediaCoder *>(nativeObj);
}
/**
 * @param format 0 I420 1 NV21 2 RGBA
 * */
JNIEXPORT void JNICALL addData(JNIEnv *env,jobject obj,jlong nativeObj,jbyteArray data,int format){
    auto *c = reinterpret_cast<MediaCoder *>(nativeObj);
    jint len = env->GetArrayLength(data);

    IMAGE_FORMAT imageFormat;
    switch (format) {
        case 0:
            imageFormat = IMAGE_FORMAT_I420;
            break;
        case 1:
            imageFormat = IMAGE_FORMAT_NV21;
            break;
        case 2:
            imageFormat = IMAGE_FORMAT_RGBA;
            break;
        default:
            LOGE("not support format %d",format);
            return;
    }
    NativeImage image(imageFormat, c->getVideoWidth(), c->getVideoHeight());
    env->GetByteArrayRegion(data, 0, len, reinterpret_cast<jbyte *>(image.getPlanePtr0()));
    c->addData(image);
}

JNIEXPORT jint JNI_OnLoad(JavaVM* vm,void * r){
    LOGD("JNI_OnLoad");
    JNIEnv* env = nullptr;
    jint result = -1;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
    LOGD("JNI_OnLoad failure");
    return result;
    }
    JNINativeMethod methods [] = {
            {"nativeInitCoder","(Ljava/lang/String;III)J",(void *)initCoder},
            {"nativeStartCoder","(J)V", (void *)startCoder},
            {"nativeStopCoder","(J)V", (void *) stopCoder},
            {"nativeReleaseCoder","(J)V", (void *) releaseCoder},
            {"nativeAddData","(J[BI)V", (void *) addData}
    };
    jclass coder = env->FindClass("com/wyc/video/recorder/FFMediaCoder");
    if (!coder){
        LOGD("FindClass failure");
        return result;
    }
    if(env->RegisterNatives(coder,methods,sizeof(methods)/ sizeof(methods[0])) < 0){
        LOGD("RegisterNatives failure ");
        return result;
    }
    return JNI_VERSION_1_6;
}

#ifdef __cplusplus
}
#endif