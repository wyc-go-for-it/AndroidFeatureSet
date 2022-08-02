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
import com.wyc.video.YUVUtils
import com.wyc.video.camera.VideoCameraManager
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread


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

    private var mVideoCodec: MediaCodec? = null
    private var mVideoOutputFormat:MediaFormat = MediaFormat.createVideoFormat(MIMETYPE, WIDTH, HEIGHT)
    private var mCodeThread:HandlerThread? = null
    private val mCodeLock = ReentrantLock()

    private var mMediaMuxer:MediaMuxer? = null
    private var mVideoTrackIndex = -1
    private var mCurPts = 0L
    private var mBaseTime = 0L

    private val audioSource = MediaRecorder.AudioSource.MIC
    private val sampleRateInHz = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val channels = 1

    private var mAudio:AudioRecord? = null
    private var mAudioOutputFormat:MediaFormat = MediaFormat.createAudioFormat(AudioMIMETYPE, sampleRateInHz, channels)
    private var mAudioCodec: MediaCodec? = null
    private var mStopAudio = false
    private var mAudioTrackIndex = -1

    private val  YUVQueue: ArrayBlockingQueue<ByteArray>  = ArrayBlockingQueue<ByteArray> (10)
    private val mYuvBuffer: ByteBuffer = ByteBuffer.allocate(WIDTH * HEIGHT * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8)
    private val mCountDownLatch  = CountDownLatch(2)

    override fun configure() {
        mImageReaderYUV = ImageReader.newInstance(WIDTH,HEIGHT,ImageFormat.YUV_420_888,2)
    }

    private fun initAudioCodec(){
        if (mAudioCodec == null){
            try {
                mAudioCodec = MediaCodec.createEncoderByType(AudioMIMETYPE).apply {
                    initAudioFormat()
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
                        configure(mAudioOutputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                    }else {
                        configure(mAudioOutputFormat, null, MediaCodec.CONFIGURE_FLAG_ENCODE, null)
                    }

                    thread {
                        initAudio()

                        try {
                            start()

                            val byteBuffer = ByteBuffer.allocateDirect(1024)
                            val bufferInfo = MediaCodec.BufferInfo()
                            while (!mStopAudio){

                                mAudio!!.read(byteBuffer,1024)
                                val inputIndex = dequeueInputBuffer(-1)
                                if (inputIndex > -1){
                                    val bufferInfo = getInputBuffer(inputIndex)
                                    bufferInfo?.put(byteBuffer.array())
                                    queueInputBuffer(inputIndex,0,1024,0,0)
                                }

                                val  outputIndex = dequeueOutputBuffer(bufferInfo,-1)
                                if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){

                                    mAudioOutputFormat = outputFormat
                                    mAudioTrackIndex = mMediaMuxer?.addTrack(mAudioOutputFormat)?:-1
                                    mCountDownLatch.countDown()

                                }else if (outputIndex > -1){
                                    val buffer = getOutputBuffer(outputIndex)
                                    mMediaMuxer?.writeSampleData(mAudioTrackIndex,buffer!!,bufferInfo)
                                    releaseOutputBuffer(outputIndex,false)
                                }

                                Log.e("AudioCodec","inputIndex:${inputIndex},outputIndex:${outputIndex}")

                            }
                            stop()
                            release()

                        }catch (e:Exception){
                            e.printStackTrace()
                        }
                    }
                }
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }

    private fun initAudioFormat(){
        mAudioOutputFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128 * 100)
        mAudioOutputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        mAudioOutputFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16*1024)
    }

    private fun initAudio(){
        val bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,channelConfig,audioFormat)
        try {
            mAudio =  AudioRecord.Builder()
                    .setAudioSource(audioSource)
                    .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRateInHz)
                        .setChannelMask(channelConfig)
                        .build()
                    ).setBufferSizeInBytes(bufferSizeInBytes).build()
            mAudio!!.startRecording()
        }catch (e:SecurityException){
            e.printStackTrace()
        }catch (e:IllegalArgumentException){
            e.printStackTrace()
        }
    }

    private fun initVideoCodec(){
        if (mVideoCodec != null)return
        try {
            mVideoCodec = MediaCodec.createEncoderByType(MIMETYPE).apply {
                reset()

                setCallback(object : MediaCodec.Callback() {
                    override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {

                        try {
                            mCodeLock.lock()

                            if (mVideoCodec != null){
                                val bytes = YUVQueue.take()
                                val bufferInfo = codec.getInputBuffer(index)
                                bufferInfo!!.put(bytes)
                                codec.queueInputBuffer(index,0,bytes.size,calPts(),0)
                            }

                        }catch (_:InterruptedException){

                        }finally {
                            mCodeLock.unlock()
                        }
                    }
                    override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                        try {
                            mCodeLock.lock()

                            if (mVideoCodec != null){
                                val mediaFormat = codec.getOutputFormat(index)

                                codec.getOutputBuffer(index)?.apply {
                                    val offset = info.offset
                                    val size = info.size
                                    val presentTimes = info.presentationTimeUs

                                    //Log.e("onOutputBufferAvailable","offset:${offset},size:${size},presentTimes:${presentTimes}")

                                    mMediaMuxer?.writeSampleData(mVideoTrackIndex,this,info)
                                }

                                codec.releaseOutputBuffer(index,false)
                            }else  {
                                info.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                mMediaMuxer?.writeSampleData(mVideoTrackIndex, ByteBuffer.allocate(8),info)
                            }

                        }finally {
                            mCodeLock.unlock()
                        }
                    }

                    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                        Utils.logInfo("CodecException:$e")
                    }

                    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                        mVideoOutputFormat = format
                        mVideoTrackIndex = mMediaMuxer?.addTrack(mVideoOutputFormat)?:-1
                        mCountDownLatch.countDown()
                        Logger.d(Thread.currentThread().name)
                        startMuxer()
                    }
                },Handler(mCodeThread!!.looper))

                initMediaFormat()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
                    configure(mVideoOutputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                }else {
                    configure(mVideoOutputFormat, null, MediaCodec.CONFIGURE_FLAG_ENCODE, null)
                }
                mVideoOutputFormat = outputFormat
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }
    private fun calPts():Long{
        val past = if (mCurPts == 0L){
            mCurPts = System.nanoTime()
            0L
        }else{
            val p = mCurPts
            mCurPts = System.nanoTime()
            (mCurPts - p) / 1000
        }
        val per = 1 * 1000 * 1000 / FRAME_RATE
        mBaseTime += per
        return mBaseTime
    }

    private val mImageReaderYUVCallback = ImageReader.OnImageAvailableListener {
        try {
            mImageReaderLock.lock()

            it.acquireLatestImage()?.let {image->
                if (image.format == ImageFormat.YUV_420_888){
                    val w = image.width
                    val h = image.height

                    val  planes = image.planes
                    val yPlane = planes[0]
                    val uPlane = planes[1]
                    val vPlane = planes[2]

                    val yBuffer = yPlane.buffer
                    val uBuffer = uPlane.buffer
                    val vBuffer = vPlane.buffer

                    mYuvBuffer.rewind()
                    mYuvBuffer.put(yBuffer)
                    var index = 0
                    val pos = vBuffer.limit()
                    val pixelStride = vPlane.pixelStride
                    while (index < pos - 1){
                        mYuvBuffer.put(uBuffer.get(index))
                        mYuvBuffer.put(vBuffer.get(index))
                        index += pixelStride
                    }

                    val bytes = ByteArray(w * h * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8)
                    if (VideoCameraManager.getInstance().isBack())
                        YUVUtils.rotateYUV_420_90(mYuvBuffer.array(),w,h,bytes)
                    else {
                        YUVUtils.rotateYUV_420_270(mYuvBuffer.array(),w,h,bytes)
                    }

                    try {
                        YUVQueue.put(bytes)
                    }catch (_:InterruptedException){
                        YUVQueue.clear()
                    }

/*                    Log.e("",String.format(
                        Locale.CHINA,"yuvWidth:%d,yuvHeight:%d,YpixelStride:%d,YrowStride:%d,VpixelStride:%d,VrowStride:%d,UpixelStride:%d,UrowStride:%d",
                        it.width,it.height,yPlane.pixelStride,yPlane.rowStride,vPlane.pixelStride,vPlane.rowStride,uPlane.pixelStride,uPlane.rowStride))*/

                }
                image.close()
            }

        }finally {
            mImageReaderLock.unlock()
        }
    }

    private fun initMediaFormat(){
        mVideoOutputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities. COLOR_FormatYUV420Flexible)
        mVideoOutputFormat.setInteger(MediaFormat.KEY_FRAME_RATE,FRAME_RATE)
        mVideoOutputFormat.setInteger(MediaFormat.KEY_BIT_RATE, WIDTH * HEIGHT * 4)
        mVideoOutputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1)
        /*
        * upon four parameters must be set,otherwise will cause "configure failed with err 0x80001001"
        * */
        mVideoOutputFormat.setInteger(MediaFormat.KEY_LEVEL,MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)

        val orientation = VideoCameraManager.getInstance().getOrientation()
        if (orientation == 90 || orientation == 270){
            mVideoOutputFormat.setInteger(MediaFormat.KEY_WIDTH, HEIGHT)
            mVideoOutputFormat.setInteger(MediaFormat.KEY_HEIGHT, WIDTH)
        }
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
        startImageReaderThread()
        startCodeThread()

        initMediaMuxer()

        initAudioCodec()

        YUVQueue.clear()
        try {
            initVideoCodec()

            mVideoCodec!!.start()


        }catch (e:Exception){
            e.printStackTrace()
        }

        mImageReaderYUV!!.setOnImageAvailableListener(mImageReaderYUVCallback,Handler(mImageReaderThread!!.looper))

        //mCountDownLatch.await()

        Logger.d(Thread.currentThread().name)

        mCountDownLatch.await()
       //
    }

    override fun stop() {
        if (mVideoCodec != null){
            try {
                mCodeThread?.interrupt()

                mCodeLock.lock()

                try {
                    mVideoCodec!!.stop()
                }catch (e:IllegalStateException){
                    e.printStackTrace()
                }
                mVideoCodec!!.release()
                mVideoCodec = null

                mCurPts = 0L
                mBaseTime = 0L


                stopMuxer()
            }finally {
                mCodeLock.unlock()
            }
        }

        stopAudioRecord()

        stopImageReaderThread()
        stopCodeThread()
    }

    private fun initMediaMuxer(){
        mMediaMuxer = MediaMuxer(getFile().absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }
    private fun startMuxer(){
        mMediaMuxer?.start()
    }

    private fun stopMuxer(){
        if (mMediaMuxer != null){
            mMediaMuxer!!.stop()
            mMediaMuxer!!.release()
            mMediaMuxer = null
        }
    }

    private fun stopAudioRecord(){
        mStopAudio = true

        mAudioCodec?.apply {
            stop()
            release()
            mAudioCodec = null
        }

        mAudio?.apply {
            stop()
            release()
            mAudio = null
        }

    }

    override fun release() {
        if (mImageReaderYUV != null){
            try {
                mImageReaderThread?.interrupt()
                mImageReaderLock.lock()
                mImageReaderYUV!!.close()
                mImageReaderYUV = null
            }finally {
                mImageReaderLock.unlock()
            }
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
        const val AudioMIMETYPE = MediaFormat.MIMETYPE_AUDIO_AAC
        const val WIDTH = 1280
        const val HEIGHT = 720
        const val FRAME_RATE = 30
    }
}