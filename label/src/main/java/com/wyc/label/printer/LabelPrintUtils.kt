package com.wyc.label.printer

import android.content.Context
import android.content.Intent
import com.wyc.label.*
import com.wyc.label.LabelPrintSetting.Companion.getSetting
import java.lang.reflect.InvocationTargetException


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
        private var sPrinter: IPrinter? = null

        @JvmStatic
        fun getInstance(): IPrinter? {
            if (sPrinter == null){
                val cls_id = getSetting().type.cls()
                try {
                    if (cls_id == null){
                        Utils.showToast(LabelApp.getInstance().getString(R.string.com_wyc_label_not_support_printer,"null"))
                        return null
                    }
                    synchronized(LabelPrintUtils::class){
                        if (sPrinter == null){
                            val printerClass =
                            if (cls_id.startsWith("com.wyc")){
                                Class.forName(cls_id)
                            }else Class.forName("com.wyc.label.printer.$cls_id")
                            sPrinter = printerClass.getConstructor().newInstance() as IPrinter
                        }
                    }
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                    Utils.showToast(LabelApp.getInstance().getString(R.string.com_wyc_label_not_support_printer,cls_id))
                } catch (e: NoSuchMethodException) {
                    e.printStackTrace()
                    Utils.showToast(e.message)
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                    Utils.showToast(e.message)
                } catch (e: InstantiationException) {
                    e.printStackTrace()
                    Utils.showToast(e.message)
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                    Utils.showToast(e.message)
                }
            }
            return sPrinter
        }

        @JvmStatic
        fun print(goods: LabelGoods){
            val labelTemplate = getLabelTemplate()
            if (labelTemplate != null && labelTemplate.hasItem()){
                getInstance()?.print(labelTemplate,goods)
            }else Utils.showToast(R.string.com_wyc_label_label_empty_hints)
        }
        @JvmStatic
        fun openPrinter(callback: PrinterStateCallback? = null){
            getInstance()?.apply {
                setCallback(callback)
                val setting = getSetting()
                when(setting.way){
                    LabelPrintSetting.Way.BLUETOOTH_PRINT->{
                        open(setting.getPrinterAddress())
                    }
                    LabelPrintSetting.Way.WIFI_PRINT->{
                        val printer = setting.printer.split("@")
                        if (printer.size > 1){
                             open(setting.printer)
                        }else{
                            Utils.showToast(R.string.com_wyc_label_no_printer_hint)
                        }
                    }
                }
            }
        }

        @JvmStatic
        fun closePrinter(){
            synchronized(LabelPrintUtils::class) {
                if (sPrinter != null) {
                    sPrinter!!.close()
                    sPrinter = null
                }
            }
        }

        @JvmStatic
        fun getLabelTemplate(): LabelTemplate? {
            return LabelTemplate.getLabelById(getSetting().labelTemplateId)
        }

        @JvmStatic
        fun wifiPrint():Boolean{
            return getSetting().way == LabelPrintSetting.Way.WIFI_PRINT
        }
        @JvmStatic
        fun bluetoothPrint():Boolean{
            return getSetting().way == LabelPrintSetting.Way.BLUETOOTH_PRINT
        }

        @JvmStatic
        fun startSetting(c:Context){
            c.startActivity(Intent(c, LabelPrintSettingActivity::class.java))
        }

        @JvmStatic
        fun startDesign(c:Context,templateId:Int = -1){
            val intent = Intent(c, LabelDesignActivity::class.java)
            if (templateId != -1)intent.putExtra(BrowseLabelActivity.LABEL_KEY,templateId)
            c.startActivity(intent)
        }
    }
}