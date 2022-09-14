package com.wyc.label.printer

import android.graphics.Color
import android.util.Log
import com.gprinter.bean.PrinterDevices
import com.gprinter.command.LabelCommand
import com.gprinter.io.*
import com.gprinter.utils.CallbackListener
import com.gprinter.utils.Command
import com.gprinter.utils.ConnMethod
import com.wyc.label.*
import com.wyc.label.LabelPrintSetting.Companion.getSetting
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.label
 * @ClassName:      GPPrinterI
 * @Description:    作用描述
 * @Author:         wyc
 * @CreateDate:     2022/4/22 15:32
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/4/22 15:32
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class GPPrinter: PrinterStateCallback, AbstractPrinter() {

    private var portManager: PortManager? = null

    private fun connect(devices: PrinterDevices?) {
        CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler{_,e->
            Utils.showToast(e.message)
        }).launch() {
            if (portManager != null) { //先close上次连接
                portManager!!.closePort()
                try {
                    Thread.sleep(2000)
                } catch (e: InterruptedException) {
                }
            }
            if (devices != null) {
                when (devices.connMethod) {
                    ConnMethod.BLUETOOTH -> {
                        portManager = BluetoothPort(devices)
                        portManager!!.openPort()
                    }
                    ConnMethod.USB -> {
                        portManager = UsbPort(devices)
                        portManager!!.openPort()
                    }
                    ConnMethod.WIFI -> {
                        portManager = EthernetPort(devices)
                        portManager!!.openPort()
                    }
                    ConnMethod.SERIALPORT -> {
                        portManager = SerialPort(devices)
                        portManager!!.openPort()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun showPrinterStatusDescription(status: Int) {
        when (status) {
            -1 -> {
                Utils.showToast(R.string.com_wyc_label_status_fail)
            }
            1 -> {
                Utils.showToast(R.string.com_wyc_label_status_feed)
            }
            0 -> {
                Utils.showToast(R.string.com_wyc_label_status_normal)
            }
            -2 -> {
                Utils.showToast(R.string.com_wyc_label_status_out_of_paper)
            }
            -3 -> {
                Utils.showToast(R.string.com_wyc_label_status_open)
            }
            -4 -> {
                Utils.showToast(R.string.com_wyc_label_status_overheated)
            }
        }
    }

    fun hasConn():Boolean{
        return portManager != null && portManager!!.connectStatus
    }

    private fun getGPTscCommand(labelTemplate: LabelTemplate, labelGoods: LabelGoods?): LabelCommand {
        val tsc = LabelCommand()
        if (labelGoods != null) {
            val setting = getSetting()
            val offsetX = setting.offsetX
            val offsetY = setting.offsetY
            tsc.addSize(labelTemplate.width, labelTemplate.height)
            tsc.addGap(5)
            tsc.addDirection(LabelCommand.DIRECTION.FORWARD, LabelCommand.MIRROR.NORMAL)
            tsc.addReference(offsetX, offsetY)
            tsc.addDensity(fromInt(setting.density))
            tsc.addCls()
            val data = labelTemplate.printSingleGoods(labelGoods)
            for (it in data) {
                val b = it.createItemBitmap(Color.WHITE)
                tsc.drawImage(it.left, it.top, b.width, b)
            }
            tsc.addPrint(1, 1)
        }
        return tsc
    }

    private fun fromInt(value: Int) = LabelCommand.DENSITY.values().first { it.value == value }

    override fun onConnecting() {
        Utils.showToast(R.string.com_wyc_label_printer_connecting)
    }

    override fun onSuccess(printer:IPrinter) {
        Utils.showToast(R.string.com_wyc_label_conn_success)
    }

    override fun onReceive() {
        Log.d(javaClass.simpleName, "onReceive")
    }

    override fun onFailure() {
        Utils.showToast(R.string.com_wyc_label_conn_fail)
    }

    override fun onDisconnect() {
        Utils.showToast(R.string.com_wyc_label_printer_disconnect)
    }

    override fun open(arg: String) {
        val blueTooth = PrinterDevices.Build()
            .setContext(LabelApp.getInstance())
            .setConnMethod(ConnMethod.BLUETOOTH)
            .setMacAddress(arg)
            .setCommand(Command.TSC)
            .setCallbackListener(object :CallbackListener{
                override fun onConnecting() {
                   mCallback?.onConnecting()
                }

                override fun onCheckCommand() {

                }

                override fun onSuccess(p0: PrinterDevices?) {
                    mCallback?.onSuccess(this@GPPrinter)
                }

                override fun onReceive(p0: ByteArray?) {
                    mCallback?.onReceive()
                }

                override fun onFailure() {
                    mCallback?.onFailure()
                }

                override fun onDisconnect() {
                    mCallback?.onDisconnect()
                }

            }).build()
        connect(blueTooth)
    }

    override fun print(labelTemplate: LabelTemplate, goods: LabelGoods) {
        if (portManager == null) {
            Utils.showToast(R.string.com_wyc_label_printer_not_init)
            return
        }
        if (!portManager!!.connectStatus){
            Utils.showToast(R.string.com_wyc_label_not_connect)
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = portManager!!.writeDataImmediately(getGPTscCommand(labelTemplate,goods).command)
                if (!result) {
                    showPrinterStatusDescription(portManager!!.getPrinterStatus(portManager!!.command))
                }
            }catch (e:IOException){
                Utils.showToast(e.localizedMessage)
            }
        }
    }

    override fun close() {
        super.close()
        if (portManager != null) {
            portManager!!.closePort()
            portManager = null
        }
    }
}