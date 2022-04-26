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

internal class GPPrinter: CallbackListener {

    companion object {
        private var printer: GPPrinter? = null
        private var portManager: PortManager? = null

        @JvmStatic
        fun getInstance(): GPPrinter? {
            if (printer == null) {
                printer = GPPrinter()
            }
            return printer
        }

        @JvmStatic
        fun connect(devices: PrinterDevices?) {
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

        @JvmStatic
        fun openBlueTooth(mac: String?) {
            val blueTooth = PrinterDevices.Build()
                    .setContext(LabelApp.getInstance())
                    .setConnMethod(ConnMethod.BLUETOOTH)
                    .setMacAddress(mac)
                    .setCommand(Command.TSC)
                    .setCallbackListener(getInstance()).build()
            connect(blueTooth)
        }

        @JvmStatic
        fun openBlueTooth(mac: String?, callbackListener: CallbackListener?) {
            Log.d("connecting printer. mac is %s", mac!!)
            val blueTooth = PrinterDevices.Build()
                    .setContext(LabelApp.getInstance())
                    .setConnMethod(ConnMethod.BLUETOOTH)
                    .setMacAddress(mac)
                    .setCommand(Command.TSC)
                    .setCallbackListener(callbackListener).build()
            connect(blueTooth)
        }

        @Throws(IOException::class)
        @JvmStatic
        fun sendDataToPrinter(vector: Vector<Byte?>?) {
            if (portManager == null) {
                Utils.showToast(R.string.com_wyc_label_printer_not_init)
                return
            }
            if (!portManager!!.connectStatus){
                Utils.showToast(R.string.com_wyc_label_not_connect)
                return
            }
            CoroutineScope(Dispatchers.IO).launch {
                val result = portManager!!.writeDataImmediately(vector)
                if (!result) {
                    showPrinterStatusDescription(portManager!!.getPrinterStatus(portManager!!.command))
                }
            }
        }

        @JvmStatic
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
        @JvmStatic
        fun close() {
            if (portManager != null) {
                portManager!!.closePort()
                portManager = null
            }
            if (printer != null) {
                printer = null
            }
        }
        @JvmStatic
        fun getGPTscCommand(labelTemplate: LabelTemplate, labelGoods: DataItem.LabelGoods?): LabelCommand {
            val tsc = LabelCommand()
            if (labelGoods != null) {
                val setting = getSetting()
                val offsetX = setting.offsetX
                val offsetY = setting.offsetY
                tsc.addSize(labelTemplate.getWidth(), labelTemplate.getHeight())
                tsc.addGap(5)
                tsc.addDirection(LabelCommand.DIRECTION.FORWARD, LabelCommand.MIRROR.NORMAL)
                tsc.addReference(offsetX, offsetY)
                tsc.addDensity(LabelCommand.DENSITY.DNESITY4)
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
    }

    override fun onConnecting() {
        Utils.showToast(R.string.com_wyc_label_printer_connecting)
    }

    override fun onCheckCommand() {
        Log.d(this.javaClass.simpleName, "onCheckCommand")
    }

    override fun onSuccess(printerDevices: PrinterDevices?) {
        Utils.showToast(R.string.com_wyc_label_conn_success)
    }

    override fun onReceive(bytes: ByteArray?) {
        Log.d(javaClass.simpleName, "onReceive")
    }

    override fun onFailure() {
        Utils.showToast(R.string.com_wyc_label_conn_fail)
    }

    override fun onDisconnect() {
        Utils.showToast(R.string.com_wyc_label_printer_disconnect)
    }
}