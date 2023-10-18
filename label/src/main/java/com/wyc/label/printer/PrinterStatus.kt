package com.wyc.label.printer

enum class PrinterStatus(val code:Int,val state:String){
    PRINTED(0,"打印完成"),
    PRINT_ERROR(0,"打印错误"),
    CONNECTING(0,"连接中"),
    CONNECTED(0,"已连接"),
    DISCONNECT(0,"断开连接"),
    TIMEOUT(1,"请求超时"),
    COVER_OPENED(1,"开盖"),
    NO_PAPER(1,"缺纸"),
    OVER_HEATING(1,"过热"),
    PRINTING(1,"正在打印"),
    OK(1,"正常"),
}