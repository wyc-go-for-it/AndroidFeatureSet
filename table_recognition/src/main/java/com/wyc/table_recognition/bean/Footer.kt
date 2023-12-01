package com.wyc.table_recognition.bean

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class Footer(@SerialName("location")
                    var location: List<Location> = listOf(),
                  @SerialName("words")
                    var words: String = "")
