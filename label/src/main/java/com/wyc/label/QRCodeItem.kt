package com.wyc.label

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.Group
import java.io.ObjectStreamException
import kotlin.math.min


/**
 *
 * @ProjectName:    AndroidClient
 * @Package:        com.wyc.cloudapp.design
 * @ClassName:      QRCodeItem
 * @Description:    二维码
 * @Author:         wyc
 * @CreateDate:     2022/4/11 18:55
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/4/11 18:55
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

internal class QRCodeItem: CodeItemBase()  {

    val minFontSize = LabelApp.getInstance().resources.getDimension(R.dimen.com_wyc_label_font_size_14)
    var fontSize =  LabelApp.getInstance().resources.getDimension(R.dimen.com_wyc_label_font_size_14)

    @Transient private  var mBottomMarge = Rect()

    init {
        width = 231
        height = width
        cBarcodeFormat = BAROMETER.QRCODE

        generateBitmap()

        initFormat()
    }
    @Throws(ObjectStreamException::class)
    private fun readResolve(): Any {
        if (cSupportFormatList == null){
            cSupportFormatList = mutableListOf()
        }
        initFormat()
        serializableInit()
        return this
    }

    private fun initFormat(){
        BAROMETER.values().forEach {
            if (it == BAROMETER.QRCODE || it == BAROMETER.SongTi){
                cSupportFormatList.add(it)
            }
        }
    }

    companion object {
        const val serialVersionUID = 1L
    }

    override fun scale(scaleX: Float, scaleY: Float) {
        if(cBarcodeFormat == BAROMETER.SongTi){
            super.scale(scaleX, scaleY)
        }else{
            width += scaleX.coerceAtLeast(scaleY).toInt()
            height = width
        }
    }

    override fun transform(scaleX: Float, scaleY: Float) {
        super.transform(scaleX, scaleY)
        fontSize *= min(scaleX,scaleY)
    }

    override fun serializableInit() {
        mBottomMarge = Rect()
        super.serializableInit()
    }


    override fun drawItem(offsetX: Float, offsetY: Float, canvas: Canvas, paint: Paint) {
        if (cBarcodeFormat == BAROMETER.SongTi) {
            drawContent(left + offsetX,top + offsetY,canvas,paint)
        }else  {
            super.drawItem(offsetX, offsetY, canvas, paint)
        }
    }

    private fun drawContent(l: Float, t: Float, canvas: Canvas, paint: Paint){
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        paint.textSize = fontSize

        paint.getTextBounds(content,0,content.length,mBottomMarge)
        val textHeight = mBottomMarge.height()
        mBottomMarge.bottom += LabelApp.getInstance().resources.getDimensionPixelSize(R.dimen.com_wyc_label_size_4)
        mBottomMarge.right += (width - mBottomMarge.width())
        mBottomMarge.offsetTo(l.toInt(), (height - mBottomMarge.height() + t).toInt())
        canvas.drawRect(mBottomMarge,paint)
        paint.color = Color.BLACK

        var textWidth = 0f
        content.forEach {c->
            textWidth += paint.measureText(c.toString())
        }
        val letterSpacing = ((mBottomMarge.width() - textWidth) / (content.length - 1)) + textWidth / content.length
        val textY = mBottomMarge.bottom - (mBottomMarge.height() - textHeight) / 2f
        content.forEachIndexed {index,it ->
            canvas.drawText(it.toString(),l  + index * letterSpacing,textY,paint)
        }
    }

    override fun popMenu(labelView: LabelView) {
        val view = View.inflate(labelView.context, R.layout.com_wyc_label_qrcode_item_attr,null)
        showEditDialog(labelView.context,view)

        val group = view.findViewById<Group>(R.id.group)
        val font: MySeekBar = view.findViewById(R.id.font)
        if (cBarcodeFormat == BAROMETER.SongTi){
            font.minValue = minFontSize.toInt()
            font.max = 98 - minFontSize.toInt()
            font.progress = fontSize.toInt() - 30
            font.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    fontSize = progress.toFloat() + minFontSize
                    labelView.postInvalidate()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    seekBar.tag = fontSize
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    val oldSize = seekBar.tag as? Float ?: fontSize
                    if (fontSize != oldSize){
                        addAttrChange(labelView,"fontSize",oldSize,fontSize)
                    }
                }
            })
        }else{
            group.visibility = View.GONE
        }


        val et: EditText = view.findViewById(R.id.content)
        et.setText(content)
        if (field.isNotEmpty()){
            et.isEnabled = false
        }
        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }
            override fun afterTextChanged(s: Editable) {
                content = s.toString()
                generateBitmap()
                labelView.postInvalidate()
            }
        })
        et.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus){
                v.tag = content
            }else{
                val old = v.tag as? String ?: content
                if (old != content){
                    addAttrChange(labelView,"content",old,content)
                }
            }
        }

        view.findViewById<Spinner>(R.id.format)?.apply {
            val adapter = ArrayAdapter<String>(labelView.context, R.layout.com_wyc_label_drop_down_style)
            adapter.setDropDownViewResource(R.layout.com_wyc_label_drop_down_style)
            adapter.add(cBarcodeFormat.description)

            cSupportFormatList.forEach {
                if (it.name == cBarcodeFormat.name)return@forEach
                adapter.add(it.description)
            }
            setAdapter(adapter)

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    cSupportFormatList.forEach {
                        if (it.description == adapter.getItem(position)){
                            cBarcodeFormat = it
                            generateBitmap()
                            labelView.postInvalidate()

                            group.visibility = if (cBarcodeFormat == BAROMETER.SongTi) View.VISIBLE else View.GONE

                            return
                        }
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }
        }
    }
}