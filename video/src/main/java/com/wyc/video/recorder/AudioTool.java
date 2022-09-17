package com.wyc.video.recorder;

import android.util.Log;

/**
 * @ProjectName: AndroidFeatureSet
 * @Package: com.wyc.video.recorder
 * @ClassName: AudioTool
 * @Description: 音频工具
 * @Author: wyc
 * @CreateDate: 2022/9/16 17:23
 * @UpdateUser: 更新者：
 * @UpdateDate: 2022/9/16 17:23
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */
public class AudioTool {
    private long mNativeObj;

    public AudioTool(){
        mNativeObj = nativeInitAudio();
        Log.e("AudioTool mNativeObj:",String.format("0x%x",mNativeObj));
    }

    public void open(){
        if (mNativeObj != 0){
            nativeOpenAudio(mNativeObj);
        }
    }

    public void start(){
        if (mNativeObj != 0){
            nativeStartAudio(mNativeObj);
        }
    }

    public void pausePlay(){
        if (mNativeObj != 0){
            nativePausePlayAudio(mNativeObj);
        }
    }

    public void stop(){
        if (mNativeObj != 0){
            nativeStopAudio(mNativeObj);
        }
    }

    public void release(){
        if (mNativeObj != 0){
            nativeStopAudio(mNativeObj);
            nativeReleaseAudio(mNativeObj);
            mNativeObj = 0;
        }
    }

    public void recordingAudio(boolean b){
        if (mNativeObj != 0){
            nativeSetRecordingAudio(mNativeObj,b?0:-1);
        }
    }

    public void playingAudio(boolean b){
        if (mNativeObj != 0){
            nativeSetPlayingAudio(mNativeObj,b?0:-1);
        }
    }

    public void loopingAudio(boolean b){
        if (mNativeObj != 0){
            nativeSetLoopingAudio(mNativeObj,b?0:-1);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        release();
    }

    private native long nativeInitAudio();
    private native void nativeReleaseAudio(long nativeObj);
    private native int nativeOpenAudio(long nativeObj);
    private native int nativeStartAudio(long nativeObj);
    private native int nativePausePlayAudio(long nativeObj);
    private native int nativeStopAudio(long nativeObj);
    private native void nativeSetRecordingAudio(long nativeObj,int b);
    private native void nativeSetPlayingAudio(long nativeObj,int b);
    private native void nativeSetLoopingAudio(long nativeObj,int b);

    static {
        System.loadLibrary("audioCoder");
    }
}
