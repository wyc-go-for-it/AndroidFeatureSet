package com.wyc.video.camera

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.media.ImageReader
import android.opengl.*
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import com.wyc.logger.Logger
import com.wyc.video.R
import com.wyc.video.Utils
import com.wyc.video.opengl.Shader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.concurrent.atomic.AtomicBoolean
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
    private val mTextureOESId = IntBuffer.allocate(1)
    private var mShader:Shader? = null


    private val vertices = floatArrayOf(-1f,-1f,0f,
                                        -1f,1f,0f,
                                        1f,-1f,0f,
                                        1f,1f,0f)

    private val texCoord = floatArrayOf(0f, 0f,
                                        0f, 1f,
                                        1f, 0f,
                                        1f, 1f)

    private val vertBufferSize = vertices.size * Float.SIZE_BYTES
    private val verCoordBuffer:FloatBuffer = ByteBuffer.allocateDirect(vertBufferSize).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val texCoordBufferSize = texCoord.size * Float.SIZE_BYTES
    private val texCoordBuffer = ByteBuffer.allocateDirect(texCoordBufferSize).order(ByteOrder.nativeOrder()).asFloatBuffer()

    private var mFrameAvailable = false
    private var stopDrawing = AtomicBoolean(false)

    private val vertIds = IntBuffer.allocate(3)


    private val mFrameBufferId = IntBuffer.allocate(1)
    private val mTextureId = IntBuffer.allocate(1)
    private var mZoomHandle = -1
    private val mZoomMatrix = FloatArray(16)
    private var mZoomFactor = 0.0f
    private var mIndex = 0


    private var mWidth = 0
    private var mHeight = 0

    private var mPixelBuffer = ByteBuffer.allocate(1)
    private var mReadType = IntBuffer.allocate(1)
    private var mReadFormat = IntBuffer.allocate(1)
    private var mBytesPerPixel = 0
    private var mReadPixelListener:OnReadPixel? = null

    init {
        verCoordBuffer.put(vertices).position(0)
        texCoordBuffer.put(texCoord).position(0)
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig?) {
        stopDrawing.set(false)
        init()
    }

    private fun init(){
        mShader = Shader(R.raw.camera_vertex,R.raw.camera_frag)
        mShader!!.use()

        //分配相机纹理对象
        GLES30.glGenTextures(1,mTextureOESId)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_SAMPLER_EXTERNAL_OES,mTextureOESId[0])
        GLES30.glTexParameteri(GLES11Ext.GL_SAMPLER_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES30.glTexParameteri(GLES11Ext.GL_SAMPLER_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        GLES30.glTexParameteri(GLES11Ext.GL_SAMPLER_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_SAMPLER_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES30.glGenerateMipmap(GLES11Ext.GL_SAMPLER_EXTERNAL_OES)
        GLES30.glBindTexture(GLES11Ext.GL_SAMPLER_EXTERNAL_OES,GLES30.GL_NONE)

        //初始化surface
        mSurfaceTexture = SurfaceTexture(mTextureOESId[0])
        mSurfaceTexture!!.setOnFrameAvailableListener(this)
        mSurface = Surface(mSurfaceTexture)

        //初始化相机
        GLVideoCameraManager.getInstance().addSurface(mSurface!!)
        GLVideoCameraManager.getInstance().openCamera()

        //初始化顶点
        GLES30.glGenVertexArrays(3,vertIds)
        //顶点坐标
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vertIds[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,vertBufferSize,verCoordBuffer,GLES30.GL_STATIC_DRAW)
        GLES30.glVertexAttribPointer(0,3,GLES30.GL_FLOAT,false,3 * Float.SIZE_BYTES,0)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,GLES30.GL_NONE)

        //纹理坐标
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vertIds[2])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,texCoordBufferSize,texCoordBuffer,GLES30.GL_STATIC_DRAW)
        GLES30.glVertexAttribPointer(1,2,GLES30.GL_FLOAT,false,2 * Float.SIZE_BYTES,0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,GLES30.GL_NONE)

        mZoomHandle = GLES30.glGetUniformLocation(mShader!!.program(),"zoom")
        Matrix.setIdentityM(mZoomMatrix,0)
    }

    private fun initFbo(w:Int,h:Int){
        //清除缓冲
        GLES30.glDeleteBuffers(1,mTextureId)
        GLES30.glDeleteFramebuffers(1,mFrameBufferId)

        //生成帧缓冲对象
        GLES30.glGenFramebuffers(1,mFrameBufferId)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFrameBufferId[0])


        //分配帧缓冲纹理对象
        GLES30.glGenTextures(1,mTextureId)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,mTextureId[0])

        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, w, h, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,GLES30.GL_NONE)


        // 将它附加到当前绑定的帧缓冲对象
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,GLES30.GL_TEXTURE_2D, mTextureId[0], 0)
        if(GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER) == GLES30.GL_FRAMEBUFFER_COMPLETE){
            Logger.d("FRAMEBUFFER FRAMEBUFFER COMPLETE")
        }

        GLES30.glReadBuffer(GLES30.GL_COLOR_ATTACHMENT0)
        GLES30.glGetIntegerv(GLES30.GL_IMPLEMENTATION_COLOR_READ_TYPE,mReadType)
        GLES30.glGetIntegerv(GLES30.GL_IMPLEMENTATION_COLOR_READ_FORMAT,mReadFormat)

        when(mReadType[0]){
            GLES30.GL_UNSIGNED_BYTE,GLES30.GL_BYTE->{
                when(mReadFormat[0]){
                    GLES30.GL_RGBA->{
                        mBytesPerPixel = 4
                    }
                    GLES30.GL_RGB,GLES30.GL_RGB_INTEGER->{
                        mBytesPerPixel = 3
                    }
                    GLES30.GL_RG,GLES30.GL_RG_INTEGER,GLES30.GL_LUMINANCE_ALPHA->{
                        mBytesPerPixel = 2
                    }
                    GLES30.GL_RED,GLES30.GL_RED_INTEGER,GLES30.GL_ALPHA,GLES30.GL_LUMINANCE->{
                        mBytesPerPixel = 1
                    }
                    else->{
                        mBytesPerPixel = -1
                    }
                }
            }
            GLES30.GL_FLOAT,GLES30.GL_INT,GLES30.GL_UNSIGNED_INT->{
                when(mReadFormat[0]){
                    GLES30.GL_RGBA,GLES30.GL_RGBA_INTEGER->{
                        mBytesPerPixel = 16
                    }
                    GLES30.GL_RGB,GLES30.GL_RGB_INTEGER->{
                        mBytesPerPixel = 12
                    }
                    GLES30.GL_RG,GLES30.GL_RG_INTEGER ->{
                        mBytesPerPixel = 8
                    }
                    GLES30.GL_RED->{
                        mBytesPerPixel = 4
                    }
                    else->{
                        mBytesPerPixel = -1
                    }
                }
            }
        }
        Logger.d("readType:%d,readFormat:%d,bytesPerPixel:%d",mReadType[0],mReadFormat[0],mBytesPerPixel)
        mPixelBuffer = ByteBuffer.allocate(w * h * mBytesPerPixel)
        mPixelBuffer.order(ByteOrder.nativeOrder())

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        mWidth = width
        mHeight = height

        GLES30.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
        GLES30.glViewport(0,0,width,height)

        initFbo(width,height)
    }

    override fun onDrawFrame(gl: GL10) {
        if (stopDrawing.get()){
            mShader?.delete()
            GLES30.glDeleteBuffers(1,mTextureOESId)
            GLES30.glDeleteBuffers(1,mTextureId)
            GLES30.glDeleteBuffers(3,vertIds)
            GLES30.glDeleteFramebuffers(1,mFrameBufferId)
            Logger.d("clear gl buffer")
            return
        }

        mSurfaceTexture!!.updateTexImage()



        //启用自定义帧缓冲区
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,mFrameBufferId[0])
        GLES30.glClearColor( 0f,  0f,  0f, 1.0f)


        GLES30.glUniform1i(GLES30.glGetUniformLocation(mShader!!.program(), "hasTri"), 1)

        if (mIndex ++ % 15 == 0){
            mZoomFactor = if (mZoomFactor == 1.02f){
                0.95f
            }else 1.02f
        }

        Matrix.setIdentityM(mZoomMatrix,0)
        Matrix.translateM(mZoomMatrix,0,0.5f,0.5f,0f)
        Matrix.scaleM(mZoomMatrix,0,mZoomFactor,mZoomFactor,1.0f)
        Matrix.translateM(mZoomMatrix,0,-0.5f,-0.5f,0f)

        GLES30.glUniformMatrix4fv(mZoomHandle,1,false,mZoomMatrix,0)


        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP,0,4)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[0])
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mShader!!.program(), "sTexture"),1)

        //读像素
        mReadPixelListener?.apply {
            GLES30.glReadPixels(0,0,mWidth,mHeight,mReadFormat[0],mReadType[0],mPixelBuffer)
            //onRead(mWidth,mHeight,mPixelBuffer.array())
        }


        //返回默认缓冲区
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,0)
        GLES30.glClear(0)

        //绘制OES纹理
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(mShader!!.program(), "hasTri"), 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP,0,4)
    }

    fun clear(){
        stopDrawing.set(true)

        mSurfaceTexture!!.release()
        mSurface?.release()
        GLVideoCameraManager.clear()
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        mFrameAvailable = true
    }

    fun setReadPixelListener(listener:OnReadPixel){
        mReadPixelListener = listener
    }

    interface OnReadPixel{
        fun onRead(w: Int,h: Int,byteBuffer: ByteArray)
    }
}