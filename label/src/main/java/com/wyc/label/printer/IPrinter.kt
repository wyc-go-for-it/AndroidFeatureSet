package com.wyc.label.printer

import com.wyc.label.LabelGoods
import com.wyc.label.LabelTemplate


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.label.printer
 * @ClassName:      IPrinter
 * @Description:    打印机接口
 * @Author:         wyc
 * @CreateDate:     2022/9/14 14:16
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/9/14 14:16
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

interface IPrinter {
    fun open(arg:String)
    fun print(labelTemplate: LabelTemplate, goods: LabelGoods)
    fun close()
    fun setCallback(callback: PrinterStateCallback?)
}