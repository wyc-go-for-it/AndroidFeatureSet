package com.wyc.label.printer

import android.graphics.Color
import android.util.Log
import com.gprinter.command.LabelCommand
import com.wyc.label.*
import com.wyc.label.LabelPrintSetting
import tspl.HPRTPrinterHelper


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.label.printer
 * @ClassName:      HTPrinter
 * @Description:    汉印HT300
 * @Author:         wyc
 * @CreateDate:     2022/9/8 16:13
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/9/8 16:13
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class HTPrinter {
    companion object{
        @JvmStatic
        fun print(labelTemplate: LabelTemplate,goods: LabelGoods?){
            if (goods != null){
                if (!HPRTPrinterHelper.IsOpened()){
                    Utils.showToast(R.string.com_wyc_label_not_connect)
                    return
                }
                val setting = LabelPrintSetting.getSetting()
                val width = (labelTemplate.width - setting.offsetX).toString()
                val height = (labelTemplate.height - setting.offsetY).toString()

                HPRTPrinterHelper.Density(setting.density.toString())
                HPRTPrinterHelper.printAreaSize(width,height)
                HPRTPrinterHelper.CLS()
                val data = labelTemplate.printSingleGoods(goods)
                for (it in data) {
                    val b = it.createItemBitmap(Color.WHITE)
                    val code = HPRTPrinterHelper.printImage(it.left.toString(),it.top.toString(),b,true,false,0)
                    Log.e("HTPrinter","code:$code")
                }
                HPRTPrinterHelper.Print("1","1")
            }
        }
        @JvmStatic
        fun open(address:String):Int{
           return HPRTPrinterHelper.PortOpen(address)
        }
        @JvmStatic
        fun close(){
            HPRTPrinterHelper.PortClose()
        }
    }
}