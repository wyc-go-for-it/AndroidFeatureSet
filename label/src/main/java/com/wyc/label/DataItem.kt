package com.wyc.label

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Parcel
import android.os.Parcelable
import com.alibaba.fastjson.annotation.JSONField


/**
 *
 * @ProjectName:    AndroidClient
 * @Package:        com.wyc.cloudapp.design
 * @ClassName:      DataItem
 * @Description:    数据列
 * @Author:         wyc
 * @CreateDate:     2022/3/28 17:32
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/3/28 17:32
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class DataItem: TextItem() {
    var field = ""
    @JSONField(serialize = false)
    var hasMark = true
    override fun drawItem(offsetX: Float, offsetY: Float, canvas: Canvas, paint: Paint) {
        super.drawItem(offsetX, offsetY, canvas, paint)
        if (hasMark){
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

    companion object {
        @JvmStatic
        fun testGoods(): LabelGoods {
            val goods = LabelGoods()
            goods.barcode = "6922711043401"
            goods.goodsTitle = "test内容"
            goods.level = "高级"
            goods.origin = "长沙"
            goods.spec = "箱"
            goods.unit = "件"
            goods.retail_price = 18.68
            goods.yh_price = 12.08
            return goods
        }
    }

    override fun toString(): String {
        return "DataItem(field='$field', hasMark=$hasMark) ${super.toString()}"
    }

    class LabelGoods() :Parcelable{
        var barcodeId:String? = null
        var goodsTitle:String? = null
        var barcode:String? = null
        var unit:String? = null
        var spec:String? = null
            get() {
                if (field.isNullOrEmpty())return "无"
                return field
            }
        var origin:String? = null
        var level:String? = null
        var yh_price:Double = 0.0
        var retail_price:Double = 0.0

        constructor(parcel: Parcel) : this() {
            barcodeId = parcel.readString()
            goodsTitle = parcel.readString()
            barcode = parcel.readString()
            unit = parcel.readString()
            spec = parcel.readString()
            origin = parcel.readString()
            level = parcel.readString()
            yh_price = parcel.readDouble()
            retail_price = parcel.readDouble()
        }

        fun getValueByField(field: String):String{
            when(field){
                FIELD.Title.field ->{
                    return goodsTitle?:""
                }
                FIELD.ProductionPlace.field  ->{
                    return origin?:""
                }
                FIELD.Unit.field  ->{
                    return unit?:""
                }
                FIELD.Spec.field  ->{
                    return spec?:""
                }
                FIELD.Level.field  ->{
                    return level?:""
                }
                FIELD.Barcode.field  ->{
                    return barcode?:""
                }
                FIELD.VipPrice.field  ->{
                    return String.format("%.2f", yh_price)
                }
                FIELD.RetailPrice.field  ->{
                    return  String.format("%.2f", retail_price)
                }
            }
            return ""
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as LabelGoods

            if (barcodeId != other.barcodeId) return false

            return true
        }

        override fun hashCode(): Int {
            return barcodeId?.hashCode() ?: 0
        }



        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(barcodeId)
            parcel.writeString(goodsTitle)
            parcel.writeString(barcode)
            parcel.writeString(unit)
            parcel.writeString(spec)
            parcel.writeString(origin)
            parcel.writeString(level)
            parcel.writeDouble(yh_price)
            parcel.writeDouble(retail_price)
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun toString(): String {
            return "LabelGoods(barcodeId=$barcodeId, goodsTitle=$goodsTitle, barcode=$barcode, unit=$unit, spec=$spec, origin=$origin, level=$level, yh_price=$yh_price, retail_price=$retail_price)"
        }

        companion object CREATOR : Parcelable.Creator<LabelGoods> {
            override fun createFromParcel(parcel: Parcel): LabelGoods {
                return LabelGoods(parcel)
            }

            override fun newArray(size: Int): Array<LabelGoods?> {
                return arrayOfNulls(size)
            }
        }
    }

    enum class FIELD(f:String,n:String){
        Title("goodsTitle","商品名称"),ProductionPlace("origin","产地"),Unit("unit","单位"),
        Spec("spec_str","规格"),Level("level","等级"),Barcode("barcode","条码"),VipPrice("yh_price","会员价"),
        RetailPrice("retail_price","零售价");
        val field = f
        val description = n
    }
}