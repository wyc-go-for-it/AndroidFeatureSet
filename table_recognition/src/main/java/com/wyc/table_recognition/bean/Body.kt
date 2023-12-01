package com.wyc.table_recognition.bean


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class Body(
    @SerialName("cell_location")
    var cellLocation: List<CellLocation> = listOf(),
    @SerialName("col_end")
    var colEnd: Int = 0,
    @SerialName("col_start")
    var colStart: Int = 0,
    @SerialName("contents")
    var contents: List<Content> = listOf(),
    @SerialName("row_end")
    var rowEnd: Int = 0,
    @SerialName("row_start")
    var rowStart: Int = 0,
    @SerialName("words")
    var words: String = ""
)