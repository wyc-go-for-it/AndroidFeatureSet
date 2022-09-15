package com.wyc.label

import android.graphics.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.FormatException
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.ObjectStreamException

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

internal open class CodeItemBase: ItemBase(){
    var cBarcodeFormat: BAROMETER = BAROMETER.AUTO
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

    @Transient protected var mBitmap:Bitmap? = null
    var hasMark = true
    @Transient protected var cSupportFormatList = mutableListOf<BAROMETER>()

    @Throws(ObjectStreamException::class)
    private fun readResolve(): Any {
        serializableInit()
        return this
    }

    companion object {
        const val serialVersionUID = 1L
    }

    override fun serializableInit() {
        super.serializableInit()
        if (cBarcodeFormat == null){
            cBarcodeFormat = BAROMETER.AUTO
        }
        generateBitmap()
    }

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
        if (attrName == "content" || attrName == "cBarcodeFormat"){
            generateBitmap()
        }
    }

    protected fun getDrawBarcodeFormat():BarcodeFormat{
        return when(cBarcodeFormat){
            BAROMETER.AUTO->{
                if (content.length == 13 && checkStandardUPCEANChecksum(content)){
                    BarcodeFormat.EAN_13
                }else BarcodeFormat.CODE_128
            }
            BAROMETER.EAN13->{
                BarcodeFormat.EAN_13
            }
            BAROMETER.CODE128->{
                BarcodeFormat.CODE_128
            }
            BAROMETER.QRCODE->{
                BarcodeFormat.QR_CODE
            }
        }
    }

    protected open fun generateBitmap(){
        if (content.isNotEmpty()){
            val writer = MultiFormatWriter()
            try {
                val result: BitMatrix = writer.encode(content,getDrawBarcodeFormat(), width,height,hashMapOf(Pair(EncodeHintType.MARGIN,0)) )

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
                Utils.showToast(e.message)
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

    protected fun checkStandardUPCEANChecksum(s: CharSequence): Boolean {
        val length = s.length
        if (length == 0) {
            return false
        }
        var sum = 0
        run {
            var i = length - 2
            while (i >= 0) {
                val digit = s[i] - '0'
                if (digit < 0 || digit > 9) {
                    return false
                }
                sum += digit
                i -= 2
            }
        }
        sum *= 3
        var i = length - 1
        while (i >= 0) {
            val digit = s[i] - '0'
            if (digit < 0 || digit > 9) {
                return false
            }
            sum += digit
            i -= 2
        }
        return sum % 10 == 0
    }

    enum class BAROMETER(s:String){
        AUTO("自动设定"),CODE128("CODE_128"),EAN13("EAN_13"),QRCODE("QR_CODE");
        val description = s
    }
}