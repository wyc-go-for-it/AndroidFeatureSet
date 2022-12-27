package com.wyc.video.camera

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.media.ImageReader
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLSurfaceView
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

    private val texCoord = floatArrayOf(1f, 1f,
                                        0f, 1f,
                                        1f, 0f,
                                        0f, 0f)

    private val texCoordFront = floatArrayOf(0f, 1f,
                                                1f, 1f,
                                                0f, 0f,
                                                1f, 0f)

    private val triVertices = floatArrayOf(
        -0.5f, -0.5f, 0.0f,
        0.5f, -0.5f, 0.0f,
        0.0f, 0.5f, 0.0f
    )
    private val triVertBufferSize = triVertices.size * Float.SIZE_BYTES
    private val triVerCoordBuffer:FloatBuffer = ByteBuffer.allocateDirect(triVertBufferSize).order(ByteOrder.nativeOrder()).asFloatBuffer()

    private val vertBufferSize = vertices.size * Float.SIZE_BYTES
    private val verCoordBuffer:FloatBuffer = ByteBuffer.allocateDirect(vertBufferSize).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val texCoordBufferSize = texCoordFront.size * Float.SIZE_BYTES
    private val texCoordBuffer = ByteBuffer.allocateDirect(texCoordBufferSize).order(ByteOrder.nativeOrder()).asFloatBuffer()

    private var mFrameAvailable = false
    private var stopDrawing = AtomicBoolean(false)

    private val vertIds = IntBuffer.allocate(3)


    private val mFrameBufferId = IntBuffer.allocate(1)
    private val mTextureId = IntBuffer.allocate(1)


    private var mFrontImageReader : ImageReader? = null
    private var mImageReaderThread: HandlerThread? = null
    private var mImageReaderHandler: Handler? = null
    private var mImageBuffer:ByteArray? = null

    init {
        verCoordBuffer.put(vertices).position(0)
        texCoordBuffer.put(texCoord).position(0)
        triVerCoordBuffer.put(triVertices).position(0)

        front()
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
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,GLES30.GL_NONE)


        // 将它附加到当前绑定的帧缓冲对象
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,GLES30.GL_TEXTURE_2D, mTextureId[0], 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)

        //初始化surface
        mSurfaceTexture = SurfaceTexture(mTextureOESId[0])
        mSurfaceTexture!!.setOnFrameAvailableListener(this)
        mSurface = Surface(mSurfaceTexture)

        //初始化相机
        GLVideoCameraManager.getInstance().addSurface(mSurface!!)

        GLVideoCameraManager.getInstance().addSurface(mFrontImageReader!!.surface)
        mFrontImageReader!!.setOnImageAvailableListener(mImageAvailableListener,mImageReaderHandler)

        //(GLVideoCameraManager.getInstance() as GLVideoCameraManager).addFrontSurface(mFrontImageReader!!.surface)


        GLVideoCameraManager.getInstance().openCamera()


        //初始化顶点
        GLES30.glGenVertexArrays(3,vertIds)
        //顶点坐标
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vertIds[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,vertBufferSize,verCoordBuffer,GLES30.GL_STATIC_DRAW)
        GLES30.glVertexAttribPointer(0,3,GLES30.GL_FLOAT,false,3 * Float.SIZE_BYTES,0)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,GLES30.GL_NONE)

        //三角形顶点坐标
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vertIds[1])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,triVertBufferSize,triVerCoordBuffer,GLES30.GL_STATIC_DRAW)
        GLES30.glVertexAttribPointer(2,3,GLES30.GL_FLOAT,false,3 * Float.SIZE_BYTES,0)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,GLES30.GL_NONE)

        //纹理坐标
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vertIds[2])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,texCoordBufferSize,texCoordBuffer,GLES30.GL_STATIC_DRAW)
        GLES30.glVertexAttribPointer(1,2,GLES30.GL_FLOAT,false,2 * Float.SIZE_BYTES,0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,GLES30.GL_NONE)
    }

    private fun front( ){
        startImageReaderThread()
        mFrontImageReader = ImageReader.newInstance(GLVideoCameraManager.getInstance().getVWidth(),GLVideoCameraManager.getInstance().getVHeight(),ImageFormat.JPEG,2)
    }

    private fun startImageReaderThread() {
        stopImageReaderThread()
        mImageReaderThread = HandlerThread("ImageReaderThread")
        mImageReaderThread!!.start()
        mImageReaderHandler = Handler(mImageReaderThread!!.looper)
    }

    private fun stopImageReaderThread() {
        if (mImageReaderThread != null) {
            mImageReaderThread!!.quitSafely()
            try {
                mImageReaderThread!!.join()
                mImageReaderThread = null
                mImageReaderHandler = null
            } catch (ignore: InterruptedException) {
            }
        }
    }

    private val mImageAvailableListener = ImageReader.OnImageAvailableListener {
        Utils.logInfo("width:${it.width},height:${it.height},format:${it.imageFormat}")

        val image = it.acquireLatestImage()
        val byteBuffer = image.planes[0].buffer


        synchronized(this){
            mImageBuffer = ByteArray(byteBuffer.remaining());
            byteBuffer.get(mImageBuffer!!)
        }
        image.close()

    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES30.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
        GLES30.glViewport(0,0,width,height)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,mTextureId[0])
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGB, width, height, 0, GLES30.GL_RGB, GLES30.GL_UNSIGNED_BYTE, null)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
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
        GLES30.glClearColor( 0f,  0f,  0f, 1.0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        GLES30.glUniform1i(GLES30.glGetUniformLocation(mShader!!.program(), "hasTri"), 1)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vertIds[1])
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP,0,3)


        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[0])

        synchronized(this){
            if (mImageBuffer != null)
                GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGB, 1280, 720, 0, GLES30.GL_RGB, GLES30.GL_UNSIGNED_BYTE, ByteBuffer.wrap(mImageBuffer!!))
        }

        GLES20.glUniform1i(GLES20.glGetUniformLocation(mShader!!.program(), "sTexture"), 1)



        //返回默认缓冲区
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,0)
        GLES30.glClear(0)


        //绘制纹理
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glUniform1i(GLES30.glGetUniformLocation(mShader!!.program(), "hasTri"), 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP,0,4)
    }

    fun clear(){
        stopDrawing.set(true)

        mSurfaceTexture!!.release()
        mSurface?.release()
        GLVideoCameraManager.clear()

        if (mFrontImageReader != null){
            mFrontImageReader!!.close()
            mFrontImageReader = null
        }
        stopImageReaderThread()
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        mFrameAvailable = true
    }
}