package com.wyc.video.opengl

interface IGLDraw {
    fun init()
    fun draw()
    fun updateData(buffer: FloatArray)
}