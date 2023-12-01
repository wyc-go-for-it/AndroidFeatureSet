package com.wyc.table_recognition

import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal class HttpUtils {
    companion object{
        @JvmStatic
        private val httpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED){
            OkHttpClient()
        }

        @JvmStatic
        fun sendAsyncPost(url: String, param: String): Call {
            return httpClient.newCall(createRequestForForm(url, param))
        }

/*        @JvmStatic
        fun <T> sendSyncPost(url: String, param: String,serializer: KSerializer<T>): T? {
            httpClient.newCall(createRequestForForm(url, param)).execute().use {
                return it.body?.let { body ->
                    SerializerCallback.jsonDecoder.decodeFromString(serializer,
                        body.string())
                }
            }
        }*/

        @JvmStatic
        private fun createRequestForForm(url: String, param: String): Request {
            val body = param.toRequestBody("application/x-www-form-urlencoded; charset=utf-8".toMediaTypeOrNull())
            return Request.Builder().header("Accept-Encoding", "identity").url(url).post(body).build()
        }
    }
}