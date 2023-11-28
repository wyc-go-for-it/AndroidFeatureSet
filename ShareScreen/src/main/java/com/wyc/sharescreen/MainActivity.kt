package com.wyc.sharescreen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.IOException
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() , CoroutineScope by CoroutineScope(Dispatchers.IO),
    SurfaceHolder.Callback {
    private var exit = false
    private var mRenderSocket: ServerSocket? = null
    private var mOutputCodec: MediaCodec? = null
    private var WIDTH  = 1080
    private var HEIGHT = 2289
    private val  YUVQueue: ArrayBlockingQueue<ByteArray> = ArrayBlockingQueue<ByteArray> (10)
    private var mSurface: Surface? = null
    private var c:Long = 0

    private var mEventSocket:Socket? = null
    private var mEventStream:OutputStream? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<SurfaceView>(R.id.surfaceView).apply {
            holder.addCallback(this@MainActivity)
        }

        initService()
    }

    private fun initEventSocket(){
        mEventSocket = Socket(InetAddress.getByName("192.168.0.29"),7777)
        mEventStream = mEventSocket!!.getOutputStream()
    }


    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        launch {

            if (ev.action == MotionEvent.ACTION_UP){
                val x = intToByteArray(ev.rawX.toInt())
                val y = intToByteArray(ev.rawY.toInt())

                val data = ByteArray(8)

                System.arraycopy(x,0,data,0,4)
                System.arraycopy(y,0,data,4,4)

                mEventStream?.write(data)
            }

        }

        return super.dispatchTouchEvent(ev)
    }

    private fun intToByteArray(c:Int):ByteArray{
        val byteArray = ByteArray(4)
        byteArray[0] = ((c shr 24) and 0xff).toByte()
        byteArray[1] = ((c shr 16) and 0xff).toByte()
        byteArray[2] = ((c shr 8) and 0xff).toByte()
        byteArray[3] = (c and 0xff).toByte()
        return byteArray
    }

    override fun onDestroy() {
        super.onDestroy()
        exit = true
        mRenderSocket?.close()
        if (mOutputCodec != null){
            mOutputCodec!!.release()
            mOutputCodec = null
        }

        mEventSocket?.close()
    }


    private fun initService(){
        thread {
            ServerSocket(9999).use {
                mRenderSocket = it
                while (!exit){
                    try {
                        val client = it.accept()
                        initEventSocket()

                        Log.e("accept",client.inetAddress.hostAddress)

                        val read = DataInputStream(client.getInputStream())
                        while (true){
                            var count = read.readInt()
                            val byteArray = ByteArray(count)
                            var off = 0
                            var rCount = read.read(byteArray,off,count)
                            off += rCount
                            while (rCount < count){
                                count -= rCount
                                rCount = read.read(byteArray,off,count)
                                off += rCount
                            }
                            YUVQueue.put(byteArray)
                        }
                    }catch (e: IOException){
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun initOutputCodec(){
        if (mOutputCodec != null)return
        try {
            mOutputCodec = MediaCodec.createDecoderByType("video/avc").apply {
                reset()
                val format = createMediaFormat()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
                    configure(format, mSurface, null, 0)
                }else {
                    configure(format, mSurface, 0, null)
                }

                launch {
                    val bufferInfo = MediaCodec.BufferInfo()
                    while (isActive){
                        val byteArray = YUVQueue.poll(100, TimeUnit.MILLISECONDS)
                        if (byteArray != null){
                            val inputIndex = dequeueInputBuffer(1000 * 100)
                            if (inputIndex > -1){
                                getInputBuffer(inputIndex)?.apply {
                                    clear()
                                    put(byteArray)
                                }
                                queueInputBuffer(inputIndex,0,byteArray.size,c++,0)
                            }
                            val  outputIndex = dequeueOutputBuffer(bufferInfo,1000 * 100)
                            if (outputIndex > -1) {
                                releaseOutputBuffer(outputIndex,true)
                            }
                        }
                    }
                }
                start()
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun createMediaFormat(): MediaFormat {
        val videoOutputFormat: MediaFormat = MediaFormat.createVideoFormat("video/avc", WIDTH, HEIGHT)
        videoOutputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities. COLOR_FormatYUV420Flexible)
        videoOutputFormat.setInteger(MediaFormat.KEY_FRAME_RATE,24)
        videoOutputFormat.setInteger(MediaFormat.KEY_BIT_RATE, WIDTH * HEIGHT)
        videoOutputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,5)
        /*
        * upon four parameters must be set,otherwise will cause "configure failed with err 0x80001001"
        * */
        videoOutputFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)

        return videoOutputFormat
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        mSurface = holder.surface

    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        WIDTH = width
        HEIGHT = height
        initOutputCodec()
        Log.e("surfaceChanged", String.format("width:%d,height:%d",width,height))
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }
}