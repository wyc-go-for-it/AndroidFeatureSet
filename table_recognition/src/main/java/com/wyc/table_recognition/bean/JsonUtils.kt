package com.wyc.table_recognition.bean

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class JsonUtils {
    companion object{
        @JvmStatic
        val jsonDecoder = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        @JvmStatic
        inline fun <reified T> string2Object( s:String):T{
            return jsonDecoder.decodeFromString(s)
        }

        @JvmStatic
        inline fun <reified T> object2String(s:T):String{
            return Json.encodeToString(s)
        }
    }
}