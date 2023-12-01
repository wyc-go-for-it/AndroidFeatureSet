package com.wyc.table_recognition.bean


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TablesResult(
    @SerialName("body")
    var body: List<Body> = listOf(),
    @SerialName("footer")
    var footer: List<Footer> = listOf(),
    @SerialName("header")
    var header: List<Header> = listOf(),
    @SerialName("table_location")
    var tableLocation: List<TableLocation> = listOf()
)