package com.wyc.label

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Environment
import androidx.core.content.ContextCompat
import com.wyc.label.printer.IType
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

class LabelApp {
    companion object{
        private var app:Application? = null
        private var appColor:Int = Color.parseColor("#67B0F8")
        private val driverList  = mutableListOf<IType>()

        /**
         * @param name 显示在参数设置选择列表里
         * @param classPath 实现类名全路径
         * @param type 0 网口打印机 1蓝牙打印机 2 驱动打印或使用厂家SDK
         * */
        @JvmStatic
        fun <T:IType> register(cls:Class<T>){
            cls.enumConstants?.forEach {
                driverList.add(it)
            }
        }
        @JvmStatic
        internal fun getDriverList():List<IType>{
            return driverList
        }
        @JvmStatic
        fun initApp(application: Application){
            app = application
        }
        @JvmStatic
        fun getInstance():Application{
            return app!!
        }
        @JvmStatic
        fun getDir():String{
            return if (ContextCompat.checkSelfPermission(app!!, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                String.format(
                    "%s%s%s%s",
                    Environment.getExternalStorageDirectory().absolutePath,
                    File.separator,
                    "template",
                    File.separator
                )
            }else{
                String.format("%s%s%s%s", app!!.filesDir, File.separator, "template", File.separator)
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