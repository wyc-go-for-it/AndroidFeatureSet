package com.wyc.video

import android.content.Context
import android.graphics.Path
import android.graphics.PointF
import android.os.Build
import android.os.Looper
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import com.wyc.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tan


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

        /**
         * @param times 用来计算线上哪个点需要计算坐标，比如 times = 1/2,则计算线段中点坐标
         *
         */
        @JvmStatic
        fun calLinePointCoordinate(
            times: Float,
            startX: Float,
            startY: Float,
            endX: Float,
            endY: Float
        ): PointF {
            val l = sqrt(
                (endX - startX).toDouble().pow(2.0) + Math.pow((endY - startY).toDouble(), 2.0)
            )
                .toFloat()
            val distance_2 = l * times
            val l_slope_radian =
                Math.atan(((endY - startY) / (endX - startX)).toDouble()).toFloat()
            val v = Math.abs(Math.cos(l_slope_radian.toDouble()) * distance_2)
            val v1 = Math.abs(Math.sin(l_slope_radian.toDouble()) * distance_2)
            var center_dynamic_x = endX - v
            var center_dynamic_y = endY - v1
            if (l_slope_radian < 0) {
                center_dynamic_x = startX + v
                center_dynamic_y = startY - v1
            }
            return PointF(center_dynamic_x.toFloat(), center_dynamic_y.toFloat())
        }
        /**
         * @param center 三角形中心点
         * @param sideLen 这个参数乘以center得出边长
         * @param round 圆角大小
         * @return 返回三角形的Path对象
         * */
        @JvmStatic
        fun calRoundTriangle(center: Float, sideLen: Float,round:Float): Path {
            val b = Path()
            val triangleSide = center * sideLen
            val firstX = (center - triangleSide / 2 * tan(Math.PI / 6)).toFloat()
            val firstY = center - triangleSide / 2
            val secondY = center + triangleSide / 2
            val thirdX = (center + triangleSide / 2 / cos(Math.PI / 6)).toFloat()
            if (round == 0f){
                b.moveTo(firstX, firstY)
                b.lineTo(firstX, secondY)
                b.lineTo(thirdX, center)
                b.lineTo(firstX, firstY)
            }else{
                b.moveTo(firstX - round / 2f, firstY + round)
                b.lineTo(firstX - round / 2f, secondY - round)
                b.quadTo(firstX - round / 2f, secondY + round / 2f, firstX + round, secondY)
                b.lineTo(thirdX - round, center + round)
                b.quadTo(thirdX, center, thirdX - round, center - round)
                b.lineTo(firstX + round, firstY)
                b.quadTo(
                    firstX - round / 2f,
                    firstY - round / 2f,
                    firstX - round / 2f,
                    firstY + round
                )
            }
            return b
        }
    }
}