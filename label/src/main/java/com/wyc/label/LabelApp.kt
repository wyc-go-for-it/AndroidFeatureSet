package com.wyc.label

import android.app.Application
import android.graphics.Color


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
        @JvmStatic
        fun initApp(application: Application){
            app = application
        }
        @JvmStatic
        fun getInstance():Application{
            return app!!
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