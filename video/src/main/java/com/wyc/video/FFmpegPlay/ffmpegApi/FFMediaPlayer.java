package com.wyc.video.FFmpegPlay.ffmpegApi;

/**
 * @ProjectName: AndroidFeatureSet
 * @Package: com.wyc.video.FFmpegPlay.ffmpegApi
 * @ClassName: FFMediaPlayer
 * @Description: FFmpeg Java api
 * @Author: wyc
 * @CreateDate: 2022/8/5 11:54
 * @UpdateUser: 更新者：
 * @UpdateDate: 2022/8/5 11:54
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */
public class FFMediaPlayer {
    static {
        System.loadLibrary("FFmpegApi");
    }
    public static String getFFmpegVersion() {
        return nativeGetFFmpegVersion();
    }
    public static String getFFmpegAllCodecName(){
        return nativeGetCodecNames();
    }
    public static String getFFmpegDemuxerName(){
        return nativeGetDemuxerNames();
    }
    public static String getFFmpegMuxerName(){
        return nativeGetMuxerNames();
    }

    private static native String nativeGetFFmpegVersion();
    private static native String nativeGetCodecNames();
    private static native String nativeGetDemuxerNames();
    private static native String nativeGetMuxerNames();
}
