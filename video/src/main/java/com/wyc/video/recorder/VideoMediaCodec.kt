package com.wyc.video.recorder

import android.graphics.ImageFormat
import android.media.*
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import com.wyc.video.Utils
import com.wyc.video.YUVUtils
import com.wyc.video.camera.VideoCameraManager
import com.wyc.video.camera.VideoCameraManager.Companion.getInstance
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
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
    private val WIDTH = getInstance().vWidth
    private val HEIGHT = getInstance().vHeight
    private val FRAME_RATE = getInstance().mBestFPSRange.upper

    private var mImageReaderYUV : ImageReader? = null
    private var mImageReaderThread:HandlerThread? = null
    private val mImageReaderLock = ReentrantLock()

    private var mVideoCodec: MediaCodec? = null
    private var mCodeThread:HandlerThread? = null
    private val mCodeLock = ReentrantLock()

    private var mMediaMuxer:MediaMuxer? = null
    private var mVideoTrackIndex = -1
    @Volatile
    private var mVideoBaseTime = 0L

    @Volatile
    private var mReadyMuxer = false

    private val audioSource = MediaRecorder.AudioSource.MIC
    private val sampleRateInHz = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val channels = 1
    @Volatile
    private var mAudioBaseTime = 0L

    @Volatile
    private var mStopAudio = true
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
                                                Log.e("AudioCodec","inputIndex:${inputIndex},outputIndex:${outputIndex},len:${len},size:${bufferInfo.size},pts:${bufferInfo.presentationTimeUs}")
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
        val perSampleByte = 1024 * 2
        return if (mAudioBaseTime == 0L){
            mAudioBaseTime += 1000 * 1000 *  1024/ 44100   * size / perSampleByte
            0L
        }else{
            val pts = mAudioBaseTime
            mAudioBaseTime += 1000 * 1000 *  1024/ 44100  * size / perSampleByte
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
                            mCodeLock.lock()

                            if (mVideoCodec != null){
                                val bytes = YUVQueue.take()
                                val bufferInfo = codec.getInputBuffer(index)
                                bufferInfo!!.put(bytes)
                                codec.queueInputBuffer(index,0,bytes.size,calVideoPts(),0)
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

                                    Log.e("onOutputBufferAvailable","offset:${offset},size:${size},presentTimes:${presentTimes}")

                                    writeDataVideo(this,info)
                                }

                                codec.releaseOutputBuffer(index,false)
                            }else  {
                                info.set(0,0,info.presentationTimeUs,MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                writeDataVideo(ByteBuffer.allocate(0),info)
                            }

                        }finally {
                            mCodeLock.unlock()
                        }
                    }

                    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                        Utils.logInfo("CodecException:$e")
                    }

                    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                        addTrackVideo(format)
                    }
                },Handler(mCodeThread!!.looper))

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
        return if (mVideoBaseTime == 0L){
            mVideoBaseTime += (1 * 1000 * 1000 / FRAME_RATE)
            0L
        }else{
            val pts = mVideoBaseTime
            mVideoBaseTime += (1 * 1000 * 1000 / FRAME_RATE)
            pts
        }
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
        startImageReaderThread()
        startCodeThread()

        initMediaMuxer()

        YUVQueue.clear()

        initAudioCodec()
        initVideoCodec()

        try {
            mVideoCodec!!.start()
        }catch (e:Exception){
            e.printStackTrace()
        }

        mImageReaderYUV!!.setOnImageAvailableListener(mImageReaderYUVCallback,Handler(mImageReaderThread!!.looper))

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

                mVideoBaseTime = 0L
            }finally {
                mCodeLock.unlock()
            }
        }

        stopAudioRecord()

        stopMuxer()

        stopImageReaderThread()
        stopCodeThread()
    }

    private fun initMediaMuxer(){
        stopMuxer()
        mMediaMuxer = MediaMuxer(getFile().absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    private fun addTrackVideo(mf:MediaFormat) {
        mMediaMuxer?.apply {
            synchronized(this@VideoMediaCodec){
                mVideoTrackIndex = addTrack(mf)
                if (mVideoTrackIndex != -1 && mAudioTrackIndex != -1){
                    start()
                    mReadyMuxer = true
                }
            }
        }
    }
    private fun addTrackAudio(mf: MediaFormat){
        mMediaMuxer?.apply {
            synchronized(this@VideoMediaCodec){
                mAudioTrackIndex = addTrack(mf)
                if (mVideoTrackIndex != -1 && mAudioTrackIndex != -1){
                    start()
                    mReadyMuxer = true
                }
            }
        }
    }

    private fun writeDataVideo(byteBuf:ByteBuffer ,bufferInfo: MediaCodec.BufferInfo){
        if (mReadyMuxer && mMediaMuxer != null){
            mMediaMuxer!!.writeSampleData(mVideoTrackIndex,byteBuf,bufferInfo)
        }
    }

    private fun writeDataAudio(byteBuf:ByteBuffer ,bufferInfo: MediaCodec.BufferInfo){
        if (mReadyMuxer && mMediaMuxer != null){
            mMediaMuxer!!.writeSampleData(mAudioTrackIndex,byteBuf,bufferInfo)
        }
    }

    private fun stopMuxer(){
        if (mMediaMuxer != null){
            try {
                mReadyMuxer = false
                mMediaMuxer!!.stop()
                mMediaMuxer!!.release()
                mMediaMuxer = null
            }catch (e:IllegalStateException ){
                e.printStackTrace()
            }
        }
    }

    private fun stopAudioRecord(){
        mStopAudio = true
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
    }
}