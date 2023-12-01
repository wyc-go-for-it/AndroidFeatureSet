package com.wyc.table_recognition.bean


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CellLocation(
    @SerialName("x")
    var x: Int = 0,
    @SerialName("y")
    var y: Int = 0
)