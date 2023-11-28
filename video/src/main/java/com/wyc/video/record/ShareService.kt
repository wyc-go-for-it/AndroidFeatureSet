package com.wyc.video.record

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import androidx.core.app.NotificationCompat
import com.wyc.video.R
import com.wyc.video.Utils
import com.wyc.video.Utils.Companion.intToByteArray
import com.wyc.video.recorder.AbstractRecorder
import com.wyc.video.recorder.VideoMediaCodec
import kotlinx.coroutines.*
import java.io.DataInputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

class ShareService : Service(),CoroutineScope by CoroutineScope(Dispatchers.IO) {

    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mImageReaderYUV : ImageReader? = null

    private val  NOTIFICATION_CHANNEL_ID = "88"
    private val  NOTIFICATION_CHANNEL_NAME = "c"
    private val NOTIFICATION_CHANNEL_DESC = "d"

    private var mInputCodec: MediaCodec? = null
    private var WIDTH  = 1080
    private var HEIGHT = 2289
    private var mInputSurface:Surface? = null
    private var mCodecThread:HandlerThread? = null
    private var mCodecHandler:Handler? = null

    private var mOutputCodec: MediaCodec? = null
    private var mOutputSurface:Surface? = null

    private val  YUVQueue: ArrayBlockingQueue<ByteArray> = ArrayBlockingQueue<ByteArray> (10)


    private var mSocket:Socket? = null
    private var mOutputStream:OutputStream? = null

    private var mEventSocket: ServerSocket? = null

    override fun onCreate() {
        super.onCreate()

        initEventService()
    }

    private fun initEventService(){
        launch {
            ServerSocket(7777).use {
                mEventSocket = it
                while (isActive){
                    try {
                        val client = it.accept()
                        Log.e("event accept",client.inetAddress.hostAddress)
                        val read = DataInputStream(client.getInputStream())
                        val byteArray = ByteArray(1024)
                        while (isActive){
                            val x = read.readInt()
                            val y = read.readInt()
                            Log.e("coordinates",String.format("x:%d,y:%d",x,y))
                            val  instrumentation = Instrumentation()
                            val event = MotionEvent.obtain(
                                SystemClock.uptimeMillis(),
                                SystemClock.uptimeMillis(),
                                MotionEvent.ACTION_DOWN,
                                x.toFloat(),
                                y.toFloat(),
                                0
                            )
                            val eventUp = MotionEvent.obtain(
                                SystemClock.uptimeMillis(),
                                SystemClock.uptimeMillis(),
                                MotionEvent.ACTION_UP,
                                x.toFloat(),
                                y.toFloat(),
                                0
                            )
                            instrumentation.sendPointerSync(event)
                            instrumentation.sendPointerSync(eventUp)
                            event.recycle()
                        }
                    }catch (e: IOException){
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startNotification()
        val resultCode = intent.getIntExtra("resultCode",-8)
        if (resultCode != -8){
            val data = intent.getParcelableExtra<Intent>("data")
            WIDTH = intent.getIntExtra("width",0)/2
            HEIGHT = intent.getIntExtra("height",0)/2
            Log.e("onStartCommand", String.format("width:%d,height:%d",WIDTH,HEIGHT))
            //mOutputSurface = intent.getParcelableExtra("surface")
            val density = intent.getIntExtra("density",0)

            initRenderConnect()

            initBuffer()

            initInputCodec()

            mMediaProjection = (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).getMediaProjection(resultCode,data!!)
            mVirtualDisplay = mMediaProjection!!.createVirtualDisplay("wyc", WIDTH,HEIGHT,density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,mInputSurface,null,null)

            initOutputCodec()

            Log.e("shareService","start")
        }else{
            stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun initRenderConnect(){
        launch {
            try {
                mSocket = Socket(InetAddress.getByName("192.168.0.27"),9999)
                mOutputStream = mSocket!!.getOutputStream()
            }catch (e:IOException){
                e.printStackTrace()
            }
        }
    }

    private fun initOutputCodec(){
        if (mOutputCodec != null)return
        try {
            mOutputCodec = MediaCodec.createDecoderByType(VideoMediaCodec.MIMETYPE).apply {
                reset()
                val format = createMediaFormat()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
                    configure(format, mOutputSurface, null, 0)
                }else {
                    configure(format, mOutputSurface, 0, null)
                }
                launch {
                    val bufferInfo = MediaCodec.BufferInfo()
                    while (isActive){
                        val byteArray = YUVQueue.poll(100, TimeUnit.MILLISECONDS)
                        if (byteArray != null){
                            sendFrame(byteArray)
                            val inputIndex = dequeueInputBuffer(1000 * 100)
                            if (inputIndex > -1){
                                getInputBuffer(inputIndex)?.apply {
                                    clear()
                                    put(byteArray)
                                }
                                queueInputBuffer(inputIndex,0,byteArray.size,0,0)
                            }
                            val  outputIndex = dequeueOutputBuffer(bufferInfo,1000 * 100)
                            if (outputIndex > -1) releaseOutputBuffer(outputIndex,false)
                        }
                    }
                }
                start()
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun sendFrame(data:ByteArray){
        val header  = intToByteArray(data.size)
        val headerLen = header.size
        val size = data.size
        val sendData = ByteArray(headerLen + size)
        header.copyInto(sendData,0,0,headerLen)
        data.copyInto(sendData,4,0,size)
        mOutputStream?.apply {
            write(sendData)
            flush()
        }
    }



    private fun initInputCodec(){
        if (mInputCodec != null)return
        try {
            mInputCodec = MediaCodec.createEncoderByType(VideoMediaCodec.MIMETYPE).apply {
                reset()
                startCodeThread()

                setCallback(object : MediaCodec.Callback() {
                    override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                    }
                    override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                        if (mInputCodec != null){
                            if (info.size > 0){
                                codec.getOutputBuffer(index)?.apply {
                                    try {
                                        val byteArray = ByteArray(info.size)
                                        this.get(byteArray)
                                        YUVQueue.put(byteArray)
                                    }catch (_:InterruptedException){
                                        YUVQueue.clear()
                                    }
                                }
                            }
                            codec.releaseOutputBuffer(index,false)
                        }
                    }

                    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                        Utils.logInfo("CodecException:$e")
                    }

                    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {

                    }
                }, mCodecHandler)

                val format = createMediaFormat()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
                    configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                }else {
                    configure(format, null, MediaCodec.CONFIGURE_FLAG_ENCODE, null)
                }
                mInputSurface = createInputSurface()

                start()
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun startCodeThread(){
        stopCodecThread()
        mCodecThread = HandlerThread("CodecThread")
        mCodecThread!!.start()
        mCodecHandler = Handler(mCodecThread!!.looper)
    }
    private fun stopCodecThread() {
        if (mCodecThread != null) {
            mCodecThread!!.quitSafely()
            try {
                mCodecThread!!.join()
                mCodecThread = null
                mCodecHandler = null
            } catch (e: InterruptedException) {
            }
        }
    }

    private fun createMediaFormat():MediaFormat{
        val videoOutputFormat:MediaFormat = MediaFormat.createVideoFormat(VideoMediaCodec.MIMETYPE, WIDTH, HEIGHT)
        videoOutputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities. COLOR_FormatYUV420Flexible)
        videoOutputFormat.setInteger(MediaFormat.KEY_FRAME_RATE,24)
        videoOutputFormat.setInteger(MediaFormat.KEY_BIT_RATE, WIDTH * HEIGHT * 4)
        videoOutputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,5)
        /*
        * upon four parameters must be set,otherwise will cause "configure failed with err 0x80001001"
        * */
        videoOutputFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)

        return videoOutputFormat
    }

    private fun createVideoFile(): File {
        val file = AbstractRecorder.getVideoDir()
        val name = String.format(
            Locale.CHINA, "%s%s%s.mp4", file.absolutePath, File.separator,
            SimpleDateFormat("yyyyMMddHHmmssSS", Locale.CHINA).format(Date()))
        return File(name)
    }

    private fun initBuffer(){
        mImageReaderYUV = ImageReader.newInstance(WIDTH,HEIGHT, ImageFormat.JPEG,2)
        mImageReaderYUV!!.setOnImageAvailableListener(mImageReaderYUVCallback,null)
    }

    private val mImageReaderYUVCallback = ImageReader.OnImageAvailableListener {
        it.acquireLatestImage()?.let {image->
            val w = image.width
            val h = image.height

            Log.e("OnImageAvailableListener", "width:$w,height:$h")

            image.close()
        }
    }

    private fun startNotification() {
        val pendingIntent = PendingIntent.getService(this,9999,Intent(this,ShareService::class.java),0)
        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        resources,
                        R.drawable.ic_launcher_foreground
                    )
                )
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Starting Service")
                .setContentText("Starting monitoring service")
                .addAction(NotificationCompat.Action(0,"stop Service",pendingIntent))
        val notification: Notification = notificationBuilder.build()
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = NOTIFICATION_CHANNEL_DESC
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        startForeground(
            1,
            notification
        )
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()

        startCodeThread()

        if (mImageReaderYUV != null){
            mImageReaderYUV!!.close()
            mImageReaderYUV = null
        }

        if (mVirtualDisplay != null){
            mVirtualDisplay!!.release()
            mVirtualDisplay = null
        }

        if (mMediaProjection != null){
            mMediaProjection!!.stop()
            mMediaProjection = null
        }


        if (mInputCodec != null){
            mInputCodec!!.release()
            mInputCodec = null
        }

        if (mInputSurface != null){
            mInputSurface!!.release()
            mInputSurface = null
        }
        if (mOutputCodec != null){
            mOutputCodec!!.release()
            mOutputCodec = null
        }

        if (mSocket != null){
            mSocket!!.close()
            mSocket = null
        }

        if (mEventSocket != null){
            mEventSocket!!.close()
            mEventSocket = null
        }

        Log.e("shareService","destroy")
    }
}