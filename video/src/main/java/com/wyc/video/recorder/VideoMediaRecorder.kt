package com.wyc.video.recorder

import android.content.Context
import android.graphics.ImageFormat
import android.media.CamcorderProfile
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Build
import android.os.storage.StorageManager
import android.view.Surface
import android.view.TextureView
import com.wyc.video.Utils
import com.wyc.video.VideoApp
import com.wyc.video.camera.VideoCameraManager
import java.io.FileInputStream


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.video.recorder
 * @ClassName:      VideoMediaRecorder
 * @Description:    基于MediaRecorder的视频录制
 * @Author:         wyc
 * @CreateDate:     2022/6/9 13:37
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/6/9 13:37
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class VideoMediaRecorder :AbstractRecorder() {
    private var mMediaRecorder: MediaRecorder? = MediaRecorder()
    override fun configure() {
        try {
            if (mMediaRecorder == null){
                mMediaRecorder =
                        /*if (Build.VERSION.SDK_INT >  Build.VERSION_CODES.R) {
                    MediaRecorder(VideoApp.getInstance())
                } else */MediaRecorder()
            }else{
                mMediaRecorder!!.reset()
            }
            mMediaRecorder!!.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)

                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    setOutputFile(getFile())
                }else{
                    setOutputFile(getFile().absolutePath)
                }

                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)

                /*if (Build.VERSION.SDK_INT >  Build.VERSION_CODES.R){
                    val encoderProfiles = CamcorderProfile.getAll(getValidCameraId(),CamcorderProfile.QUALITY_720P)
                    encoderProfiles?.apply {
                        val videos = videoProfiles
                        if (videos.isNotEmpty()){
                            val  videoProfile = videos[0]
                            setVideoSize(videoProfile.width,videoProfile.height)
                            setVideoFrameRate(videoProfile.frameRate)
                            setVideoEncodingBitRate(videoProfile.bitrate)
                        }

                        val audios =  audioProfiles
                        if (audios.isNotEmpty()){
                            val audio = audios[0]
                            setAudioChannels(audio.channels)
                            setAudioSamplingRate(audio.sampleRate)
                            setAudioEncodingBitRate(audio.bitrate)
                        }
                    }
                }else*/

                setVideoSize(VideoCameraManager.getInstance().vWidth,VideoCameraManager.getInstance().vHeight)

                val  camcorderProfile =  CamcorderProfile.get(VideoCameraManager.getInstance().getValidCameraId().toInt(), CamcorderProfile.QUALITY_720P)
                setVideoFrameRate(camcorderProfile.videoFrameRate)
                setVideoEncodingBitRate(camcorderProfile.videoBitRate)

                setAudioChannels(camcorderProfile.audioChannels)
                setAudioSamplingRate(camcorderProfile.audioSampleRate)
                setAudioEncodingBitRate(camcorderProfile.audioBitRate)

                setOrientationHint(VideoCameraManager.getInstance().getOrientation())

                setOnErrorListener { _, what, _ -> Utils.logInfo("mediaRecorder error:$what") }

                prepare()
            }
        }catch (e:IllegalStateException ){
            e.printStackTrace()
        }
    }

    override fun getSurface():Surface {
        if (mMediaRecorder == null){
            throw IllegalStateException(javaClass.name + " has not configured")
        }
        return mMediaRecorder!!.surface
    }

    override fun start() {
        mMediaRecorder?.apply {
            try {
                start()
            }catch (e: IllegalStateException){
                e.printStackTrace()
            }
        }
    }

    override fun stop() {
        mMediaRecorder?.apply {
            try {
                stop()
            }catch (e: IllegalStateException){
                e.printStackTrace()
            }
        }
    }
    override fun release() {
        if (mMediaRecorder != null){
            mMediaRecorder!!.release()
            mMediaRecorder = null
        }
        super.release()
    }
}