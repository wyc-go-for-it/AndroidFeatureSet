package com.wyc.table_recognition.bean

import android.content.Context
import com.wyc.table_recognition.App
import kotlinx.serialization.Serializable

@Serializable
internal data class RecognizingSetting(var barcodeFiled:String = "条码", var nameFiled:String = "商品名称", var numFiled:String = "数量", var priceFiled:String = "单价"){
    var barcodeEnable = true
    var nameEnable = true
    var numEnable = true
    var priceEnable = true

    fun save(){
        val sp = App.getInstance().getSharedPreferences("recognizingSetting", Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putString("setting", JsonUtils.object2String(this))
        editor.apply()
    }

    companion object{
        @JvmStatic
        fun load():RecognizingSetting{
            val sp = App.getInstance().getSharedPreferences("recognizingSetting", Context.MODE_PRIVATE)
            val s = sp.getString("setting","{}")
            return JsonUtils.string2Object(s!!)
        }
    }
}
