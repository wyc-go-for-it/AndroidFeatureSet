//
// Created by Administrator on 2022/9/16.
//

#include "AudioApi.h"
#include "../utils/LogUtil.h"
#include "AudioEngine.h"

JNIEXPORT jlong JNICALL initAudio(JNIEnv *env,jobject obj){
    return (jlong)new AudioEngine();
}
JNIEXPORT jint JNICALL startAudio(JNIEnv *env,jobject obj,jlong nativeObj) {
   return reinterpret_cast<AudioEngine *>(nativeObj)->start();
}
JNIEXPORT jint JNICALL stopAudio(JNIEnv *env,jobject obj,jlong nativeObj){
    return reinterpret_cast<AudioEngine *>(nativeObj)->stop();
}

JNIEXPORT void JNICALL setRecordingAudio(JNIEnv *env,jobject obj,jlong nativeObj,jint b){
    reinterpret_cast<AudioEngine *>(nativeObj)->setRecording(b == 0);
}
JNIEXPORT void JNICALL setPlayingAudio(JNIEnv *env,jobject obj,jlong nativeObj,jint b){
    reinterpret_cast<AudioEngine *>(nativeObj)->setPlaying(b == 0);
}
JNIEXPORT void JNICALL setLoopingAudio(JNIEnv *env,jobject obj,jlong nativeObj,jint b){
    reinterpret_cast<AudioEngine *>(nativeObj)->setLooping(b == 0);
}
JNIEXPORT void JNICALL releaseAudio(JNIEnv *env,jobject obj,jlong nativeObj){
    delete reinterpret_cast<AudioEngine *>(nativeObj);
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
            {"nativeInitAudio","()J",(void *)initAudio},
            {"nativeReleaseAudio","(J)V",(void *)releaseAudio},
            {"nativeStartAudio", "(J)I",(void *)startAudio},
            {"nativeStopAudio","(J)I", (void *)stopAudio},
            {"nativeSetRecordingAudio","(JI)V", (void *)setRecordingAudio},
            {"nativeSetPlayingAudio","(JI)V", (void *)setPlayingAudio},
            {"nativeSetLoopingAudio","(JI)V", (void *)setLoopingAudio}
    };
    jclass coder = env->FindClass("com/wyc/video/recorder/AudioTool");
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