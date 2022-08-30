//
// Created by Administrator on 2022/8/30.
//

#ifndef ANDROIDFEATURESET_MEDIACODERAPI_H
#define ANDROIDFEATURESET_MEDIACODERAPI_H
#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT jlong JNICALL initCoder(JNIEnv *env,jobject obj,jstring file,int frameRadio,int width,int height);
JNIEXPORT void JNICALL startCoder(JNIEnv *env,jobject obj,jlong nativeObj);
JNIEXPORT void JNICALL stopCoder(JNIEnv *env,jobject obj,jlong nativeObj);
JNIEXPORT void JNICALL releaseCoder(JNIEnv *env,jobject obj,jlong nativeObj);
JNIEXPORT void JNICALL addData(JNIEnv *env,jobject obj,jlong nativeObj,jbyteArray data,int format);
#ifdef __cplusplus
}
#endif
#endif //ANDROIDFEATURESET_MEDIACODERAPI_H
