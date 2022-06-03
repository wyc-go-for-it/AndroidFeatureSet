package com.wyc.label
import android.graphics.*
import android.view.View
import android.widget.RadioButton
import android.widget.SeekBar
import androidx.constraintlayout.widget.Group
import kotlin.math.max


/**
 *
 * @ProjectName:    AndroidClient
 * @Package:        com.wyc.cloudapp.design
 * @ClassName:      ShapeItem
 * @Description:    形状
 * @Author:         wyc
 * @CreateDate:     2022/3/16 14:21
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/3/16 14:21
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

internal open class ShapeItemBase: ItemBase() {
    init {
        width = LabelApp.getInstance().resources.getDimensionPixelOffset(R.dimen.com_wyc_label_width_88)
    }
    var borderWidth = MIN_BORDER_WIDTH
        set(value) {
            field = max(value, MIN_BORDER_WIDTH)
        }
    var dotBorderWidth = MIN_BORDER_WIDTH
        set(value) {
            field = max(value, MIN_BORDER_WIDTH)
        }

    var borderColor: Int = Color.BLACK
    var hasfill = false
    var hasDash = false
    var hasBorder = true

    companion object {
        const val serialVersionUID = 1L
    }

    override fun drawItem(offsetX: Float, offsetY: Float, canvas: Canvas, paint: Paint) {
        paint.strokeWidth = borderWidth
        paint.color = borderColor

        when {
            hasfill -> paint.style = Paint.Style.FILL
            hasBorder -> paint.style = Paint.Style.STROKE
            else -> paint.style = Paint.Style.STROKE
        }

        if (hasDash)paint.pathEffect = DashPathEffect(floatArrayOf(dotBorderWidth,dotBorderWidth),0f)

        drawShape(offsetX, offsetY, canvas,paint)

        paint.strokeWidth = MIN_BORDER_WIDTH

        if (hasfill)paint.style = Paint.Style.STROKE
        if (hasDash)paint.pathEffect = null
    }
    open fun drawShape(offsetX: Float, offsetY: Float, canvas: Canvas, paint: Paint){

    }

    override fun createItemBitmap(bgColor:Int): Bitmap {
        val offset = borderWidth
        val bmp = Bitmap.createBitmap((width + offset).toInt(), (height + offset).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.translate(-left + offset / 2, -top + offset / 2)
        c.drawColor(bgColor)
        draw(0f,0f,c, Paint())
        return bmp
    }

    override fun popMenu(labelView: LabelView) {
        val view = View.inflate(labelView.context,R.layout.com_wyc_label_shap_item_attr,null)
        showShapeEditDialog(labelView,view)
    }

    protected fun showShapeEditDialog(labelView: LabelView, view: View){
        showEditDialog(labelView.context,view)

        val fill: RadioButton = view.findViewById(R.id.fill)
        fill.isChecked = hasfill
        fill.setOnCheckedChangeListener{ _, check ->
            addAttrChange(labelView,"hasfill",hasfill,check)
            hasfill = check
            labelView.postInvalidate()
        }

        val border: RadioButton = view.findViewById(R.id.border)
        border.isChecked = hasBorder
        border.setOnCheckedChangeListener{ _, check ->
            addAttrChange(labelView,"hasBorder",hasBorder,check)
            hasBorder = check
            labelView.postInvalidate()
        }

        val dot_width: MySeekBar = view.findViewById(R.id.dot_width)
        dot_width.visibility = View.VISIBLE
        dot_width.minValue = MIN_BORDER_WIDTH.toInt()
        dot_width.max = 48 - MIN_BORDER_WIDTH.toInt()
        dot_width.progress = dotBorderWidth.toInt()
        dot_width.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                dotBorderWidth = progress.toFloat()
                labelView.postInvalidate()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                seekBar.tag = dotBorderWidth
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val oldSize = seekBar.tag as? Float ?: dotBorderWidth
                if (dotBorderWidth != oldSize){
                    addAttrChange(labelView,"dotBorderWidth",oldSize,dotBorderWidth)
                }
            }

        })
        view.findViewById<Group>(R.id.dot_group).visibility = if (hasDash){ View.VISIBLE }else View.GONE

        val dot: RadioButton = view.findViewById(R.id.dot)
        dot.isChecked = hasDash
        dot.setOnCheckedChangeListener{ _, check ->
            addAttrChange(labelView,"hasDash",hasDash,check)
            hasDash = check
            view.findViewById<Group>(R.id.dot_group).visibility = if (check){
                  View.VISIBLE
            }else View.GONE

            labelView.postInvalidate()
        }


        val bw: MySeekBar = view.findViewById(R.id.border_width)
        bw.minValue = MIN_BORDER_WIDTH.toInt()
        bw.max = 48 - MIN_BORDER_WIDTH.toInt()
        bw.progress = borderWidth.toInt()
        bw.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                borderWidth = progress.toFloat()
                labelView.postInvalidate()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                seekBar.tag = borderWidth
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val oldSize = seekBar.tag as? Float ?: borderWidth
                if (borderWidth != oldSize){
                    addAttrChange(labelView,"borderWidth",oldSize,borderWidth)
                }
            }

        })
    }

    override fun toString(): String {
        return "ShapeItemBase(borderWidth=$borderWidth, dotBorderWidth=$dotBorderWidth, borderColor=$borderColor, hasfill=$hasfill, hasDash=$hasDash, hasBorder=$hasBorder) ${super.toString()}"
    }


}