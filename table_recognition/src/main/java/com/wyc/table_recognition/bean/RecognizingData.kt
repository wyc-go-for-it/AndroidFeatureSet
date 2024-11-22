package com.wyc.table_recognition.bean

import android.os.Parcel
import android.os.Parcelable

class RecognizingData() :Parcelable{
    var barcode:String = ""
    var name:String = ""
    var num:Double = 0.0
    var price:Double = 0.0

    var spec = ""
    var unit = ""
    var buyingPrice = 0.0
    var sellPrice = 0.0

    var barcodeEnable = true
    var nameEnable = true
    var numEnable = true
    var priceEnable = true

    var specEnable = true
    var unitEnable = true
    var buyingPriceEnable = true
    var sellPriceEnable = true

    constructor(parcel: Parcel) : this() {
        barcode = parcel.readString().toString()
        name = parcel.readString().toString()
        num = parcel.readDouble()
        price = parcel.readDouble()
        spec = parcel.readString().toString()
        unit = parcel.readString().toString()
        buyingPrice = parcel.readDouble()
        sellPrice = parcel.readDouble()
        barcodeEnable = parcel.readByte() != 0.toByte()
        nameEnable = parcel.readByte() != 0.toByte()
        numEnable = parcel.readByte() != 0.toByte()
        priceEnable = parcel.readByte() != 0.toByte()
        specEnable = parcel.readByte() != 0.toByte()
        unitEnable = parcel.readByte() != 0.toByte()
        buyingPriceEnable = parcel.readByte() != 0.toByte()
        sellPriceEnable = parcel.readByte() != 0.toByte()
    }



    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(barcode)
        parcel.writeString(name)
        parcel.writeDouble(num)
        parcel.writeDouble(price)
        parcel.writeString(spec)
        parcel.writeString(unit)
        parcel.writeDouble(buyingPrice)
        parcel.writeDouble(sellPrice)
        parcel.writeByte(if (barcodeEnable) 1 else 0)
        parcel.writeByte(if (nameEnable) 1 else 0)
        parcel.writeByte(if (numEnable) 1 else 0)
        parcel.writeByte(if (priceEnable) 1 else 0)
        parcel.writeByte(if (specEnable) 1 else 0)
        parcel.writeByte(if (unitEnable) 1 else 0)
        parcel.writeByte(if (buyingPriceEnable) 1 else 0)
        parcel.writeByte(if (sellPriceEnable) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "RecognizingData(barcode='$barcode', name='$name', num=$num, price=$price, spec='$spec', unit='$unit', buyingPrice=$buyingPrice, sellPrice=$sellPrice, barcodeEnable=$barcodeEnable, nameEnable=$nameEnable, numEnable=$numEnable, priceEnable=$priceEnable, specnable=$specEnable, unitEnable=$unitEnable, buyingPriceEnable=$buyingPriceEnable, sellPriceEnable=$sellPriceEnable)"
    }

    companion object CREATOR : Parcelable.Creator<RecognizingData> {
        override fun createFromParcel(parcel: Parcel): RecognizingData {
            return RecognizingData(parcel)
        }

        override fun newArray(size: Int): Array<RecognizingData?> {
            return arrayOfNulls(size)
        }
    }

}
