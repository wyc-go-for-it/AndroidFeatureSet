package com.wyc.label

import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.widget.SeekBar


/**
 *
 * @ProjectName:    AndroidClient
 * @Package:        com.wyc.cloudapp.design
 * @ClassName:      RectItem
 * @Description:    画矩形
 * @Author:         wyc
 * @CreateDate:     2022/3/23 13:37
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/3/23 13:37
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class RectItem: ShapeItemBase() {
    init {
        height = App.getInstance().resources.getDimensionPixelOffset(R.dimen.com_wyc_label_height_88)
    }
    var rc = 0f
    override fun drawShape(offsetX: Float, offsetY: Float, canvas: Canvas, paint: Paint) {
        canvas.drawRoundRect(left + offsetX,top + offsetY,left + offsetX + width,top + offsetY + height,rc,rc,paint)
    }

    override fun popMenu(labelView: LabelView) {
        val view = View.inflate(labelView.context,R.layout.com_wyc_label_rect_item_attr,null)
        super.showShapeEditDialog(labelView,view)

        val round: MySeekBar = view.findViewById(R.id.round)
        round.minValue = 0
        round.max = 48
        round.progress = rc.toInt()
        round.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                rc = progress.toFloat()
                labelView.postInvalidate()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                seekBar.tag = rc
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val oldSize = seekBar.tag as? Float ?: rc
                if (rc != oldSize){
                    addAttrChange(labelView,"rc",oldSize,rc)
                }
            }

        })
    }

    override fun toString(): String {
        return "RectItem(rc=$rc) ${super.toString()}"
    }

}