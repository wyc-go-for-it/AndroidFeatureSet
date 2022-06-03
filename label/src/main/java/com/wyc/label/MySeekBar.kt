package com.wyc.label

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar
import kotlin.math.abs


/**
 *
 * @ProjectName:    AndroidClient
 * @Package:        com.wyc.cloudapp.customizationView
 * @ClassName:      MySeekBar
 * @Description:    作用描述
 * @Author:         wyc
 * @CreateDate:     2022/4/8 18:29
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/4/8 18:29
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class MySeekBar: AppCompatSeekBar {
    private val rect = Rect()
    private val paint = Paint()
    var minValue = 0

    constructor(context: Context):this(context, null)
    constructor(context: Context, attrs: AttributeSet?):this(context, attrs, R.attr.seekBarStyle)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int):super(context, attrs, defStyleAttr){
        paint.isAntiAlias = true
        paint.textSize = context.resources.getDimension(R.dimen.com_wyc_label_font_size_16)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cur = (progress + minValue).toString()

        paint.getTextBounds(cur,0,cur.length,rect)

        val baseLineY = height / 2 + (abs(paint.fontMetrics.ascent) - paint.fontMetrics.descent) / 2
        canvas.drawText(cur, (width - rect.width() - thumbOffset / 2).toFloat(),baseLineY,paint)
    }
}