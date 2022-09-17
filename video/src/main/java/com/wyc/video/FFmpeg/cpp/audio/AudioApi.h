//
// Created by Administrator on 2022/9/16.
//

#ifndef WYC_AUDIOAPI_H
#define WYC_AUDIOAPI_H

#include "jni.h"

JNIEXPORT jlong JNICALL initAudio(JNIEnv *env,jobject obj);
JNIEXPORT jint JNICALL openAudio(JNIEnv *env,jobject obj,jlong nativeObj);
JNIEXPORT jint JNICALL startAudio(JNIEnv *env,jobject obj,jlong nativeObj);
JNIEXPORT jint JNICALL pauseAudioPlay(JNIEnv *env,jobject obj,jlong nativeObj);
JNIEXPORT jint JNICALL stopAudio(JNIEnv *env,jobject obj,jlong nativeObj);
JNIEXPORT void JNICALL setRecordingAudio(JNIEnv *env,jobject obj,jlong nativeObj,jint b);
JNIEXPORT void JNICALL setPlayingAudio(JNIEnv *env,jobject obj,jlong nativeObj,jint b);
JNIEXPORT void JNICALL setLoopingAudio(JNIEnv *env,jobject obj,jlong nativeObj,jint b);
JNIEXPORT void JNICALL releaseAudio(JNIEnv *env,jobject obj,jlong nativeObj);
#endif //WYC_AUDIOAPI_H
