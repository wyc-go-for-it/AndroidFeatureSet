package com.wyc.table_recognition.bean

import android.content.Context
import com.wyc.table_recognition.App
import kotlinx.serialization.Serializable

@Serializable
internal data class AccessToken(val access_token:String = "", val expires_in:Int = 0, var start_in:Int = 0){
    fun save(){
        val sp = App.getInstance().getSharedPreferences("AccessToken", Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putString("AccessToken", JsonUtils.object2String(this))
        editor.apply()
    }

    companion object{
        @JvmStatic
        fun load():AccessToken{
            val sp = App.getInstance().getSharedPreferences("AccessToken", Context.MODE_PRIVATE)
            val s = sp.getString("AccessToken","{}")
            return JsonUtils.string2Object(s!!)
        }
    }
}
