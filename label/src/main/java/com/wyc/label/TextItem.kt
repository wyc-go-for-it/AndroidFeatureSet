package com.wyc.label

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.SeekBar
import com.alibaba.fastjson.annotation.JSONField
import kotlin.math.abs
import kotlin.math.min


/**
 *
 * @ProjectName:    AndroidClient
 * @Package:        com.wyc.cloudapp.design
 * @ClassName:      TextItem
 * @Description:    文本
 * @Author:         wyc
 * @CreateDate:     2022/3/16 14:54
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/3/16 14:54
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

open class TextItem: ItemBase() {
    private val minFontSize = App.getInstance().resources.getDimensionPixelSize(R.dimen.com_wyc_label_font_size_10)
    private val maxFontSize = 128

    var content = "编辑内容"
    var mFontSize = App.getInstance().resources.getDimension(R.dimen.com_wyc_label_font_size_18)
    var mFontColor = Color.BLACK
    var mLetterSpacing = 0f
    var hasNewLine = false
    var hasBold = false
    var hasItalic = false
    var hasUnderLine = false
    var hasDelLine = false
    var textAlign = Align.LEFT

    @JSONField(serialize = false)
    private var mPaint:Paint = Paint()
    @JSONField(serialize = false)
    private val mRect = Rect()

    override fun measureItem(w:Int,h:Int) {
        updatePaintAttr()

        getBound(mRect)

        width = min(mRect.width(),w)
        height = min(mRect.height(),h)
    }
    private fun updatePaintAttr(){
        mPaint.textSize = mFontSize
        mPaint.letterSpacing = mLetterSpacing
        mPaint.isFakeBoldText = hasBold
        mPaint.textSkewX = if (hasItalic) -0.5f else 0f
        mPaint.isUnderlineText = hasUnderLine
        mPaint.isStrikeThruText = hasDelLine
    }

    override fun transform(scaleX: Float, scaleY: Float) {
        super.transform(scaleX, scaleY)
        mFontSize *= min(scaleX,scaleY)
    }

    override fun drawItem(offsetX:Float,offsetY:Float,canvas: Canvas,paint: Paint) {
        if (content.isNotEmpty()){
            mPaint = paint

            paint.style = Paint.Style.FILL
            paint.color = mFontColor

            updatePaintAttr()

            canvas.save()

            val l = left + offsetX
            val t = top + offsetY
            canvas.clipRect(l,t,l + width ,t + height)

            if (content.contains("\n")){
                val str = content.split("\n")
                var currentY  = 0f
                str.forEach {
                    paint.getTextBounds(it,0,it.length,mRect)
                    canvas.drawText(it,l,t + mRect.height() + currentY,paint)
                    currentY += mRect.height() + paint.fontMetrics.descent + paint.fontMetrics.leading
                }
            }else{
                val baseLineY = height / 2 + (abs(paint.fontMetrics.ascent) - paint.fontMetrics.descent) / 2
                val left = when(textAlign){
                    Align.MID ->{
                        paint.getTextBounds(content,0,content.length,mRect)
                        (width - mRect.width()) shr 1
                    }
                    Align.RIGHT ->{
                        paint.getTextBounds(content,0,content.length,mRect)
                        width - mRect.width()
                    }else ->{
                        0
                    }
                }
                canvas.drawText(content,l + left,t + baseLineY,paint)
            }

            canvas.restore()
        }
    }

    override fun shrink() {
        scale(0f,-mFontSize * 0.2f)
    }

    override fun zoom() {
        scale(0f,mFontSize * 0.2f)
    }

    override fun scale(scaleX: Float, scaleY: Float) {
        if (abs(scaleY) >= 1.0f){
            mFontSize += scaleY
            if (mFontSize <= maxFontSize) {
                if (mFontSize < minFontSize)mFontSize = minFontSize.toFloat()
            }else mFontSize = maxFontSize.toFloat()
        }else if (abs(scaleX) >= 1.0f){
            width = (width + scaleX).toInt()
        }
        updateNewline()
    }
    fun updateNewline(){
        updatePaintAttr()
        if (hasNewLine){
            if (content.contains("\n")){
                content = content.replace("\n","")
            }
            val textLen = content.length
            mPaint.getTextBounds(content,0,textLen,mRect)
            val newWidth = mRect.width()

            if (newWidth > width){

                mPaint.letterSpacing = 0f
                mPaint.getTextBounds(content,0,textLen,mRect)
                val zeroTotalWidth = mRect.width()
                var zeroPerWidth = 0
                content.forEach {c->
                    mPaint.getTextBounds(c.toString(),0,1,mRect)
                    zeroPerWidth += mRect.width()
                }
                val zeroExtra = (zeroTotalWidth - zeroPerWidth) / textLen.toFloat()

                val space = if(mLetterSpacing > 0f) (newWidth  - zeroTotalWidth) / textLen * 2f else  zeroExtra

                val stringBuilder = StringBuilder(content)
                var len = 0f

                mPaint.letterSpacing = mLetterSpacing
                stringBuilder.forEachIndexed { index, c ->
                    mPaint.getTextBounds(c.toString(),0,1,mRect)
                    len += mRect.width().toFloat() + space
                    if (len > width){
                        if (index > 0 && stringBuilder[index - 1] != '\n')
                            stringBuilder.insert(index,"\n")
                        len = 0f
                    }
                }
                content = stringBuilder.toString()
            }
        }else{
            if (content.contains("\n")){
                content = content.replace("\n","")
            }
        }
        getBound(mRect)
        height = mRect.height()
    }


    private fun getBound(b:Rect){
        if (content.contains("\n")){
            val t = Rect()
            val aStr = content.split("\n")
            var currentY: Float
            var maxWidth = 0
            var maxHeight = 0
            aStr.forEach {
                mPaint.getTextBounds(it,0,it.length,t)
                currentY = t.height() + mPaint.fontMetrics.leading + mPaint.fontMetrics.descent
                maxHeight += currentY.toInt()
                if (t.width() > maxWidth)
                    maxWidth = t.width()
            }
            b.set(0,0,maxWidth,maxHeight)
        }else{
            mPaint.getTextBounds(content,0,content.length,b)
        }
    }

    override fun resetAttr(attrName: String) {
        if (attrName == "content" || attrName == "hasNewLine" || attrName == "mFontSize"){
            updatePaintAttr()
            updateNewline()
        }
    }

    override fun popMenu(labelView: LabelView) {
        val view = View.inflate(labelView.context,R.layout.com_wyc_label_text_item_attr,null)
        showTextEditDialog(labelView,view)
    }
    protected fun showTextEditDialog(labelView: LabelView, view: View){
        showEditDialog(labelView.context,view)
        val font: MySeekBar = view.findViewById(R.id.font)
        font.minValue = minFontSize
        font.max = maxFontSize - minFontSize
        font.progress = mFontSize.toInt() - minFontSize
        font.setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mFontSize = progress.toFloat() + minFontSize

                updateNewline()
                labelView.postInvalidate()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                seekBar.tag = mFontSize
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val oldSize = seekBar.tag as? Float ?: mFontSize
                if (mFontSize != oldSize){
                    addAttrChange(labelView,"mFontSize",oldSize,mFontSize)
                }
            }
        })

        val bCheckBox:CheckBox = view.findViewById(R.id.bold)
        bCheckBox.isChecked = hasBold
        bCheckBox.setOnCheckedChangeListener{ _, check ->
            addAttrChange(labelView,"hasBold",hasBold,check)
            hasBold = check
            labelView.postInvalidate()
        }

        val italic:CheckBox = view.findViewById(R.id.italic)
        italic.isChecked = hasItalic
        italic.setOnCheckedChangeListener { _, isChecked ->
            addAttrChange(labelView,"hasItalic",hasItalic,isChecked)
            hasItalic = isChecked
            updateNewline()
            labelView.postInvalidate()
        }

        val underLine:CheckBox = view.findViewById(R.id.underLine)
        underLine.isChecked = hasUnderLine
        underLine.setOnCheckedChangeListener { _, isChecked ->
            addAttrChange(labelView,"hasUnderLine",hasUnderLine,isChecked)
            hasUnderLine = isChecked
            labelView.postInvalidate()
        }

        val delLine:CheckBox = view.findViewById(R.id.delLine)
        delLine.isChecked = hasDelLine
        delLine.setOnCheckedChangeListener { _, isChecked ->
            addAttrChange(labelView,"hasDelLine",hasDelLine,isChecked)
            hasDelLine = isChecked
            labelView.postInvalidate()
        }

        val newline:CheckBox = view.findViewById(R.id.newline)
        newline.isChecked = hasNewLine
        newline.setOnCheckedChangeListener { _, isChecked ->
            addAttrChange(labelView,"hasNewLine",hasNewLine,isChecked)
            hasNewLine = isChecked
            updateNewline()
            labelView.postInvalidate()
        }

        val left: RadioButton = view.findViewById(R.id.left)
        left.isChecked = textAlign == Align.LEFT
        left.setOnCheckedChangeListener{ _, check ->
            if (check){
                addAttrChange(labelView,"textAlign",textAlign, Align.LEFT)
                textAlign = Align.LEFT
                labelView.postInvalidate()
            }
        }

        val right: RadioButton = view.findViewById(R.id.right)
        right.isChecked = textAlign == Align.RIGHT
        right.setOnCheckedChangeListener{ _, check ->
            if (check){
                addAttrChange(labelView,"textAlign",textAlign, Align.RIGHT)
                textAlign = Align.RIGHT
                labelView.postInvalidate()
            }
        }

        val mid: RadioButton = view.findViewById(R.id.mid)
        mid.isChecked = textAlign == Align.MID
        mid.setOnCheckedChangeListener{ _, check ->
            if (check){
                addAttrChange(labelView,"textAlign",textAlign, Align.MID)
                textAlign = Align.MID
                labelView.postInvalidate()
            }
        }


        val letterSpacing:SeekBar = view.findViewById(R.id.letterSpacing)
        letterSpacing.progress = (mLetterSpacing * 10).toInt()
        letterSpacing.max = 20
        letterSpacing.setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mLetterSpacing = progress / 10f
                updateNewline()
                labelView.postInvalidate()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                seekBar.tag = mLetterSpacing
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val oldSize = seekBar.tag as? Float ?: mLetterSpacing
                if (mLetterSpacing != oldSize){
                    addAttrChange(labelView,"mLetterSpacing",oldSize,mLetterSpacing)
                }
            }

        })

        val et:EditText = view.findViewById(R.id.content)
        et.setText(content)
        if (this is DataItem){
            et.isEnabled = false
        }
        et.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }
            override fun afterTextChanged(s: Editable) {
                content = s.toString()
                updateNewline()
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
    }

    enum class Align{
        LEFT,MID,RIGHT
    }

    override fun toString(): String {
        return "TextItem(content='$content', mFontSize=$mFontSize, mFontColor=$mFontColor, mLetterSpacing=$mLetterSpacing,mPaint=$mPaint, mRect=$mRect) ${super.toString()}"
    }


}