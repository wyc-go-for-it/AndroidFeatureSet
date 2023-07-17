package com.wyc.label.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.graphics.*
import android.util.Log
import com.wyc.label.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.*


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.label.printer
 * @ClassName:      ThermalPrinter
 * @Description:    热敏打印
 * @Author:         wyc
 * @CreateDate:     2023-05-31 15:08
 * @UpdateUser:     更新者：
 * @UpdateDate:     2023-05-31 15:08
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class ThermalPrinter: AbstractPrinter(),CoroutineScope by CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler{_,e ->
    Utils.showToast(e.message)
}){
    private var mAddress:String = ""
    private val mutex = Mutex()
    private var mConnect:android.bluetooth.BluetoothSocket? = null
    override fun open(arg: String) {
        mAddress = arg
        launch {
            mutex.withLock {
                connect()
            }
        }
    }

    private fun connect():Boolean{
        if (mConnect == null){

            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null) {
                val bluetoothDevice: BluetoothDevice = bluetoothAdapter.getRemoteDevice(mAddress)
                if (mCallback != null) {
                    mCallback!!.onConnecting()
                } else
                    Utils.showToast(R.string.com_wyc_label_printer_connecting)

                try {
                    mConnect = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                    mConnect!!.connect()

                    if (mCallback != null) {
                        mCallback!!.onSuccess(this@ThermalPrinter)
                    } else
                        Utils.showToast(R.string.com_wyc_label_conn_success)

                    return true

                }catch (e:IOException){
                    if (mCallback != null) {
                        mCallback!!.onFailure()
                    } else
                        Utils.showToast(R.string.com_wyc_label_conn_fail)
                }
            }else Utils.showToast(R.string.not_support_bluetooth)

            return false
        }
        return true
    }

    override fun print(labelTemplate: LabelTemplate, goods: LabelGoods) {
        launch {

            val bt =  async {
                draw2PxPoint(printSingleGoodsBitmap(labelTemplate,goods))
            }

            mutex.withLock {
                try {
                    if (!mConnect!!.isConnected){
                        mConnect!!.connect()
                    }

                    Utils.showToast(R.string.com_wyc_label_printer_printing)

                    val setting = LabelPrintSetting.getSetting()
                    var gap = setting.paperType.value
                    val rn = "\n".toByteArray()
                    mConnect!!.outputStream.apply {
                        write(bt.await())
                        write(byteArrayOf(0x1b, 0x40))
                        while (gap-- > 0){
                            write(rn)
                        }
                    }

                    if (mCallback != null) {
                        mCallback!!.onReceive()
                    } else
                        Utils.showToast(R.string.com_wyc_label_print_success)

                } catch (e: IOException) {
                    if (mCallback != null) {
                        mCallback!!.onFailure()
                    } else
                        Utils.showToast(R.string.com_wyc_label_print_failure)
                }
            }
        }
    }

    override fun close() {
        super.close()
        launch {
            mutex.withLock {
                mConnect?.close()
                mConnect = null
            }
        }
    }

    companion object{
        @JvmStatic
        fun printSingleGoodsBitmap(labelTemplate: LabelTemplate, labelGoods: LabelGoods):Bitmap{
            val dpi = LabelPrintSetting.getSetting().dpi

            val wDot = labelTemplate.width2Dot(dpi).toFloat()
            val hDot = labelTemplate.height2Dot(dpi).toFloat()

            val bmp = Bitmap.createBitmap(wDot.toInt(), hDot.toInt(),Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            c.drawColor(Color.WHITE)


            val p = Paint()
            p.style = Paint.Style.STROKE

            val itemCopy: MutableList<ItemBase> = labelTemplate.printSingleGoods(labelGoods)

            itemCopy.forEach {
                val b = it.createItemBitmap(Color.TRANSPARENT)
                c.save()

                c.translate(it.left.toFloat(), it.top.toFloat())

                c.drawBitmap(b,0f,0f,null)

                c.restore()
            }

            return bmp
        }
    }



    private fun draw2PxPoint(bit:Bitmap):ByteArray {
        val newBit = compressPic(bit);
        val w = newBit.width;
        val h = newBit.height;

        val data = ByteArray(w * h / 24 * 3 + h / 24*8)//数据大小 + 指令字节
        var k = 0
        val n2 = (w / 256)
        val n1 = (w % 256)

        for (i in 0 until (h / 24)){
            data[k++] = 0x1B
            data[k++] = 0x2A
            data[k++] = 33
            data[k++] = n1.toByte()
            data[k++] = n2.toByte()

            for (j in 0 until w){
                for (p in 0 until 3){
                    for (l in 0 until 8){
                        val b = px2Binaryzation(j, i * 24 + p * 8 + l,newBit)
                        val t = (data[k] + b).toByte()
                        data[k] = (t + data[k]).toByte()
                    }
                    k++
                }
            }

            data[k++] = 0x1b
            data[k++] = 0x33
            data[k++] = 0x10
        }
        return data
    }

    private fun px2Binaryzation(x: Int, y: Int, bit: Bitmap): Byte {
        //最高一个字节为alpha;
        val b: Byte
        val pixel = bit.getPixel(x, y)
        val red = pixel and 0x00ff0000 shr 16 // 取高两位
        val green = pixel and 0x0000ff00 shr 8 // 取中两位
        val blue = pixel and 0x000000ff // 取低两位
        val gray: Int = (0.29900 * red + 0.58700 * green + 0.11400 * blue).toInt()
        b = if (gray < 128) {
            1
        } else {
            0
        }
        return b
    }

    private fun compressPic(bitmapOrg: Bitmap): Bitmap {
        // 获取这个图片的宽和高
        val width = bitmapOrg.width
        val height = bitmapOrg.height
        val targetBmp = Bitmap.createBitmap(
            alignToN(width, 8),
            alignToN(
                height,
                24
            ),
            Bitmap.Config.ARGB_8888
        )
        val targetCanvas = Canvas(targetBmp)
        targetCanvas.drawColor(-0x1)
        targetCanvas.drawBitmap(
            bitmapOrg,
            Rect(0, 0, width, height),
            Rect(
                0,
                0,
                 alignToN(width, 8),
                 alignToN(
                    height,
                    24
                )
            ),
            null
        )
        return targetBmp
    }
    private fun alignToN(num: Int, N: Int): Int { //N对齐target
        return if (num % N != 0) num + (N - num % N) else num
    }

}