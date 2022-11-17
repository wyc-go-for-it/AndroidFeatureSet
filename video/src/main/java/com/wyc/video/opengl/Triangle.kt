package com.wyc.video.opengl

import android.opengl.GLES30
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

class Triangle:IGLDraw {
    private val bytePerFloat = 4
    private var mShader:Shader? = null

    private val vertex =  floatArrayOf(0.0f,0.5f,0.0f,
                               -0.5f,-0.0f,0.0f,
                               0.5f,0.0f,0.0f,)

    private val color = floatArrayOf(1.0f,1.0f,1.0f,0.0f,
                                     0.2f,0.0f,0.0f,0.0f,
                                     0.0f,0.7f,0.0f,0.0f)

    private val vertexBuffer = ByteBuffer.allocateDirect(3 * 24000 * bytePerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private val colorBuffer = ByteBuffer.allocateDirect(color.size * bytePerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer()

    private val vertexBufferObject= IntBuffer.allocate(2)
    private val vertexArrayObject= IntBuffer.allocate(1)


    private fun initBuffer(){
        vertexBuffer.put(vertex).position(0)
        colorBuffer.put(color).rewind()

        GLES30.glGenBuffers(2,vertexBufferObject)

        GLES30.glGenVertexArrays(1,vertexArrayObject)
        GLES30.glBindVertexArray(vertexArrayObject[0])


        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vertexBufferObject[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,vertex.size * bytePerFloat,vertexBuffer,GLES30.GL_STATIC_DRAW)
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
    }

    override fun draw() {

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        //GLES30.glBindVertexArray(vertexArrayObject[0])

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vertexBufferObject[0])

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
            Log.e("xPos","$xPos ")

            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,count,vertexBuffer,GLES30.GL_DYNAMIC_DRAW)

            vertexBuffer.position(prePos)
        }
        GLES30.glDrawArrays(GLES30.GL_POINTS,0,count)
    }

    override fun updateData(buffer: FloatArray) {

        val combined  = FloatArray(buffer.size * 3)

        var index = 0
        buffer.forEach {

            combined[index] = 0f
            combined[index + 1] = it
            combined[index + 2] = 0f

            index += 3
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
}