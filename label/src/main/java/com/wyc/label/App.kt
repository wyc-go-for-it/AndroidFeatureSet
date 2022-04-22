package com.wyc.label

import android.app.Application


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.label
 * @ClassName:      App
 * @Description:    作用描述
 * @Author:         wyc
 * @CreateDate:     2022/4/22 11:19
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/4/22 11:19
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class App {
    companion object{
        private var app:Application? = null
        @JvmStatic
        fun initApp(application: Application){
            app = application
        }
        fun getInstance():Application{
            return app!!
        }
    }
}