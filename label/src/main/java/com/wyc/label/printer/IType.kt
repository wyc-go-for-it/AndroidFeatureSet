package com.wyc.label.printer

import java.io.Serializable


interface IType : Serializable {
    fun getEnumName():String
    fun description():String
    fun cls():String
    fun getDeviceType():Int
}