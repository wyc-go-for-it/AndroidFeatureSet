package com.wyc.video.camera

import android.view.Surface
import java.io.File

interface ICamera {
    fun releaseResource()
    fun clearCallback()
    fun setPicCallback(callback: VideoCameraManager.OnPicture?)
    fun switchCamera()
    fun recodeVideo()
    fun stopRecord(preview:Boolean)
    fun tackPic()
    fun sycCaptureMode(mode: VideoCameraManager.MODE)
    fun getPicDir(): File
    fun getVWidth():Int
    fun getVHeight():Int
    fun getBastFPS():Int
    fun isBack():Boolean
    fun getOrientation():Int
    fun getValidCameraId():String
    fun sycRecordingState(state: VideoCameraManager.RECORD_STATUS)
    fun calPreViewAspectRatio():Float
    fun updateFocusRegion(w: Int, h: Int, l: Int, t: Int, r: Int, b: Int)
    fun addSurface(surface: Surface)
    fun openCamera()
}