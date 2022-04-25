package com.wyc.label

import android.graphics.*
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.*
import com.alibaba.fastjson.annotation.JSONField
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlin.math.min


/**
 *
 * @ProjectName:    AndroidClient
 * @Package:        com.wyc.cloudapp.design
 * @ClassName:      code
 * @Description:    条码
 * @Author:         wyc
 * @CreateDate:     2022/4/11 17:18
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/4/11 17:18
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class BarcodeItem: CodeItemBase() {
    val minFontSize = App.getInstance().resources.getDimension(R.dimen.com_wyc_label_font_size_14)
    var fontSize =  App.getInstance().resources.getDimension(R.dimen.com_wyc_label_font_size_14)
    @JSONField(serialize = false)
    private var mBottomMarge = Rect()
    @JSONField(serialize = false)
    private var leftMargin = 0
    @JSONField(serialize = false)
    private var rightMargin = 0

    init {
        width = 370
        height =  App.getInstance().resources.getDimensionPixelSize(R.dimen.com_wyc_label_size_28)
        generateBitmap()
        BarcodeFormat.values().forEach {
            if (it.name == BarcodeFormat.CODE_128.name || it.name == BarcodeFormat.EAN_13.name){
                supportFormatList.add(it)
            }
        }
    }

    override fun drawItem(offsetX: Float, offsetY: Float, canvas: Canvas, paint: Paint) {
        super.drawItem(offsetX, offsetY, canvas, paint)
        if (mBitmap != null)drawContent(left + offsetX,top + offsetY,canvas,paint)
    }

    override fun transform(scaleX: Float, scaleY: Float) {
        super.transform(scaleX, scaleY)
        fontSize *= min(scaleX,scaleY)
        if (barcodeFormat == BarcodeFormat.EAN_13){
            leftMargin = (leftMargin * min(scaleX,scaleY)).toInt()
            rightMargin = (rightMargin * min(scaleX,scaleY)).toInt()
        }
    }

    override fun scale(scaleX: Float, scaleY: Float) {
        super.scale(scaleX, scaleY)
        if (barcodeFormat == BarcodeFormat.EAN_13){
            generateBitmap()
        }
    }

    private fun drawContent(l: Float, t: Float,canvas: Canvas, paint: Paint){
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        paint.textSize = fontSize

        if (barcodeFormat == BarcodeFormat.EAN_13){
            val start = leftMargin * 2

            val first = content.substring(0,1)
            val end = content.substring(1,content.length)
            paint.getTextBounds(end,0,end.length,mBottomMarge)
            val textHeight = mBottomMarge.height()
            mBottomMarge.bottom += App.getInstance().resources.getDimensionPixelSize(R.dimen.com_wyc_label_size_4)
            mBottomMarge.right += (width- rightMargin - start - mBottomMarge.width())
            mBottomMarge.offsetTo(l.toInt() + start, (height - mBottomMarge.height() + t).toInt())

            canvas.drawRect(mBottomMarge.left.toFloat() - start,
                mBottomMarge.top.toFloat() + (mBottomMarge.height() shr 1), mBottomMarge.right.toFloat() + rightMargin,
                mBottomMarge.bottom.toFloat(),paint)

            canvas.drawRect(mBottomMarge,paint)
            paint.color = Color.BLACK

            var textWidth = 0f
            end.forEach {c->
                textWidth += paint.measureText(c.toString())
            }
            val letterSpacing = ((mBottomMarge.width() - textWidth) / (end.length - 1)) + textWidth / end.length
            val textY = mBottomMarge.bottom - (mBottomMarge.height() - textHeight) / 2f

            canvas.drawText(first,l ,textY,paint)

            end.forEachIndexed {index,it ->
                canvas.drawText(it.toString(),l + start + index * letterSpacing,textY,paint)
            }
        }else{
            paint.getTextBounds(content,0,content.length,mBottomMarge)
            val textHeight = mBottomMarge.height()
            mBottomMarge.bottom += App.getInstance().resources.getDimensionPixelSize(R.dimen.com_wyc_label_size_4)
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
    }

    override fun generateBitmap(){
        if (content.isNotEmpty()){
            val writer = MultiFormatWriter()
            try {
                val result: BitMatrix = writer.encode(content,barcodeFormat, width,height,hashMapOf(Pair(
                    EncodeHintType.MARGIN,18)) )

                var start = 0
                var end = result.width

                for (i in 0 .. result.width){
                    if (result[i,0]){
                        start = i
                        break
                    }
                }
                for (i in result.width downTo 0 ){
                    if (result[i,0]){
                        end = i
                        break
                    }
                }

                if (barcodeFormat == BarcodeFormat.EAN_13){
                    var count = 0
                    var code = true
                    for (i in start .. end){
                        if (result[i,0]){
                            if (code){
                                if (++count == 3){
                                    leftMargin = i- start
                                    break
                                }
                                code = false
                            }
                        }else code = true
                    }

                    count = 0
                    code = true
                    for (i in end downTo  start){
                        if (result[i,0]){
                            if (code){
                                if (++count == 3){
                                    rightMargin = end - i
                                    break
                                }
                                code = false
                            }
                        }else code = true
                    }
                }else {
                    leftMargin = 0
                    rightMargin = 0
                }

                val codeWidth = end - start + leftMargin
                val codeHeight = result.height
                val pixels = IntArray(codeWidth * codeHeight)

                for (y in 0 until codeHeight) {
                    val offset = y * codeWidth
                    for (x in 0 until codeWidth) {
                        if (x < leftMargin){
                            pixels[offset + x] = Color.WHITE
                        }else{
                            val xx = x - leftMargin
                            if (y < result.height && xx + start < result.width){
                                pixels[offset + x] = if (result[xx + start , y]) Color.BLACK else Color.WHITE
                            }else pixels[offset + x] = Color.RED
                        }
                    }
                }
                if(mBitmap != null){
                    if (mBitmap!!.width>= codeWidth && mBitmap!!.height >= codeHeight){
                        mBitmap!!.reconfigure(codeWidth,codeHeight, Bitmap.Config.ARGB_8888)
                        mBitmap?.setPixels(pixels,0, codeWidth, 0, 0, codeWidth, codeHeight)
                    }else{
                        mBitmap!!.recycle()
                        mBitmap = null
                    }
                }
                if (mBitmap == null){
                    mBitmap = Bitmap.createBitmap(codeWidth, codeHeight, Bitmap.Config.ARGB_8888)
                    mBitmap?.setPixels(pixels, 0, codeWidth, 0, 0, codeWidth, codeHeight)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (e is IllegalArgumentException && barcodeFormat == BarcodeFormat.EAN_13){
                    Utils.showToast(R.string.com_wyc_label_ean_13_error_hint)
                }else
                    Utils.showToast(App.getInstance().getString(R.string.com_wyc_label_new_barcode_hint,e.message))

                if(mBitmap != null){
                    mBitmap!!.recycle()
                    mBitmap = null
                }
            }
        }else
            if(mBitmap != null){
                mBitmap!!.recycle()
                mBitmap = null
            }
    }

    override fun popMenu(labelView: LabelView) {
        val view = View.inflate(labelView.context, R.layout.com_wyc_label_barcode_item_attr,null)
        showEditDialog(labelView.context,view)
        val font: MySeekBar = view.findViewById(R.id.font)
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

        val et: EditText = view.findViewById(R.id.content)
        et.inputType = InputType.TYPE_CLASS_NUMBER
        et.setText(content)
        if (field.isNotEmpty()){
            et.isEnabled = false
        }
        if (barcodeFormat == BarcodeFormat.EAN_13){
            et.filters = arrayOf(InputFilter.LengthFilter(13))
        }
        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }
            override fun afterTextChanged(s: Editable) {
                content = s.toString()
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
            adapter.add(barcodeFormat.name)

            supportFormatList.forEach {
                if (it.name == barcodeFormat.name)return@forEach
                adapter.add(it.name)
            }
            setAdapter(adapter)

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    supportFormatList.forEach {
                        if (it.name == adapter.getItem(position)){
                            if (it.name  == BarcodeFormat.EAN_13.name && content.length != 13){
                                Utils.showToast(R.string.com_wyc_label_not_ean_13)
                                setSelection(supportFormatList.indexOf(barcodeFormat))
                                return
                            }else if (it.name == BarcodeFormat.CODE_128.name){
                                et.filters = arrayOf()
                            }
                            addAttrChange(labelView,"barcodeFormat",barcodeFormat,it)
                            barcodeFormat = it
                            labelView.postInvalidate()
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