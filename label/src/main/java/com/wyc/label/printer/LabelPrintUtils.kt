package com.wyc.label.printer

import com.gprinter.utils.CallbackListener
import com.wyc.label.*
import com.wyc.label.LabelPrintSetting.Companion.getSetting
import com.wyc.label.printer.GPPrinter.Companion.getGPTscCommand
import com.wyc.label.printer.GPPrinter.Companion.sendDataToPrinter
import com.wyc.label.room.BluetoothUtils


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.label.printer
 * @ClassName:      LabelPrintUtils
 * @Description:    标签打印工具
 * @Author:         wyc
 * @CreateDate:     2022/4/25 11:44
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/4/25 11:44
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class LabelPrintUtils {
    companion object{
        @JvmStatic
        fun print(goods: LabelGoods){
            val labelTemplate = getLabelTemplate()
            if (labelTemplate != null && labelTemplate.hasItem()){
                sendDataToPrinter(getGPTscCommand(labelTemplate,goods).command)
            }else Utils.showToast(R.string.com_wyc_label_label_empty_hints)
        }
        @JvmStatic
        fun openPrinter(){
            if (BluetoothUtils.hasSupportBluetooth())
                GPPrinter.openBlueTooth(getSetting().getPrinterAddress())
        }
        @JvmStatic
        fun openPrinter(callbackListener: CallbackListener){
            if (BluetoothUtils.hasSupportBluetooth())
                GPPrinter.openBlueTooth(getSetting().getPrinterAddress(),callbackListener)
        }
        @JvmStatic
        fun closePrinter(){
            GPPrinter.close()
        }
        @JvmStatic
        fun hasConn(){
            GPPrinter.getInstance()
        }
        @JvmStatic
        fun getLabelTemplate(): LabelTemplate? {
            return LabelTemplate.getLabelById(getSetting().labelTemplateId)
        }
    }
}