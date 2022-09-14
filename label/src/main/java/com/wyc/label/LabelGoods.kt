package com.wyc.label

import android.os.Parcel
import android.os.Parcelable


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
    var spec:String? = null
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
        only_coding= parcel.readString()
        special_price = parcel.readDouble()
    }

    fun getValueByField(field: String):String{
        when(field){
            DataItem.FIELD.Title.field ->{
                return goodsTitle?:""
            }
            DataItem.FIELD.ProductionPlace.field  ->{
                return origin?:""
            }
            DataItem.FIELD.Unit.field  ->{
                return unit?:""
            }
            DataItem.FIELD.Spec.field  ->{
                return spec?:""
            }
            DataItem.FIELD.Level.field  ->{
                return level?:""
            }
            DataItem.FIELD.Barcode.field  ->{
                return barcode?:""
            }
            DataItem.FIELD.OnlyCoding.field  ->{
                return only_coding?:""
            }
            DataItem.FIELD.VipPrice.field  ->{
                return String.format("%.2f", yh_price)
            }
            DataItem.FIELD.RetailPrice.field  ->{
                return  String.format("%.2f", retail_price)
            }
            DataItem.FIELD.SpecialPrice.field  ->{
                return  String.format("%.2f", special_price)
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
        parcel.writeString(only_coding)
        parcel.writeDouble(special_price)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "LabelGoods(barcodeId=$barcodeId, goodsTitle=$goodsTitle, barcode=$barcode, unit=$unit, spec=$spec, origin=$origin, level=$level, yh_price=$yh_price, retail_price=$retail_price, only_coding=$only_coding, special_price=$special_price)"
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
