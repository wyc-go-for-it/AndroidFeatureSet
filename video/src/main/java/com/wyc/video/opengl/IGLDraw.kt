package com.wyc.video.opengl

import java.nio.FloatBuffer

interface IGLDraw {
    fun init()
    fun draw()
    fun updateData(buffer: FloatBuffer)
}