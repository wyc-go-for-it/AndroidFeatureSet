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
import com.wyc.video.camera.VideoCameraManager.Companion.getInstance
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.LockSupport
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min


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
    private val WIDTH = getInstance().vWidth
    private val HEIGHT = getInstance().vHeight
    private val FRAME_RATE = getInstance().mBestFPSRange.upper

    private var mImageReaderYUV : ImageReader? = null
    private var mImageReaderThread:HandlerThread? = null
    private var mImageReaderHandler:Handler? = null

    @Volatile
    private var mVideoCodec: MediaCodec? = null
    private var mCodecThread:HandlerThread? = null
    private var mCodecHandler:Handler? = null

    private var isFirstVideoFrame = true
    private var isFirstAudioFrame = true
    private var mMediaMuxer:MediaMuxer? = null
    @Volatile
    private var mVideoTrackIndex = -1
    @Volatile
    private var mVideoFrameTime = -1L

    private val audioSource = MediaRecorder.AudioSource.MIC
    private val sampleRateInHz = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val channels = 1
    @Volatile
    private var mAudioFrameTime = -1L

    @Volatile
    private var mStopAudio = true
    @Volatile
    private var mAudioTrackIndex = -1

    private val  YUVQueue: ArrayBlockingQueue<ByteArray>  = ArrayBlockingQueue<ByteArray> (10)
    private val mYuvBuffer: ByteBuffer = ByteBuffer.allocate(WIDTH * HEIGHT * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8)

    override fun configure() {
        mImageReaderYUV = ImageReader.newInstance(WIDTH,HEIGHT,ImageFormat.YUV_420_888,2)
    }

    private fun initAudioCodec(){
        if (mStopAudio){
            try {
                 MediaCodec.createEncoderByType(AudioMIMETYPE).apply {
                    val format = createAudioFormat()
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
                        configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                    }else {
                        configure(format, null, MediaCodec.CONFIGURE_FLAG_ENCODE, null)
                    }

                    thread {
                        val  audio = createAudio()
                        if (audio != null){

                            try {
                                start()

                                mStopAudio = false

                                val audioData = ByteArray(2048)
                                val bufferInfo = MediaCodec.BufferInfo()

                                while (!mStopAudio){
                                    val len = audio.read(audioData,0,2048)
                                    if (len > 0){
                                        val inputIndex = dequeueInputBuffer(1000 * 100)
                                        if (inputIndex > -1){
                                            getInputBuffer(inputIndex)?.apply {
                                                clear()
                                                put(audioData)
                                            }
                                            queueInputBuffer(inputIndex,0,len,calAudioPts(len),0)
                                        }

                                        val  outputIndex = dequeueOutputBuffer(bufferInfo,1000 * 100)
                                        if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                                            addTrackAudio(outputFormat)
                                        }else if (outputIndex > -1){
                                            getOutputBuffer(outputIndex)?.apply {
                                                writeDataAudio(this,bufferInfo)
                                            }
                                            releaseOutputBuffer(outputIndex,false)
                                        }
                                    }
                                }
                                val inputIndex = dequeueInputBuffer(1000 * 100)
                                if (inputIndex > -1){
                                    queueInputBuffer(inputIndex,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                }
                            }catch (e:Exception){
                                e.printStackTrace()
                            }finally {
                                audio.stop()
                                audio.release()

                                stop()
                                release()
                                Log.e("audio:","release")
                            }
                        }
                    }
                }
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }

    private fun calAudioPts(size: Int): Long {
        val perSampleByte = 1024 * 2L
        return if (mAudioFrameTime == -1L){
            if (hasTrackFinish())
                mAudioFrameTime = 1000 * 1000 *  1024/ 44100   * size / perSampleByte
            0L
        }else{
            val pts = mAudioFrameTime
            mAudioFrameTime += 1000 * 1000 *  1024/ 44100  * size / perSampleByte
            pts
        }
    }

    private fun createAudioFormat():MediaFormat{
        val audioOutputFormat:MediaFormat = MediaFormat.createAudioFormat(AudioMIMETYPE, sampleRateInHz, channels)
        audioOutputFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128 * 1000)
        audioOutputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        audioOutputFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16*1024)
        return audioOutputFormat
    }

    private fun createAudio():AudioRecord?{
        val bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,channelConfig,audioFormat)
        try {
            return AudioRecord.Builder()
                    .setAudioSource(audioSource)
                    .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRateInHz)
                        .setChannelMask(channelConfig)
                        .build()
                    ).setBufferSizeInBytes(bufferSizeInBytes).build().apply {
                    startRecording()
                }
        }catch (e:SecurityException){
            e.printStackTrace()
        }catch (e:IllegalArgumentException){
            e.printStackTrace()
        }
        return null;
    }

    private fun initVideoCodec(){
        if (mVideoCodec != null)return
        try {
            mVideoCodec = MediaCodec.createEncoderByType(MIMETYPE).apply {
                reset()

                setCallback(object : MediaCodec.Callback() {
                    override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                        try {
                            if (mVideoCodec != null){
                                YUVQueue.poll(100,TimeUnit.MILLISECONDS)?.apply {
                                    codec.getInputBuffer(index)?.put(this)
                                    if (isFirstVideoFrame){
                                        if (hasTrackFinish())isFirstVideoFrame = false
                                        codec.queueInputBuffer(index,0,size,calVideoPts(),MediaCodec.BUFFER_FLAG_KEY_FRAME)
                                    }else
                                        codec.queueInputBuffer(index,0,size,calVideoPts(),0)
                                }
                            }
                        }catch (_:InterruptedException){
                        }
                    }
                    override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                        if (mVideoCodec != null){
                            val mediaFormat = codec.getOutputFormat(index)

                            codec.getOutputBuffer(index)?.apply {
                                val offset = info.offset
                                val size = info.size
                                val presentTimes = info.presentationTimeUs
                                writeDataVideo(this,info)
                            }

                            codec.releaseOutputBuffer(index,false)
                        }
                    }

                    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                        Utils.logInfo("CodecException:$e")
                    }

                    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                        addTrackVideo(format)
                    }
                }, mCodecHandler)

                val format = createMediaFormat()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
                    configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                }else {
                    configure(format, null, MediaCodec.CONFIGURE_FLAG_ENCODE, null)
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }
    private fun calVideoPts():Long{
        return if (mVideoFrameTime == -1L){
            if (hasTrackFinish())
                mVideoFrameTime = (1000 * 1000L / FRAME_RATE)
            0L
        }else{
            val pts = mVideoFrameTime
            mVideoFrameTime += (1 * 1000 * 1000 / FRAME_RATE)
            pts
        }
    }

    private val mImageReaderYUVCallback = ImageReader.OnImageAvailableListener {
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
                if (getInstance().isBack())
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
    }

    private fun createMediaFormat():MediaFormat{
        val videoOutputFormat:MediaFormat = MediaFormat.createVideoFormat(MIMETYPE, WIDTH, HEIGHT)
        videoOutputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities. COLOR_FormatYUV420Flexible)
        videoOutputFormat.setInteger(MediaFormat.KEY_FRAME_RATE,FRAME_RATE)
        videoOutputFormat.setInteger(MediaFormat.KEY_BIT_RATE, WIDTH * HEIGHT * 4)
        videoOutputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,5)
        /*
        * upon four parameters must be set,otherwise will cause "configure failed with err 0x80001001"
        * */
        videoOutputFormat.setInteger(MediaFormat.KEY_LEVEL,MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)

        val orientation = VideoCameraManager.getInstance().getOrientation()
        if (orientation == 90 || orientation == 270){
            videoOutputFormat.setInteger(MediaFormat.KEY_WIDTH, HEIGHT)
            videoOutputFormat.setInteger(MediaFormat.KEY_HEIGHT, WIDTH)
        }
        return videoOutputFormat
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

        initMediaMuxer()

        YUVQueue.clear()

        initVideoCodec()
        initAudioCodec()

        try {
            mVideoCodec!!.start()
        }catch (e:Exception){
            e.printStackTrace()
        }

        startImageReaderThread()
        mImageReaderYUV!!.setOnImageAvailableListener(mImageReaderYUVCallback,mImageReaderHandler)

    }

    override fun stop() {
        stopMuxer()

        stopAudioRecord()

        stopCodec()

        mImageReaderYUV?.setOnImageAvailableListener(null,null)
        stopImageReaderThread()
    }
    private fun stopCodec(){
        if (mCodecHandler != null && mCodecThread != null){
            mCodecHandler!!.post {
                if (mVideoCodec != null){
                    try {
                        mVideoCodec!!.stop()
                    }catch (e:IllegalStateException){
                        e.printStackTrace()
                    }
                    mVideoCodec!!.release()
                    mVideoCodec = null
                    Log.e("video:","release")
                }
            }
        }
        stopCodecThread()
    }

    private fun initMediaMuxer(){
        stopMuxer()
        mMediaMuxer = MediaMuxer(getFile().absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    private fun addTrackVideo(mf:MediaFormat) {
        mMediaMuxer?.apply {
            mVideoTrackIndex = addTrack(mf)
            Logger.d("VideoTrackIndex:%d",mVideoTrackIndex)
            if (mVideoTrackIndex != -1 && mAudioTrackIndex != -1){
                start()
            }
        }
    }
    private fun addTrackAudio(mf: MediaFormat){
        mMediaMuxer?.apply {
            mAudioTrackIndex = addTrack(mf)
            Logger.d("AudioTrackIndex:%d",mAudioTrackIndex)
            if (mVideoTrackIndex != -1 && mAudioTrackIndex != -1){
                start()
            }
        }
    }

    private fun writeDataVideo(byteBuf:ByteBuffer ,bufferInfo: MediaCodec.BufferInfo){
        if (readyMuxer()){
            Log.e("videoData:",String.format("VideoTrackIndex:%d,presentationTimeUs:%d,key_frame:%d",mVideoTrackIndex,bufferInfo.presentationTimeUs,bufferInfo.flags))
            mMediaMuxer!!.writeSampleData(mVideoTrackIndex,byteBuf,bufferInfo)
        }
    }

    private fun writeDataAudio(byteBuf:ByteBuffer ,bufferInfo: MediaCodec.BufferInfo){
        if (readyMuxer()){
            if (isFirstAudioFrame){
                bufferInfo.presentationTimeUs = 0
                isFirstAudioFrame = false
            }
            Log.e("audioData:",String.format("AudioTrackIndex:%d,presentationTimeUs:%d",mAudioTrackIndex,bufferInfo.presentationTimeUs))
            mMediaMuxer!!.writeSampleData(mAudioTrackIndex,byteBuf,bufferInfo)
        }
    }
    private fun readyMuxer():Boolean{
        return hasTrackFinish() && mVideoFrameTime != -1L && mAudioFrameTime != -1L && mMediaMuxer != null
    }
    private fun hasTrackFinish():Boolean{
        return mVideoTrackIndex != -1 && mAudioTrackIndex != -1
    }


    private fun stopMuxer(){
        if (mMediaMuxer != null){
            try {
                writeLastFrame()

                mMediaMuxer!!.stop()
                mMediaMuxer!!.release()
                mMediaMuxer = null
            }catch (e:IllegalStateException ){
                e.printStackTrace()
            }
        }
    }
    private fun writeLastFrame(){
        if (mVideoFrameTime != -1L && mAudioFrameTime != -1L){
            val info = MediaCodec.BufferInfo()
            Logger.d("videoBaseTime:%s,audioBaseTime:%s",mVideoFrameTime,mAudioFrameTime)
            info.set(0,0, max(mAudioFrameTime,mVideoFrameTime),MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            mMediaMuxer!!.writeSampleData(mVideoTrackIndex, ByteBuffer.allocate(0),info)
            mMediaMuxer!!.writeSampleData(mAudioTrackIndex, ByteBuffer.allocate(0),info)

            mVideoTrackIndex = -1
            mAudioTrackIndex = -1

            mVideoFrameTime = -1L
            mAudioFrameTime = -1L

            isFirstAudioFrame = true
            isFirstVideoFrame = true
        }
    }

    private fun stopAudioRecord(){
        mStopAudio = true
    }

    override fun release() {
        stopYUV()
        stop()
        super.release()
    }
    private fun stopYUV(){
        if (mImageReaderHandler != null && mImageReaderThread != null){
            mImageReaderHandler!!.post {
                if (mImageReaderYUV != null){
                    mImageReaderYUV!!.close()
                    mImageReaderYUV = null
                }
            }
            mImageReaderThread!!.interrupt()
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

    private fun startImageReaderThread(){
        stopImageReaderThread()
        mImageReaderThread = HandlerThread("ImageReaderThread")
        mImageReaderThread!!.start()
        mImageReaderHandler = Handler(mImageReaderThread!!.looper)
    }
    private fun stopImageReaderThread() {
        if (mImageReaderThread != null) {
            mImageReaderThread!!.quitSafely()
            try {
                mImageReaderThread!!.join()
                mImageReaderThread = null
                mImageReaderHandler = null
            } catch (e: InterruptedException) {
            }
        }
    }
    companion object{
        const val MIMETYPE = "video/avc"
        const val AudioMIMETYPE = MediaFormat.MIMETYPE_AUDIO_AAC
    }
}