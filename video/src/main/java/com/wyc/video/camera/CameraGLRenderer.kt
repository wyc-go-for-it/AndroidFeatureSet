package com.wyc.video.camera

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.view.Surface
import com.wyc.logger.Logger
import com.wyc.video.R
import com.wyc.video.opengl.Shader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.video.camera
 * @ClassName:      CameraGLRenderer
 * @Description:    gl相机渲染
 * @Author:         wyc
 * @CreateDate:     2022/12/16 16:24
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/12/16 16:24
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class CameraGLRenderer : GLSurfaceView.Renderer,SurfaceTexture.OnFrameAvailableListener {
    private var mSurfaceTexture:SurfaceTexture? = null
    private var mSurface:Surface? = null
    private val mTextureId = IntBuffer.allocate(1)
    private var mShader:Shader? = null


    // coord-s
    private val vertices = floatArrayOf(-1f, -1f,0f, -1f, 1f,0f, 1f, -1f,0f, 1f, 1f,0f)
    private val texCoord = floatArrayOf(0f, 0f,0f,0f, 0f, 1f,0f,0f, 1f, 1f,0f,0f, 1f, 0f,0f,0f)

    private val vertBufferSize = vertices.size * Float.SIZE_BYTES
    private val verCoordBuffer:FloatBuffer = ByteBuffer.allocateDirect(vertBufferSize).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val texCoordBufferSize = texCoord.size * Float.SIZE_BYTES
    private val texCoordBuffer = ByteBuffer.allocateDirect(texCoordBufferSize).order(ByteOrder.nativeOrder()).asFloatBuffer()

    private var mFrameAvailable = false;

    private val vertIds = IntBuffer.allocate(2)


    private var mTextureMatrixHandle = -1;
    private val mTextureMatrix = FloatArray(16)

    init {
        verCoordBuffer.put(vertices).position(0)
        texCoordBuffer.put(texCoord).position(0)
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig?) {
        init()
    }

    private fun init(){
        mShader = Shader(R.raw.camera_vertex,R.raw.camera_frag)
        mShader!!.use()

        //分配纹理对象
        GLES30.glGenTextures(1,mTextureId)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,mTextureId[0])
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES30.GL_NONE)

        //
        mTextureMatrixHandle = GLES30.glGetUniformLocation(mShader!!.program(),"uTextureMatrix")
        Logger.d("uTextureMatrix:%d",mTextureMatrixHandle)

        mSurfaceTexture = SurfaceTexture(mTextureId[0])
        mSurfaceTexture!!.setOnFrameAvailableListener(this)
        mSurface = Surface(mSurfaceTexture)

        //初始化相机
        VideoCameraManager.getInstance().addSurface(mSurface!!)
        VideoCameraManager.getInstance().openCamera()

        //初始化顶点

        GLES30.glGenVertexArrays(2,vertIds)
        //顶点坐标
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vertIds[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,vertBufferSize,verCoordBuffer,GLES30.GL_STATIC_DRAW)
        GLES30.glVertexAttribPointer(0,3,GLES30.GL_FLOAT,false,3 * Float.SIZE_BYTES,0)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,GLES30.GL_NONE)

        //纹理坐标
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vertIds[1])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,texCoordBufferSize,texCoordBuffer,GLES30.GL_STATIC_DRAW)
        GLES30.glVertexAttribPointer(1,4,GLES30.GL_FLOAT,false,4 * Float.SIZE_BYTES,0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,GLES30.GL_NONE)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES30.glViewport(0,0,width,height)
    }

    override fun onDrawFrame(gl: GL10) {
        if (mFrameAvailable){
            mSurfaceTexture!!.updateTexImage()
            mSurfaceTexture!!.getTransformMatrix(mTextureMatrix)
            mFrameAvailable = false
        }

        GLES30.glClear(0)

        //传递矩阵
        GLES30.glUniformMatrix4fv(mTextureMatrixHandle, 1, false, mTextureMatrix, 0)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP,0,4)
    }

    fun clear(){
        mSurfaceTexture!!.release()
        mSurface?.release()

        mShader?.delete()

        GLES30.glDeleteBuffers(1,mTextureId)
        GLES30.glDeleteBuffers(2,vertIds)

        VideoCameraManager.clear()
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        mFrameAvailable = true
    }
}