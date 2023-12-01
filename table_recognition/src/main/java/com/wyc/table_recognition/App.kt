package com.wyc.table_recognition

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import com.wyc.table_recognition.bean.AccessToken
import com.wyc.table_recognition.bean.JsonUtils
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.File
import java.io.IOException


class App {
    companion object {
        private var app: Application? = null
        private var appColor: Int = Color.parseColor("#67B0F8")
        @JvmStatic
        fun initThemeColor(c:Int){
            appColor = c
        }
        @JvmStatic
        fun themeColor():Int{
            return appColor
        }
        @JvmStatic
        fun initApp(application: Application){
            app = application
            getAccessToken()
        }
        @JvmStatic
        fun getInstance(): Application {
            return app!!
        }
        @JvmStatic
        fun getPicDir():String{
            return if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else ContextCompat.checkSelfPermission(app!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                String.format(
                    "%s%s%s%s",
                    Environment.getExternalStorageDirectory().absolutePath,
                    File.separator,
                    "wycVideo",
                    File.separator
                )
            }else{
                String.format("%s%s%s%s", app!!.filesDir, File.separator, "wycVideo", File.separator)
            }
        }
        const val grant_type = "client_credentials"
        const val client_id = "GwiPOk6EwA19Iz0SqUFvbXNT"
        const val client_secret = "apPiXweRVhbNdfU04g2wGN2gdfExNv35"

        private fun getAccessToken(){
            val accessToken = AccessToken.load()
            if (accessToken.access_token == ""){
                val url = "https://aip.baidubce.com/oauth/2.0/token?grant_type=$grant_type&client_id=$client_id&client_secret=$client_secret"
                HttpUtils.sendAsyncPost(url,"").enqueue(object : Callback{
                    override fun onFailure(call: Call, e: IOException) {

                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful){
                            val c = response.body!!.string()
                            val access:AccessToken = JsonUtils.string2Object(c)
                            Log.e("getAccessToken",access.toString())
                            access.start_in = (System.currentTimeMillis() / 1000).toInt()
                            access.save()
                        }
                    }
                })
            }
        }
    }
}