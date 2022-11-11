package com.wyc.video.opengl

import android.opengl.GLES30
import android.util.Log
import com.wyc.logger.Logger
import com.wyc.video.R
import com.wyc.video.VideoApp
import java.io.BufferedReader
import java.io.IOException
import java.lang.StringBuilder
import java.nio.IntBuffer


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.video.opengl
 * @ClassName:      Shader
 * @Description:    着色器
 * @Author:         wyc
 * @CreateDate:     2022/11/11 14:21
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/11/11 14:21
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class Shader(vertexCode: String, fragCode: String) {
    private var mProgramId = -1

    init {
        val vertexId = compileShader(GLES30.GL_VERTEX_SHADER, vertexCode)
        val fragId = compileShader(GLES30.GL_FRAGMENT_SHADER,fragCode)
        mProgramId = getProgram(vertexId,fragId)
    }

    constructor(vertexRes:Int,fragRes:Int):this(loadStringFromFile(vertexRes),loadStringFromFile(fragRes))

    fun use(){
        GLES30.glUseProgram(mProgramId)
    }

    fun program():Int{
        return mProgramId
    }

    fun delete(){
        if (mProgramId == -1){
            GLES30.glDeleteProgram(mProgramId)
            mProgramId = -1
        }
    }

    protected fun finalize(){
        delete()
    }

    companion object{
        private const val shaderCompileError = -1
        private const val programLinkError = -2
        /**
         * @param type GLES30.GL_VERTEX_SHADER 顶点着色器，GLES30.GL_FRAGMENT_SHADER 片段着色器
         *
         * */
        @JvmStatic
        fun compileShader(type:Int,shaderCode:String):Int{
            val shaderId = GLES30.glCreateShader(type)
            GLES30.glShaderSource(shaderId,shaderCode)
            GLES30.glCompileShader(shaderId)
            val status = IntBuffer.allocate(1)
            GLES30.glGetShaderiv(shaderId,GLES30.GL_COMPILE_STATUS,status)
            if (status.get() == 0){
                Log.e("compileShader",GLES30.glGetShaderInfoLog(shaderId))
                return shaderCompileError
            }
            return shaderId
        }
        @JvmStatic
        fun getProgram(vertexShaderId:Int,fragShaderId:Int):Int{
            val id = GLES30.glCreateProgram()
            GLES30.glAttachShader(id,vertexShaderId)
            GLES30.glAttachShader(id,fragShaderId)
            GLES30.glLinkProgram(id)

            GLES30.glDeleteShader(vertexShaderId)
            GLES30.glDeleteShader(fragShaderId)

            val status = IntBuffer.allocate(1)
            GLES30.glGetProgramiv(id,GLES30.GL_LINK_STATUS,status)
            if (status.get() == 0){
                Log.e("getProgram",GLES30.glGetProgramInfoLog(id))
                return programLinkError
            }

            return id
        }
        @JvmStatic
        fun loadStringFromFile(resourceId:Int):String{
            val resource = VideoApp.getInstance().resources
            try {
                BufferedReader(resource.openRawResource(resourceId).reader()).use {
                    val sb = StringBuilder()
                    var line:String? = it.readLine()
                    while (line != null){
                        sb.append(line)
                        sb.append("\n")
                        line  = it.readLine()
                    }
                    return sb.toString()
                }
            }catch (e: IOException){
                e.printStackTrace()
            }
            return ""
        }

    }
}