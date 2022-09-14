package com.wyc.label.printer

import com.gprinter.bean.PrinterDevices


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.label.printer
 * @ClassName:      PrinterStateCallback
 * @Description:    打印机状态回调
 * @Author:         wyc
 * @CreateDate:     2022/9/14 14:57
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/9/14 14:57
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

interface PrinterStateCallback {
    fun onConnecting()

    fun onSuccess(printer:IPrinter)

    fun onReceive()

    fun onFailure()

    fun onDisconnect()
}