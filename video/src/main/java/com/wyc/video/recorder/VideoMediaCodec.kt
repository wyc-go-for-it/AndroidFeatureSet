package com.wyc.video.recorder

import android.graphics.ImageFormat
import android.media.*
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import com.wyc.logger.Logger
import com.wyc.video.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.video.recorder
 * @ClassName:      VideoMediaCodec
 * @Description:    基于mediaCodec录制视频
 * @Author:         wyc
 * @CreateDate:     2022/7/28 14:24
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/7/28 14:24
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class VideoMediaCodec:AbstractRecorder() {
    private var mImageReaderYUV : ImageReader? = null
    private var mImageReaderThread:HandlerThread? = null
    private val mImageReaderLock = ReentrantLock()

    private var mCodec: MediaCodec? = null
    private var mOutputFormat:MediaFormat = MediaFormat.createVideoFormat(MIMETYPE, WIDTH, HEIGHT)
    private var mCodeThread:HandlerThread? = null
    private val mCodeLock = ReentrantLock()

    private val  YUVQueue: ArrayBlockingQueue<ByteArray>  = ArrayBlockingQueue<ByteArray> (10)
    val mYuvBuffer = ByteBuffer.allocate(WIDTH * HEIGHT * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8)

    override fun configure() {
        mImageReaderYUV = ImageReader.newInstance(WIDTH,HEIGHT, ImageFormat.YUV_420_888,2)
    }

    private fun initCodec(){
        if (mCodec != null)return

        initMediaFormat()
        try {
            mCodec = MediaCodec.createEncoderByType(MIMETYPE).apply {
                reset()

                setCallback(object : MediaCodec.Callback() {
                    override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {

                        try {
                            mCodeLock.lock()

                            if (mCodec != null){
                                val bytes = YUVQueue.take()
                                val bufferInfo = codec.getInputBuffer(index)
                                bufferInfo!!.put(bytes)
                                codec.queueInputBuffer(index,0,bufferInfo.position(),0,0)
                            }

                        }catch (_:InterruptedException){

                        }finally {
                            mCodeLock.unlock()
                        }
                    }
                    override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                        try {
                            mCodeLock.lock()

                            if (mCodec != null){
                                val mediaFormat = codec.getOutputFormat(index)
                                val buffer = codec.getOutputBuffer(index)

                                val offset = info.offset
                                val size = info.size
                                val presentTimes = info.presentationTimeUs

                                Log.e("onOutputBufferAvailable","offset:${offset},size:${size},presentTimes:${presentTimes}")

                                codec.releaseOutputBuffer(index,false)
                            }

                        }finally {
                            mCodeLock.unlock()
                        }
                    }

                    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                        Utils.logInfo("CodecException:$e")
                    }

                    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                        mOutputFormat = format
                    }
                },Handler(mCodeThread!!.looper))

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
                    configure(mOutputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                }else {
                    configure(mOutputFormat, null, MediaCodec.CONFIGURE_FLAG_ENCODE, null)
                }
            }
        }catch (e: IOException){
            e.printStackTrace()
        }catch (e: IllegalArgumentException){
            e.printStackTrace()
        }catch (e: IllegalStateException){
            e.printStackTrace()
        }catch (e: MediaCodec.CodecException){
            e.printStackTrace()
        }
    }

    private val mImageReaderYUVCallback = ImageReader.OnImageAvailableListener {
        try {
            mImageReaderLock.lock()

            val image = it.acquireLatestImage()
            if (image != null){
                if (image.format == ImageFormat.YUV_420_888){

                    val  planes = image.planes
                    val yPlane = planes[0]
                    val uPlane = planes[1]
                    val vPlane = planes[2]

                    val yBuffer = yPlane.buffer
                    val uBuffer = uPlane.buffer
                    val vBuffer = vPlane.buffer

                    mYuvBuffer.rewind()
                    mYuvBuffer.put(yBuffer)
                    while (mYuvBuffer.hasRemaining()){
                        mYuvBuffer.put(uBuffer.get())
                        mYuvBuffer.put(vBuffer.get())
                    }

                    val bytes = ByteArray(it.width * it.height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8)
                    System.arraycopy(mYuvBuffer.array(),0,bytes,0,mYuvBuffer.position())

                    try {
                        YUVQueue.put(bytes)
                    }catch (_:InterruptedException){
                        YUVQueue.clear()
                    }

/*                    Logger.d("yuvWidth:%d,yuvHeight:%d,YpixelStride:%d,YrowStride:%d,VpixelStride:%d,VrowStride:%d,UpixelStride:%d,UrowStride:%d",
                        it.width,it.height,yPlane.pixelStride,yPlane.rowStride,vPlane.pixelStride,vPlane.rowStride,uPlane.pixelStride,uPlane.rowStride)*/

                }

                image.close()
            }

        }finally {
            mImageReaderLock.unlock()
        }
    }



    private fun initMediaFormat(){
        mOutputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities. COLOR_FormatYUV420Flexible)
        mOutputFormat.setInteger(MediaFormat.KEY_FRAME_RATE,30)
        mOutputFormat.setInteger(MediaFormat.KEY_BIT_RATE, WIDTH * HEIGHT * 4)
        mOutputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1)
        /*
        * upon four parameters must be set,otherwise will cause "configure failed with err 0x80001001"
        * */
        mOutputFormat.setInteger(MediaFormat.KEY_LEVEL,MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
    }

    private fun selectEncoderCodec(mimeType: String): MediaCodecInfo? {
        val list = MediaCodecList(MediaCodecList.ALL_CODECS)
        val codecs = list.codecInfos
        codecs.forEach {
            if(it.isEncoder){
                val types = it.supportedTypes
                for (j in types.indices) {
                    if (types[j].equals(mimeType, ignoreCase = true)) {
                        Utils.logInfo("mimeType:$mimeType,name:${it.name}")
                        return it
                    }
                }
            }
        }
        return null
    }

    override fun getSurface(): Surface {
        if (mImageReaderYUV == null)throw IllegalArgumentException("must invoke configure method.")
        return mImageReaderYUV!!.surface
    }

    override fun start() {
        startCodeThread()
        startImageReaderThread()
        mImageReaderYUV!!.setOnImageAvailableListener(mImageReaderYUVCallback,Handler(mImageReaderThread!!.looper))
        try {
            initCodec()
            mCodec!!.start()
        }catch (e:IllegalStateException){
            e.printStackTrace()
        }catch (e:MediaCodec.CodecException){
            e.printStackTrace()
        }
    }

    override fun stop() {
        if (mCodec != null){
            try {
                mCodeLock.lock()

                try {
                    mCodec!!.stop()
                }catch (e:IllegalStateException){
                    e.printStackTrace()
                }
                mCodec!!.release()
                mCodec = null
            }finally {
                mCodeLock.unlock()
            }
        }
        stopImageReaderThread()
        stopCodeThread()
    }

    override fun release() {
        if (mImageReaderYUV != null){
            try {
                mImageReaderLock.lock()
                mImageReaderYUV!!.close()
            }finally {
                mImageReaderLock.unlock()
            }
            mImageReaderYUV = null
        }
        stop()

        super.release()
    }

    private fun startCodeThread(){
        stopCodeThread()
        mCodeThread = HandlerThread("CodeThread")
        mCodeThread!!.start()
    }
    private fun stopCodeThread() {
        if (mCodeThread != null) {
            mCodeThread!!.interrupt()
            mCodeThread!!.quitSafely()
            try {
                mCodeThread!!.join()
                mCodeThread = null
            } catch (e: InterruptedException) {
            }
        }
    }

    private fun startImageReaderThread(){
        stopImageReaderThread()
        mImageReaderThread = HandlerThread("ImageReaderThread")
        mImageReaderThread!!.start()
    }
    private fun stopImageReaderThread() {
        if (mImageReaderThread != null) {
            mImageReaderThread!!.interrupt()
            mImageReaderThread!!.quitSafely()
            try {
                mImageReaderThread!!.join()
                mImageReaderThread = null
            } catch (e: InterruptedException) {
            }
        }
    }
    companion object{
        const val MIMETYPE = "video/avc"
        const val WIDTH = 1280
        const val HEIGHT = 720
    }
}