package com.wyc.video.opengl

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGLRenderer:GLSurfaceView.Renderer {
    private val mInstance:IGLDraw = Audio()
    fun updateData(data:FloatArray){
        mInstance.updateData(data)
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        mInstance.init()
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES30.glViewport(0,0,width,height)
        mInstance.sizeChanged(width,height)

    }

    override fun onDrawFrame(gl: GL10) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        mInstance.draw()
    }
}