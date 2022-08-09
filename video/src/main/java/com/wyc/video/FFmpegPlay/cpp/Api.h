//
// Created by Administrator on 2022/8/5.
//

#ifndef ANDROIDFEATURESET_API_H
#define ANDROIDFEATURESET_API_H

#ifdef __cplusplus
extern "C"{
#endif

JNIEXPORT jstring JNICALL GetFFmpegVersion(JNIEnv *env);
JNIEXPORT jstring JNICALL GetCodecNames(JNIEnv *env);
JNIEXPORT jstring JNICALL GetDemuxerNames(JNIEnv *env);
JNIEXPORT jstring JNICALL GetMuxerNames(JNIEnv *env);
#ifdef __cplusplus
}
#endif

#endif //ANDROIDFEATURESET_API_H
