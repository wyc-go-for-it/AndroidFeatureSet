package com.wyc.label

import android.graphics.Canvas
import android.graphics.Paint
import java.io.ObjectStreamException


/**
 *
 * @ProjectName:    AndroidClient
 * @Package:        com.wyc.cloudapp.design
 * @ClassName:      LineItem
 * @Description:    画线
 * @Author:         wyc
 * @CreateDate:     2022/3/23 9:36
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/3/23 9:36
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

internal class LineItem: ShapeItemBase() {
    init {
        height = borderWidth.toInt()
    }

    @Throws(ObjectStreamException::class)
    private fun readResolve(): Any {
        serializableInit()
        return this
    }
    companion object {
        const val serialVersionUID = 1L
    }

    override fun drawShape(offsetX: Float, offsetY: Float, canvas: Canvas, paint: Paint) {
        canvas.drawLine(left + offsetX,top + offsetY + borderWidth / 2f,
            left + offsetX + width,top + offsetY + borderWidth / 2f,paint)
    }

    override fun scale(scaleX: Float, scaleY: Float) {
        width += scaleX.toInt()
        if (borderWidth >= MIN_BORDER_WIDTH || scaleY > 0f)borderWidth += scaleY
        height = borderWidth.toInt()
    }

}