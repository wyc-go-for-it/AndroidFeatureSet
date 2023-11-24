package com.wyc.video.record

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.wyc.video.R

class ShareService : Service() {

    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mImageReaderYUV : ImageReader? = null

    private val  NOTIFICATION_CHANNEL_ID = "88"
    private val  NOTIFICATION_CHANNEL_NAME = "c"
    private val NOTIFICATION_CHANNEL_DESC = "d"

    override fun onCreate() {
        super.onCreate()
    }
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startNotification()

        val resultCode = intent.getIntExtra("resultCode",-1)
        val data = intent.getParcelableExtra<Intent>("data")
        val width = intent.getIntExtra("width",0)
        val height = intent.getIntExtra("height",0)
        val density = intent.getIntExtra("density",0)

        initBuffer(width,height)

        mMediaProjection = (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).getMediaProjection(resultCode,data!!)
        mVirtualDisplay = mMediaProjection!!.createVirtualDisplay("wyc", width,height,density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,mImageReaderYUV!!.surface,null,null)

        Log.e("shareService","start")
        return super.onStartCommand(intent, flags, startId)
    }

    private fun initBuffer(width:Int,height:Int){
        mImageReaderYUV = ImageReader.newInstance(width,height, ImageFormat.JPEG,2)
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
        Log.e("shareService","destroy")
    }
}