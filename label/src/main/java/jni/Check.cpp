#include "Check.h"
#include "android/log.h"
#include <string>
#include <unistd.h>
#include <vector>

#define  LOG_TAG "WYC"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)


std::vector<std::string> split(const std::string &c,const std::string &s){
    std::vector<std::string> token;
    std::string::size_type placeholder = 0;
    std::string::size_type pos = c.find(s,0);
    while (pos != std::string::npos){
        token.push_back(c.substr(placeholder,pos - placeholder));
        placeholder = pos + s.size();
        pos = c.find(s,placeholder);
    }
    token.push_back(c.substr(placeholder,c.size() - placeholder));
    return token;
}

#ifdef __cplusplus
extern "C" {
#endif

static std::string  jstring2string(JNIEnv *env,jstring s){
    if (!s)
        return "";

    const jclass cls = env->GetObjectClass(s);
    const jmethodID sMethodId = env->GetMethodID(cls,"getBytes", "(Ljava/lang/String;)[B");

    const jstring utf_8 = env->NewStringUTF("UTF-8");

    const auto array = static_cast<jbyteArray const>(env->CallObjectMethod(s, sMethodId,utf_8));

    env->DeleteLocalRef(utf_8);

    jsize len = env->GetArrayLength(array);
    jbyte * bytes = env->GetByteArrayElements(array, nullptr);

    std::string ss = std::string((char *)bytes,len);

    env->ReleaseByteArrayElements(array,bytes,JNI_ABORT);

    env->DeleteLocalRef(array);
    env->DeleteLocalRef(cls);

    return ss;
}

JNIEXPORT void JNICALL checkPackageName(JNIEnv *env,jobject thiz,jobject context){
    const jclass cls = env->GetObjectClass(context);
    if (!cls){
        LOGE("GetObjectClass failure");
        return;
    }
    const jmethodID methodId = env->GetMethodID(cls,"getPackageName","()Ljava/lang/String;");
    if (!methodId){
        LOGE("GetMethodID failure");
        return;
    }
    auto name = jstring2string(env,(jstring)env->CallObjectMethod(context, methodId));
    const size_t code = name.find("com.wyc");
    if ((int)code < 0){
        LOGE("checkPackageName %s",name.c_str());
        const int pid = getpid();
        LOGE("code:%d,pid:%d",code,pid);
        kill(pid,SIGKILL);
    }
}

JNIEXPORT jint JNI_OnLoad(JavaVM* vm,void * r){
    LOGD("JNI_OnLoad");
    JNIEnv* env = nullptr;
    jint result = -1;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        LOGE("JNI_OnLoad failure");
        return result;
    }

    const JNINativeMethod methods[] = {
            {"check","(Ljava/lang/Object;)V", (void *)checkPackageName}
    };

    jclass cls = env->FindClass("com/wyc/label/Check");
    if (!cls){
        LOGE("FindClass failure");
        return result;
    }

    if (env->RegisterNatives(cls,methods,sizeof (methods)/sizeof (methods[0])) < 0){
        LOGE("RegisterNatives failure");
        return result;
    }

    return JNI_VERSION_1_6;
}

#if __cplusplus
}
#endif