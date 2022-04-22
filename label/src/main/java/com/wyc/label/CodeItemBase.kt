package com.wyc.label

import android.graphics.*
import android.widget.Toast
import com.alibaba.fastjson.annotation.JSONField
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

/**
 *
 * @ProjectName:    AndroidClient
 * @Package:        com.wyc.cloudapp.design
 * @ClassName:      ImageItem
 * @Description:    图片
 * @Author:         wyc
 * @CreateDate:     2022/3/16 14:51
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/3/16 14:51
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

open class CodeItemBase: ItemBase() {
    var barcodeFormat: BarcodeFormat = BarcodeFormat.CODE_128
        set(value) {
            field = value
            generateBitmap()
        }
    var content: String = "6922711043401"
        set(value) {
            field = value
            generateBitmap()
        }
        get() {
            if (field == null)return ""
            return field
        }

    /**
     * 引用数据字段。如果不为空则content的值需要从数据源获取，获取数据源的具体值由field的值决定
     * */
    var field = ""

    @JSONField(serialize = false)
    protected var mBitmap:Bitmap? = null
    @JSONField(serialize = false)
    var hasMark = true
    @JSONField(serialize = false)
    protected val supportFormatList = mutableListOf<BarcodeFormat>()

    override fun drawItem(offsetX: Float, offsetY: Float, canvas: Canvas, paint: Paint) {
        mBitmap?.let {
            val l = left + offsetX
            val t = top + offsetY

            canvas.drawBitmap(it, null,RectF(l,t,l + width,t + height),paint)
            if (hasMark && field.isNotEmpty()){
                val r = radian % 360 != 0f
                if (r){
                    canvas.save()
                    canvas.rotate(-radian,offsetX+ left + width / 2f,offsetY + top + height / 2f)
                }

                paint.color = Color.RED
                paint.style = Paint.Style.STROKE
                canvas.drawRect(cRECT,paint)

                if (r){
                    canvas.restore()
                }
            }
        }
    }

    override fun clone(): ItemBase {
        val c =  super.clone()
        if (mBitmap != null){
            mBitmap = mBitmap!!.copy(mBitmap!!.config,true)
        }
        return c
    }

    override fun resetAttr(attrName: String) {
        if (attrName == "content" || attrName == "barcodeFormat"){
            generateBitmap()
        }
    }

    protected open fun generateBitmap(){
        if (content.isNotEmpty()){
            val writer = MultiFormatWriter()
            try {
                val result: BitMatrix = writer.encode(content,barcodeFormat, width,height,hashMapOf(Pair(EncodeHintType.MARGIN,0)) )

                val codeWidth = result.width
                val codeHeight = result.height
                val pixels = IntArray(codeWidth * codeHeight)

                for (y in 0 until codeHeight) {
                    val offset = y * codeWidth
                    for (x in 0 until codeWidth) {
                        if (y < result.height ){
                            pixels[offset + x] = if (result[x , y]) Color.BLACK else Color.WHITE
                        }else pixels[offset + x] = Color.RED
                    }
                }
                if(mBitmap != null){
                    if (mBitmap!!.width>= codeWidth && mBitmap!!.height >= codeHeight){
                        mBitmap!!.reconfigure(codeWidth,codeHeight,Bitmap.Config.ARGB_8888)
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
                Toast.makeText(App.getInstance(),e.message,Toast.LENGTH_LONG).show()
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
}