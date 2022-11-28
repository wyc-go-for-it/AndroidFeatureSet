package com.wyc.video.opengl

import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import com.wyc.video.R
import java.nio.*


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

    private val color = floatArrayOf(0.0f,1.0f,1.0f,0.0f)

    private val vertexBuffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val colorBuffer = ByteBuffer.allocateDirect(color.size * bytePerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer()

    private val vertexBufferObject= IntBuffer.allocate(2)


    private var vProjectionHandle: Int = 0
    private var vViewHandle: Int = 0
    private var vModelHandle: Int = 0

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    private fun initBuffer(){
        colorBuffer.put(color).position(0)

        GLES30.glGenBuffers(2,vertexBufferObject)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vertexBufferObject[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,bufferSize,vertexBuffer,GLES30.GL_DYNAMIC_DRAW)
        GLES30.glVertexAttribPointer(0,3,GLES30.GL_FLOAT,false,3 * bytePerFloat,0)
        GLES30.glEnableVertexAttribArray(0)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vertexBufferObject[1])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,color.size * bytePerFloat,colorBuffer,GLES30.GL_STATIC_DRAW)
        GLES30.glVertexAttribPointer(1,4,GLES30.GL_FLOAT,false,4 * bytePerFloat,0)
        GLES30.glEnableVertexAttribArray(1)
    }

    override fun init() {
        initBuffer()
        mShader = Shader(R.raw.vertex,R.raw.fragment)
        mShader!!.use()

        vProjectionHandle = GLES30.glGetUniformLocation(mShader!!.program(), "projection")
        vViewHandle = GLES30.glGetUniformLocation(mShader!!.program(), "view")
        vModelHandle = GLES30.glGetUniformLocation(mShader!!.program(), "model")
    }

    override fun draw() {
        GLES30.glUniformMatrix4fv(vProjectionHandle, 1, false, projectionMatrix, 0)
        GLES30.glUniformMatrix4fv(vViewHandle, 1, false, viewMatrix, 0)
        GLES30.glUniformMatrix4fv(vModelHandle, 1, false, modelMatrix, 0)

        var count:Int
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

            combined[index + 3] = 0.1f
            combined[index + 4] = buffer[r] - 0.5f
            combined[index + 5] = 0.2f

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
        val ratio: Float = w.toFloat() / h.toFloat()

        Matrix.perspectiveM(projectionMatrix,0,30f,ratio,1f,100f)

        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)

        Matrix.setIdentityM(modelMatrix,0)
        Matrix.rotateM(modelMatrix,0,-60f,0.0f,0.0f,0.8f)
    }
}