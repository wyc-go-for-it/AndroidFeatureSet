package com.wyc.video

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.label
 * @ClassName:      App
 * @Description:    保存第三放变量
 * @Author:         wyc
 * @CreateDate:     2022/4/22 11:19
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/4/22 11:19
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class VideoApp {
    companion object{
        private var app:Application? = null
        private var appColor:Int = Color.parseColor("#67B0F8")
        @JvmStatic
        fun initApp(application: Application){
            app = application
        }
        @JvmStatic
        fun getInstance():Application{
            return app!!
        }
        @JvmStatic
        fun getVideoDir():String{
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
        @JvmStatic
        fun initThemeColor(c:Int){
            appColor = c
        }
        @JvmStatic
        fun themeColor():Int{
            return appColor
        }
    }
}