package com.wyc.label.printer

import android.bluetooth.BluetoothAdapter
import android.util.Log
import com.gprinter.utils.CallbackListener
import com.wyc.label.*
import com.wyc.label.LabelPrintSetting.Companion.getSetting
import com.wyc.label.printer.GPPrinter.Companion.getGPTscCommand
import com.wyc.label.printer.GPPrinter.Companion.sendDataToPrinter
import com.wyc.label.room.BluetoothUtils
import tspl.HPRTPrinterHelper


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
        private fun hasSupportBluetooth(): Boolean {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val code = bluetoothAdapter != null && bluetoothAdapter.isEnabled
            if (!code) {
                Utils.showToast("未开启蓝牙功能...")
            }
            return code
        }

        @JvmStatic
        fun print(goods: LabelGoods){
            val labelTemplate = getLabelTemplate()
            if (labelTemplate != null && labelTemplate.hasItem()){
                val setting = getSetting()
                when(setting.way){
                    LabelPrintSetting.Way.BLUETOOTH_PRINT->{
                        sendDataToPrinter(getGPTscCommand(labelTemplate,goods).command)
                    }
                    LabelPrintSetting.Way.WIFI_PRINT->{
                        HTPrinter.print(labelTemplate,goods)
                    }
                }
            }else Utils.showToast(R.string.com_wyc_label_label_empty_hints)
        }
        @JvmStatic
        fun openPrinter(){
            val setting = getSetting()
            when(setting.way){
                LabelPrintSetting.Way.BLUETOOTH_PRINT->{
                    if (BluetoothUtils.hasSupportBluetooth())
                        GPPrinter.openBlueTooth(getSetting().getPrinterAddress())
                }
                LabelPrintSetting.Way.WIFI_PRINT->{
                    val printer = setting.printer.split("@")
                    if (printer.size > 1){
                        val open = String.format("WiFi,%s,%s",printer[0],printer[1])
                        val code = HTPrinter.open(open)
                        Log.e("connPrinter", "code:$code,address:$open")
                    }else{
                        Utils.showToast(R.string.com_wyc_label_no_printer_hint)
                    }
                }
            }
        }
        @JvmStatic
        fun openPrinter(callbackListener: CallbackListener){
            if (BluetoothUtils.hasSupportBluetooth())
                GPPrinter.openBlueTooth(getSetting().getPrinterAddress(),callbackListener)
        }
        @JvmStatic
        fun closePrinter(){
            val setting = getSetting()
            when(setting.way){
                LabelPrintSetting.Way.BLUETOOTH_PRINT->{
                    GPPrinter.close()
                }
                LabelPrintSetting.Way.WIFI_PRINT->{
                    HTPrinter.close()
                }
            }
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