package com.wyc.video

import android.os.Looper
import android.widget.Toast
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

class ToastUtils {
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
    }
}