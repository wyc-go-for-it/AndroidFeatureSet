package com.wyc.label

import android.graphics.Color
import android.util.Log
import android.widget.Toast
import com.gprinter.bean.PrinterDevices
import com.gprinter.command.LabelCommand
import com.gprinter.io.*
import com.gprinter.utils.CallbackListener
import com.gprinter.utils.Command
import com.gprinter.utils.ConnMethod
import com.wyc.label.LabelPrintSetting.Companion.getSetting
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

class GPPrinter: CallbackListener {

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
            CoroutineScope(Dispatchers.IO).launch {
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
                    .setContext(App.getInstance())
                    .setConnMethod(ConnMethod.BLUETOOTH)
                    .setMacAddress(mac)
                    .setCommand(Command.TSC)
                    .setCallbackListener(GPPrinter.getInstance()).build()
            GPPrinter.connect(blueTooth)
        }

        @JvmStatic
        fun openBlueTooth(mac: String?, callbackListener: CallbackListener?) {
            Log.d("connecting printer. mac is %s", mac!!)
            val blueTooth = PrinterDevices.Build()
                    .setContext(App.getInstance())
                    .setConnMethod(ConnMethod.BLUETOOTH)
                    .setMacAddress(mac)
                    .setCommand(Command.TSC)
                    .setCallbackListener(callbackListener).build()
            GPPrinter.connect(blueTooth)
        }

        @Throws(IOException::class)
        @JvmStatic
        fun sendDataToPrinter(vector: Vector<Byte?>?) {
            if (portManager == null) {
                Toast.makeText(App.getInstance(), R.string.com_wyc_label_printer_not_init, Toast.LENGTH_LONG).show()
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
                    Toast.makeText(App.getInstance(), R.string.status_fail, Toast.LENGTH_LONG).show()
                }
                1 -> {
                    Toast.makeText(App.getInstance(), R.string.status_feed, Toast.LENGTH_LONG).show()
                }
                0 -> {
                    Toast.makeText(App.getInstance(), R.string.status_normal, Toast.LENGTH_LONG).show()
                }
                -2 -> {
                    Toast.makeText(App.getInstance(), R.string.status_out_of_paper, Toast.LENGTH_LONG).show()
                }
                -3 -> {
                    Toast.makeText(App.getInstance(), R.string.status_open, Toast.LENGTH_LONG).show()
                }
                -4 -> {
                    Toast.makeText(App.getInstance(), R.string.status_overheated, Toast.LENGTH_LONG).show()
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
        Toast.makeText(App.getInstance(), R.string.com_wyc_label_printer_connecting, Toast.LENGTH_LONG).show()
    }

    override fun onCheckCommand() {
        Log.d(this.javaClass.simpleName, "onCheckCommand")
    }

    override fun onSuccess(printerDevices: PrinterDevices?) {
        Toast.makeText(App.getInstance(), R.string.com_wyc_label_conn_success, Toast.LENGTH_LONG).show()
    }

    override fun onReceive(bytes: ByteArray?) {
        Log.d(javaClass.simpleName, "onReceive")
    }

    override fun onFailure() {
        Toast.makeText(App.getInstance(), R.string.com_wyc_label_conn_fail, Toast.LENGTH_LONG).show()
    }

    override fun onDisconnect() {
        Toast.makeText(App.getInstance(), R.string.com_wyc_label_printer_disconnect, Toast.LENGTH_LONG).show()
    }
}