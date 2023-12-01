package com.wyc.table_recognition

import android.view.Surface
import java.io.File

internal interface ICamera {
    fun releaseResource()
    fun clearCallback()
    fun setPicCallback(callback: VideoCameraManager.OnPicture?)
    fun recodeVideo()
    fun tackPic()
    fun getPicDir(): File
    fun getBastFPS():Int
    fun getOrientation():Int
    fun getValidCameraId():String
    fun calPreViewAspectRatio():Float
    fun updateFocusRegion(w: Int, h: Int, l: Int, t: Int, r: Int, b: Int)
    fun addSurface(surface: Surface)
    fun openCamera()
    fun setDisplaySize(w:Int,h: Int)
}