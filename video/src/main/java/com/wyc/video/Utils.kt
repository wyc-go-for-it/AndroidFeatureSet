package com.wyc.video

import android.content.Context
import android.os.Build
import android.os.Looper
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import com.wyc.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.video
 * @ClassName:      ToastUtils
 * @Description:    提示工具
 * @Author:         wyc
 * @CreateDate:     2022/6/1 11:16
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/6/1 11:16
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class Utils {
    companion object{
        @JvmStatic
        fun showToast(message: String?) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Toast.makeText(VideoApp.getInstance(),message, Toast.LENGTH_LONG).show()
            } else{
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(VideoApp.getInstance(),message, Toast.LENGTH_LONG).show()
                }
            }
        }
        @JvmStatic
        fun hasNatureRotation(context: Context):Boolean{
            val o = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                context.display?.rotation?:0
            }else{
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
            }

            return o == Surface.ROTATION_0 || o == Surface.ROTATION_180
        }
        @JvmStatic
        fun getScreenWidth(context: Context):Int{
            val wm = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                wm.currentWindowMetrics.bounds.width()
            }else{
                wm.defaultDisplay.width
            }
        }
        @JvmStatic
        fun logInfo(errMsg:String?){
            if (errMsg != null) {
                //Log.e(this::class.simpleName,errMsg)
                Logger.d(errMsg)
            }
        }
    }
}