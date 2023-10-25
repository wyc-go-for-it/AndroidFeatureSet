package com.wyc.label.printer

import android.graphics.Color
import android.util.Log
import com.wyc.label.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

internal class HTPrinter: AbstractPrinter() {
    @Volatile
    private var state = PRINTER_STATE.CLOSE
    private var isExit = false

    override fun open(arg: String) {
        when(state){
            PRINTER_STATE.OPEN->{
                if (mCallback != null){
                    mCallback!!.onSuccess(this@HTPrinter)
                }else
                    Utils.showToast(R.string.com_wyc_label_conn_success)

                Log.w(this.javaClass.simpleName,"printer has opened")
            }
            PRINTER_STATE.CLOSE,PRINTER_STATE.FAILURE->{
                isExit = false

                if (mCallback != null){
                    mCallback!!.onConnecting()
                }else
                    Utils.showToast(R.string.com_wyc_label_printer_connecting)

                CoroutineScope(Dispatchers.IO).launch{
                    val printer = arg.split("@")
                    val code = HPRTPrinterHelper.PortOpen(String.format("WiFi,%s,%s",printer[0],printer[1]))
                    if (code < 0){
                        state = PRINTER_STATE.FAILURE
                        if (mCallback != null){
                            mCallback!!.onFailure()
                        }else
                            Utils.showToast(R.string.com_wyc_label_conn_fail)
                    }else{
                        if (isExit){//正在打开的过程中调用了close,打开返回之后再判断状态
                            close()
                        }else {
                            state = PRINTER_STATE.OPEN
                            if (mCallback != null){
                                mCallback!!.onSuccess(this@HTPrinter)
                            }else
                                Utils.showToast(R.string.com_wyc_label_conn_success)
                        }
                    }
                    Log.e("connPrinter", "code:$code,address:$arg")
                }
            }
        }
    }

    override fun print(labelTemplate: LabelTemplate, goods: LabelGoods) {
        if (!HPRTPrinterHelper.IsOpened()){
            Utils.showToast(R.string.com_wyc_label_not_connect)
            return
        }
        var code: Int

        val setting = LabelPrintSetting.getSetting()
        val width = labelTemplate.width.toString()
        val height = labelTemplate.height.toString()

        code = HPRTPrinterHelper.printAreaSize(width,height)
        printerStatus("printAreaSize",code)

        HPRTPrinterHelper.CLS()

        HPRTPrinterHelper.Density(setting.density.toString())


        val offX = LabelTemplate.mm2Pixel(setting.offsetX.toFloat())
        val offY = LabelTemplate.mm2Pixel(setting.offsetY.toFloat())

        HPRTPrinterHelper.Reference("0","0")

        val data = labelTemplate.printSingleGoods(goods)
        for (it in data) {
            val b = it.createItemBitmap(Color.WHITE)
            val left = it.left - offX
            val top = it.top - offY
            if (it is BarcodeItem){
                val type = if (it.getRealBarcodeFormat() == CodeItemBase.BAROMETER.EAN13){
                    "EAN13"
                }else "128"

                if (it.radian != 0f)
                    HPRTPrinterHelper.printBarcode((left+ it.width).toString(),(top + it.height).toString(),type, it.height.toString(),"1",it.radian.toInt().toString(),"2","2",it.content.trim())
                else
                    HPRTPrinterHelper.printBarcode(left.toString(),top.toString(),type, it.height.toString(),"1","0","2","2",it.content.trim())
            }else {
                HPRTPrinterHelper.printImage(left.toString(),top.toString(), b, true, false, 1)
            }
        }
        code = HPRTPrinterHelper.Print("1","1")
        if (code < 0){

            if (mCallback != null){
                mCallback!!.onFailure()
            }else
                Utils.showToast(R.string.com_wyc_label_conn_fail)
        }
        printerStatus("Print",code)
    }

    private fun printerStatus(tag:String,code:Int){
        var s = "打印机正常"
        if (code < 0){
            when(HPRTPrinterHelper.getPrinterStatus()){
                HPRTPrinterHelper.STATUS_DISCONNECT ->{
                    s = "断开连接"
                    state = PRINTER_STATE.CLOSE
                    mCallback?.onDisconnect()
                }
                HPRTPrinterHelper.STATUS_TIMEOUT ->{
                    s = "查询超时"
                }
                HPRTPrinterHelper.STATUS_COVER_OPENED ->{
                    s = "开盖"
                }
                HPRTPrinterHelper.STATUS_NOPAPER ->{
                    s = "缺纸"
                }
                HPRTPrinterHelper.STATUS_OVER_HEATING ->{
                    s = "过热"
                }
                HPRTPrinterHelper.STATUS_PRINTING ->{
                    s = "打印中"
                }
                HPRTPrinterHelper.STATUS_OK ->{
                    state = PRINTER_STATE.OPEN
                }
            }
        }
        Log.e(tag,"code:$code,state:$s")
    }

    override fun close() {
        val code = HPRTPrinterHelper.PortClose()
        Log.e("close","code:$code")
        state = PRINTER_STATE.CLOSE
        isExit = true
        super.close()
    }
}