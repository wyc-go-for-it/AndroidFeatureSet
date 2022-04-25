package com.wyc.label.printer

import com.wyc.label.DataItem
import com.wyc.label.LabelPrintSetting.Companion.getSetting
import com.wyc.label.R
import com.wyc.label.Utils
import com.wyc.label.printer.GPPrinter.Companion.getGPTscCommand
import com.wyc.label.printer.GPPrinter.Companion.sendDataToPrinter
import com.wyc.label.room.AppDatabase.Companion.getInstance


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
        fun print(goods: DataItem.LabelGoods){
            val labelTemplate = getInstance().LabelTemplateDao().getLabelTemplateById(getSetting().labelTemplateId)
            if (labelTemplate != null && labelTemplate.hasItem()){
                sendDataToPrinter(getGPTscCommand(labelTemplate,goods).command)
            }else Utils.showToast(R.string.com_wyc_label_label_empty_hints)
        }
        @JvmStatic
        fun openPrint(){
            GPPrinter.openBlueTooth(getSetting().getPrinterAddress()+"111")
        }
    }
}