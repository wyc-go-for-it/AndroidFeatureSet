package com.wyc.table_recognition.bean


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TableResult(
    @SerialName("log_id")
    var logId: Long = 0,
    @SerialName("table_num")
    var tableNum: Int = 0,

    val error_code:Int = 0,

    val error_msg:String = "",

    @SerialName("tables_result")
    var tablesResult: List<TablesResult> = listOf()
)