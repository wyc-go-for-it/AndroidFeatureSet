package com.wyc.table_recognition.bean


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class Content(
    @SerialName("poly_location")
    var polyLocation: List<PolyLocation> = listOf(),
    @SerialName("word")
    var word: String = ""
)