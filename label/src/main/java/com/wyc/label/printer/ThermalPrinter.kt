package com.wyc.label.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.graphics.*
import com.wyc.label.*
import com.wyc.label.DataItem
import com.wyc.label.LabelPrintSetting
import kotlinx.coroutines.*
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

class ThermalPrinter: AbstractPrinter(){
    private var mAddress:String = ""
    override fun open(arg: String) {
        mAddress = arg
        if (mCallback != null) {
            mCallback!!.onSuccess(this)
        } else
            Utils.showToast(R.string.com_wyc_label_conn_success)
    }

    override fun print(labelTemplate: LabelTemplate, goods: LabelGoods) {
        CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler{_,e ->
            Utils.showToast(e.message)
        }).launch {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null) {
                if (bluetoothAdapter.isEnabled) {

                    val bt =  async {
                        draw2PxPoint(printSingleGoodsBitmap(labelTemplate,goods))
                    }

                    val bluetoothDevice: BluetoothDevice =
                        bluetoothAdapter.getRemoteDevice(mAddress)
                    try {
                        if (mCallback != null) {
                            mCallback!!.onConnecting()
                        } else
                            Utils.showToast(R.string.com_wyc_label_printer_printing)

                        bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                            .use { socket ->
                                socket.connect()
                                socket.outputStream.use {
                                    it.write(bt.await())
                                }
                            }

                        if (mCallback != null) {
                            mCallback!!.onSuccess(this@ThermalPrinter)
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
    }

    private fun printSingleGoodsBitmap(labelTemplate: LabelTemplate, labelGoods: LabelGoods):Bitmap{
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


    fun draw2PxPoint(bit:Bitmap):ByteArray {
        val newBit = compressPic(bit);
        val w = newBit.width;
        val h = newBit.height;

        val data = ByteArray(w * h + h / 24*6 + 8 );//图片大小 + 指令字节 + 留空字节
        var k = 0
        val n2 = (w / 256).toByte()
        val n1 = (w - 256*n2).toByte()

        for (i in 0 until (h / 24)){
            data[k++] = 0x1B;
            data[k++] = 0x2A;
            data[k++] = 33 // m=33时，选择24点双密度打印，分辨率达到200DPI。
            data[k++] = n1
            data[k++] = n2

            for (j in 0 until w){
                for (p in 0 until 3){
                    for (l in 0 until 8){
                        val b = px2Binaryzation(j, i * 24 + p * 8 + l,newBit);
                        val t = (data[k] + b).toByte()
                        data[k] = (t + data[k]).toByte()
                    }
                    k++
                }
            }
            data[k++] = 10;
        }
        return data
    }

    fun px2Binaryzation(x: Int, y: Int, bit: Bitmap): Byte {
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

    fun compressPic(bitmapOrg: Bitmap): Bitmap {
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