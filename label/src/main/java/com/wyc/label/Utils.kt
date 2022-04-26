package com.wyc.label

import android.content.Context
import android.graphics.*
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class Utils {
    companion object{
        @JvmStatic
        fun dpToPx(context: Context, dp: Float): Int {
            return (dp * context.resources.displayMetrics.density).toInt()
        }
        @JvmStatic
        fun sp2px(context: Context, spValue: Float): Int {
            val fontScale = context.resources.displayMetrics.scaledDensity
            return (spValue * fontScale + 0.5f).toInt()
        }
        @JvmStatic
        fun getPixel(a: Int, r: Int, g: Int, b: Int): Int {
            var newPixel = 0
            newPixel = newPixel or (a and 0xff)
            newPixel = (newPixel shl 8) or (r and 0xff)
            newPixel = (newPixel shl 8) or (g and 0xff)
            newPixel = (newPixel shl 8) or (b and 0xff)
            return newPixel
        }
        @JvmStatic
        fun drawWarnToBitmap(`in`: Bitmap): Bitmap? {
            val bitmap = `in`.copy(Bitmap.Config.ARGB_8888, true)
            val width = bitmap.width
            val height = bitmap.height
            val canvas = Canvas(bitmap)
            val paint = Paint()
            paint.color = Color.RED
            paint.strokeWidth = 2f
            paint.isAntiAlias = true
            val w = width / 5.0f
            val h = w * 3
            val left = width - w
            val top = height - h
            val rectF = RectF(left / 2f, top - w, left / 2f + w, height - w)
            canvas.drawOval(rectF, paint)
            val radius = w / 2
            canvas.drawCircle(left / 2f + radius, height - w + radius, radius, paint)
            return bitmap
        }
        @JvmStatic
        fun dpToPxF(context: Context,spValue: Float): Float {
            val fontScale = context.resources.displayMetrics.scaledDensity
            return (spValue * fontScale + 0.5f)
        }

        @JvmStatic
        fun showToast(message: String?) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Toast.makeText(LabelApp.getInstance(),message, Toast.LENGTH_LONG).show()
            } else{
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(LabelApp.getInstance(),message, Toast.LENGTH_LONG).show()
                }
            }
        }
        @JvmStatic
        fun showToast(id:Int) {
            showToast(LabelApp.getInstance().getString(id))
        }
        @JvmStatic
        fun showToast(@StringRes  id:Int,vararg formatArgs:Any){
            Toast.makeText(LabelApp.getInstance(),LabelApp.getInstance().getString(id,formatArgs), Toast.LENGTH_LONG).show()
        }
    }

}