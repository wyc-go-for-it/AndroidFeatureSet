package com.wyc.cloudapp.design

import android.graphics.Canvas
import android.graphics.Paint
import com.wyc.label.LabelApp
import com.wyc.label.R
import com.wyc.label.ShapeItemBase


/**
 *
 * @ProjectName:    AndroidClient
 * @Package:        com.wyc.cloudapp.design
 * @ClassName:      CircleItem
 * @Description:    作用描述
 * @Author:         wyc
 * @CreateDate:     2022/3/23 13:57
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/3/23 13:57
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class CircleItem: ShapeItemBase() {
    init {
        height = LabelApp.getInstance().resources.getDimensionPixelOffset(R.dimen.com_wyc_label_height_88)
    }

    override fun drawShape(offsetX: Float, offsetY: Float, canvas: Canvas, paint: Paint) {
        canvas.drawOval(left + offsetX,top + offsetY,left + offsetX + width,top + offsetY + height,paint)
    }

}