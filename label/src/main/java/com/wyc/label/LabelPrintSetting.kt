package com.wyc.label

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.annotation.JSONField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

/**
 *
 * @ProjectName:    AndroidClient
 * @Package:        com.wyc.cloudapp.bean
 * @ClassName:      LabelPrintSetting
 * @Description:    标签打印设置
 * @Author:         wyc
 * @CreateDate:     2022/3/25 16:53
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/3/25 16:53
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class LabelPrintSetting {
    @JSONField(serialize = false)
    private var change = false
    enum class Way(s: String)  {
        BLUETOOTH_PRINT(App.getInstance().getString(R.string.com_wyc_label_bluetooth_way));
        val description:String = s
    }
    var way: Way by change(Way.BLUETOOTH_PRINT)

    enum class Rotate(degree:Int){
        D_0(0),D_180(180);
        val description:String = degree.toString()
        val value = degree
    }
    /**
     * 打印偏移 单位mm
     * */
    var offsetX = 0
    var offsetY = 0

    var dpi = 203

    var rotate: Rotate by change(Rotate.D_0)

    var labelTemplateId: Int by change(0)
    var labelTemplateName:String by change("")

    var printNum by change(1)

    var printer:String by change("")
    fun getPrinterAddress():String{
        if (printer.contains("@")){
            return printer.split("@")[1]
        }
        return ""
    }

    companion object{
        const val P_KEY = "label_print"
        const val C_KEY = "c"

        @JvmStatic
        fun combinationPrinter(a:String,n:String):String{
            return String.format("%s@%s",n,a)
        }
        @JvmStatic
        fun getSetting(): LabelPrintSetting {
            val preferences: SharedPreferences = App.getInstance().getSharedPreferences(P_KEY, Context.MODE_PRIVATE)
            val setting = JSONObject.parseObject(preferences.getString(C_KEY,"{}"), LabelPrintSetting::class.java)?: LabelPrintSetting()
            setting.change = false
            return setting
        }
    }

    fun saveSetting(){
        CoroutineScope(Dispatchers.IO).launch {
            val preferences: SharedPreferences = App.getInstance().getSharedPreferences(P_KEY, Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.putString(C_KEY,JSONObject.toJSONString(this@LabelPrintSetting))
            editor.apply()

            withContext(Dispatchers.Main){
                Toast.makeText(App.getInstance(),R.string.com_wyc_label_success,Toast.LENGTH_LONG).show()
            }
        }
    }

    fun hasChange():Boolean{
        return change
    }

    private fun <T> change(iv:T) = Delegates.observable(iv) { _, oldValue, newValue ->
        if (oldValue != newValue) change = true
    }

    override fun toString(): String {
        return "LabelPrintSetting(way=$way, rotate=$rotate, labelTemplateId=$labelTemplateId, labelTemplateName='$labelTemplateName', printNum=$printNum, printer='$printer')"
    }


}