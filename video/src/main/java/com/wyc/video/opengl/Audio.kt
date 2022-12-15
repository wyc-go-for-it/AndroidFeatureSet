package com.wyc.video.opengl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.Matrix
import android.os.SystemClock
import android.util.Log
import com.wyc.logger.Logger
import com.wyc.video.R
import com.wyc.video.VideoApp
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.*
import java.util.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.video.opengl
 * @ClassName:      Triangle
 * @Description:    三角形
 * @Author:         wyc
 * @CreateDate:     2022/11/11 15:33
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/11/11 15:33
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class Audio:IGLDraw {
    private val bytePerFloat = 4
    private val sampleSize = 48000
    private val bufferSize = 3 * sampleSize * bytePerFloat
    private var mShader:Shader? = null

    private val vertexBuffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val lColor = floatArrayOf(0.3f,0.6f,0.8f,0f)
    private var timeValue = 0.01f

    private val vertexBufferObject= IntBuffer.allocate(1)
    private var colorHandle = -1
    private var timeValueHandle = -1
    private var timeValueHandle1 = -1

    private val textureObj = IntBuffer.allocate(1)
    private var textureHandle = -1

    override fun init() {
        mShader = Shader(R.raw.vertex,R.raw.fragment)
        mShader!!.use()

        initBuffer()

        initColor()

        initTexture()
    }

    private fun initTexture(){
        GLES30.glGenTextures(1,textureObj)
        GLES30.glActiveTexture(0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,textureObj[0])

        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)

        val img = BitmapFactory.decodeResource(VideoApp.getInstance().resources,R.raw.orange)

        val outputStream = ByteArrayOutputStream()
        img.compress(Bitmap.CompressFormat.JPEG,100,outputStream)
        val bytes = outputStream.toByteArray()
        outputStream.close()

        val bBuffer = ByteBuffer.wrap(bytes)

        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D,0,GLES30.GL_RGB,img.width,img.height,0,GLES30.GL_RGB,GLES30.GL_UNSIGNED_BYTE,bBuffer)
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)

        textureHandle = GLES30.glGetUniformLocation(mShader!!.program(),"ourTexture")
        GLES30.glUniform1i(textureHandle,0)
        Logger.d("textureHandle:%d",textureHandle)
    }

    private fun initBuffer(){
        GLES30.glGenBuffers(1,vertexBufferObject)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vertexBufferObject[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,bufferSize,vertexBuffer,GLES30.GL_DYNAMIC_DRAW)
        GLES30.glVertexAttribPointer(0,3,GLES30.GL_FLOAT,false,3 * bytePerFloat,0)
        GLES30.glEnableVertexAttribArray(0)
    }

    private fun initColor(){
        colorHandle = GLES30.glGetUniformLocation(mShader!!.program(),"uColor")
        GLES30.glUniform4fv(colorHandle,1,lColor,0)

        timeValueHandle = GLES30.glGetUniformLocation(mShader!!.program(),"timeValue")
        GLES30.glUniform1f(timeValueHandle,timeValue)

        timeValueHandle1 = GLES30.glGetUniformLocation(mShader!!.program(),"timeValue")
        GLES30.glUniform1f(timeValueHandle1,timeValue)

        Logger.d("colorHandle:%d,timeValueHandle:%d",colorHandle,timeValueHandle)
    }

    private fun updateColor(){
        timeValue += 0.01f
        GLES30.glUniform1f(timeValueHandle,timeValue)
        GLES30.glUniform1f(timeValueHandle1,timeValue)
    }

    override fun draw() {
        var count:Int
        updateColor()
        synchronized(this){
            val prePos = vertexBuffer.position()
            vertexBuffer.rewind()

            count = vertexBuffer.limit()

            if (count == 0)return

            val step = (2f / (count.toFloat() / 3f))
            var xPos = -1f
            for (i in 0 until count step 3){
                xPos += step
                vertexBuffer.put(i,xPos)
            }

            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vertexBufferObject[0])
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,bufferSize,vertexBuffer,GLES30.GL_DYNAMIC_DRAW)

            vertexBuffer.position(prePos)
        }
        GLES30.glDrawArrays(GLES30.GL_POINTS,0,count)
    }

    override fun updateData(buffer: FloatArray) {

        val combined  = FloatArray(buffer.size * 3)

        var index = 0
        var l = 0
        var r = 1
        val end = buffer.size shr  1
        for (i in 0 until end){
            combined[index] = 0f
            combined[index + 1] = buffer[l] + 0.5f
            combined[index + 2] = 0f

            combined[index + 3] = 0f
            combined[index + 4] =  buffer[r] - 0.5f
            combined[index + 5] = 0f

            index += 6
            l += 2
            r += 2
        }

        val bSize = combined.size

        synchronized(this){

        val capacity = vertexBuffer.capacity()
        val prePos = vertexBuffer.position()

            if (bSize >= capacity){
                vertexBuffer.clear()
                vertexBuffer.put(combined,0,vertexBuffer.limit())
            }else{
                if (prePos + bSize > capacity){
                    vertexBuffer.position(bSize)
                    vertexBuffer.compact()
                }
                vertexBuffer.put(combined)
            }
        }

    }

    override fun sizeChanged(w: Int, h: Int) {

    }
}