package com.wyc.label

import android.os.Parcel
import android.os.Parcelable
import java.util.Locale


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.label
 * @ClassName:      LabelContent
 * @Description:    作用描述
 * @Author:         wyc
 * @CreateDate:     2022/4/27 16:46
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/4/27 16:46
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class LabelGoods(): Parcelable {
    var barcodeId:String? = null
    var goodsTitle:String? = null
    var barcode:String? = null
    var unit:String? = null
    var spec_str:String? = null
        get() {
            if (field.isNullOrEmpty())return "无"
            return field
        }
    var origin:String? = null
    var level:String? = null
    var yh_price:Double = 0.0
    var retail_price:Double = 0.0
    var only_coding:String? = null
    var special_price:Double = 0.0
    var specifi_attr:String? = null
        get() {
            if (field.isNullOrEmpty())return ""
            return field
        }

    var discount_type:String? = null
        get() {
            if (field.isNullOrEmpty())return "8折"
            return field
        }

    var discount_barcode:String? = null
        get() {
            if (field.isNullOrEmpty())return "82080000185000680001212"
            return field
        }

    var xnum:Double = 0.0

    var amt:Double = 0.0

    constructor(parcel: Parcel) : this() {
        barcodeId = parcel.readString()
        goodsTitle = parcel.readString()
        barcode = parcel.readString()
        unit = parcel.readString()
        spec_str = parcel.readString()
        origin = parcel.readString()
        level = parcel.readString()
        yh_price = parcel.readDouble()
        retail_price = parcel.readDouble()
        only_coding= parcel.readString()
        special_price = parcel.readDouble()
        specifi_attr = parcel.readString()
        discount_type = parcel.readString()
        discount_barcode = parcel.readString()
        xnum = parcel.readDouble()
        amt = parcel.readDouble()
    }

    fun getValueByField(field: String):String{
        val clazz = this.javaClass
        try {
            val field = clazz.getDeclaredField(field).apply { isAccessible = true }
            val value = field.get(this)
            val type = field.type

            return if (type == Double::class.java  || type == Double::class.javaPrimitiveType){
                String.format(Locale.CHINA,"%.2f", value as Double)
            }else{
                value?.toString() ?: ""
            }
        }catch (_:NoSuchFieldException){
        }

        return ""
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LabelGoods

        return barcodeId == other.barcodeId
    }

    override fun hashCode(): Int {
        return barcodeId?.hashCode() ?: 0
    }


    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(barcodeId)
        parcel.writeString(goodsTitle)
        parcel.writeString(barcode)
        parcel.writeString(unit)
        parcel.writeString(spec_str)
        parcel.writeString(origin)
        parcel.writeString(level)
        parcel.writeDouble(yh_price)
        parcel.writeDouble(retail_price)
        parcel.writeString(only_coding)
        parcel.writeDouble(special_price)
        parcel.writeString(specifi_attr)
        parcel.writeString(discount_type)
        parcel.writeString(discount_barcode)
        parcel.writeDouble(xnum)
        parcel.writeDouble(amt)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "LabelGoods(barcodeId=$barcodeId, goodsTitle=$goodsTitle, barcode=$barcode, unit=$unit, spec=$spec_str, origin=$origin, level=$level, yh_price=$yh_price, retail_price=$retail_price, only_coding=$only_coding, special_price=$special_price, specifi_attr=$specifi_attr, discount_type=$discount_type, discount_barcode=$discount_barcode, xnum=$xnum, amt=$amt)"
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
