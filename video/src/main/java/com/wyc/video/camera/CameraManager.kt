package com.wyc.video.camera

import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.CameraDevice.StateCallback.*
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.CamcorderProfile
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.view.Surface
import com.wyc.logger.Logger
import com.wyc.video.Utils
import com.wyc.video.VideoApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.video.camera
 * @ClassName:      CameraManager
 * @Description:    相机管理
 * @Author:         wyc
 * @CreateDate:     2022/6/1 10:11
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/6/1 10:11
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class CameraManager : CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private var mCameraFaceId:String = ""
    private var mCameraBackId:String = ""
    private var mCameraDevice:CameraDevice? = null
    private var mCameraCaptureSession:CameraCaptureSession? = null
    private var mBackgroundHandler:Handler? = null
    private var mBackgroundThread:HandlerThread? = null
    private var hasBack = true
    private var mPreviewSurface:Surface? = null
    private var mPreviewCaptureRequestBuilder:CaptureRequest.Builder? = null
    private var mExecutor: ExecutorService? = null
    private var mAspectRatio = 0.75f
    private var mSensorOrientation = 90
    private var mImageReader : ImageReader? = null
    private var mPicCallback:OnPicture? = null
    private var mMediaRecorder:MediaRecorder? = null
    private var mRecordSurface:Surface? = null

    init {
        initCamera()
    }

    private fun initCamera(){
        val cManager = VideoApp.getInstance().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val camIdList = cManager.cameraIdList
        if (camIdList.isEmpty()){
            Utils.logInfo("not find any camera with this device.")
            return
        }
        try {
            if (camIdList.size >=2 ){
                mCameraFaceId = camIdList[1]
                mCameraBackId = camIdList[0]
            }

            val characteristic = cManager.getCameraCharacteristics(getValidCameraId())

            val activeRect = characteristic.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            val pixelRect = characteristic.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
            val physicalRect = characteristic.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            val afRegion = characteristic.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)
            val fpsRegion = characteristic.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)

            mSensorOrientation = characteristic.get(CameraCharacteristics.SENSOR_ORIENTATION)?:0

            Utils.logInfo("sensorOrientation:$mSensorOrientation,physicalRect:$physicalRect,pixelRect:$pixelRect" +
                    ",activeRect:$activeRect,afRegionCount:$afRegion,fpsRegion:" + Arrays.toString(fpsRegion))

            activeRect?.apply {
                mAspectRatio = height() * 1f / width() * 1f
                Logger.d("aspectRatio:%f",mAspectRatio)
            }

/*            camIdList.forEach {
                val characteristics = cManager.getCameraCharacteristics(it)
                val value = characteristics.get(LENS_FACING)
                if (value == CameraMetadata.LENS_FACING_FRONT){
                    mCameraFaceId = it
                }else if (value == CameraMetadata.LENS_FACING_BACK){
                    mCameraBackId = it
                }
            }*/
        }catch (e:CameraAccessException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }catch (e: IllegalArgumentException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }
    }

    private fun startBackgroundThread(){
        Utils.logInfo("start backgroundThread.")
        stopBackgroundThread()
        mBackgroundThread = HandlerThread("cameraBackgroundThread")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
        mExecutor = Executors.newFixedThreadPool(2)
    }
    private fun stopBackgroundThread() {
        Utils.logInfo("stop backgroundThread.")
        if (mBackgroundThread != null) {
            mBackgroundThread!!.quitSafely()
            try {
                mBackgroundThread!!.join()
                mBackgroundThread = null
                mBackgroundHandler = null
            } catch (e: InterruptedException) {
                Utils.logInfo("stop background thread error:$e")
            }
        }
        if (mExecutor != null) {
            mExecutor!!.shutdown()
            mExecutor = null
        }
    }

    fun openCamera(surface:Surface?){

        if (surface == null){
            return
        }

        if (mCameraBackId.isBlank() && mCameraFaceId.isBlank()){
            Utils.logInfo("backId and faceId both are empty.")
            return
        }

        releaseResource()
        startBackgroundThread()

        mPreviewSurface = surface
        mImageReader = ImageReader.newInstance(4000,3000,ImageFormat.JPEG,1)
        mImageReader!!.setOnImageAvailableListener(mImageAvailableListener,mBackgroundHandler)

        val cManager = VideoApp.getInstance().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cManager.openCamera(getValidCameraId(),mStateCallback,mBackgroundHandler)
        }catch (e:SecurityException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }catch (e: CameraAccessException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }catch (e:IllegalArgumentException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }
    }

    fun calPreViewAspectRatio():Float{
        return  mAspectRatio
    }
    private val mStateCallback = object  : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            mCameraDevice = camera
            createCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            mCameraDevice?.apply {
                close()
                mCameraDevice = null
            }
            Utils.showToast("camera has disconnected.")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            mCameraDevice?.apply {
                close()
                mCameraDevice = null
            }
            Utils.showToast("camera open error:" + getOpenErrorMsg(error))
        }
    }

    private fun createCaptureSession(){
        if (mCameraDevice == null){
            return
        }
        if (null != mCameraCaptureSession) {
            Log.e(this::class.simpleName, "createCameraPreviewSession: mCameraCaptureSession is already started")
            return
        }

        initMediaRecorder()

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P){
                mCameraDevice!!.createCaptureSession(listOf(mPreviewSurface,mImageReader!!.surface,mRecordSurface),mCaptureSessionCallback,mBackgroundHandler)
            }else {
                val config = SessionConfiguration(SessionConfiguration.SESSION_REGULAR,listOf(OutputConfiguration(mPreviewSurface!!),
                    OutputConfiguration(mImageReader!!.surface), OutputConfiguration(mRecordSurface!!)),mExecutor!!,mCaptureSessionCallback)

                mCameraDevice!!.createCaptureSession(config)
            }
        }catch (e: CameraAccessException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }catch (e:IllegalArgumentException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }
    }

    private val mCaptureSessionCallback = object : CameraCaptureSession.StateCallback(){
        override fun onConfigured(session: CameraCaptureSession) {
            if (mCameraDevice == null){
                return
            }
            mCameraCaptureSession = session
            createPreviewRequest()
        }
        override fun onConfigureFailed(session: CameraCaptureSession) {
            Utils.showToast("CaptureSession configure failure.")
        }

        override fun onClosed(session: CameraCaptureSession) {
            stopBackgroundThread()
        }
    }
    private fun createPreviewRequest(){
        try {
            if (mCameraCaptureSession != null){
                if (mPreviewCaptureRequestBuilder != null){
                    mCameraCaptureSession!!.setRepeatingRequest(mPreviewCaptureRequestBuilder!!.build(),null,null)
                }else{
                    mPreviewCaptureRequestBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                        addTarget(mPreviewSurface!!)

                        set(CaptureRequest.CONTROL_AF_MODE,CameraMetadata.CONTROL_AF_MODE_AUTO)
                        set(CaptureRequest.CONTROL_AF_TRIGGER,CameraMetadata.CONTROL_AF_TRIGGER_START)
                        set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)

                        mCameraCaptureSession!!.setRepeatingRequest(build(),null,null)
                    }
                }
            }
        }catch (e:IllegalArgumentException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }catch (e:CameraAccessException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }
    }

    private val mImageAvailableListener = ImageReader.OnImageAvailableListener {
        Utils.logInfo("width:${it.width},height:${it.height},format:${it.imageFormat}")

        val image = it.acquireLatestImage()
        val byteBuffer = image.planes[0].buffer
        val bmpBuffer  = ByteArray(byteBuffer.remaining())
        byteBuffer.get(bmpBuffer)

        Utils.logInfo("width:${it.width},height:${it.height},format:${it.imageFormat},size:${bmpBuffer.size}")

        image.close()

        try {
            val file = getPicFile();
            FileOutputStream(file).use { fileOutputStream->
                var bmp = BitmapFactory.decodeByteArray(bmpBuffer,0,bmpBuffer.size)
                if (hasBack){
                    bmp.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream)
                }else{
                    val matrix = Matrix()
                    matrix.setScale(1f,-1f)
                    bmp = Bitmap.createBitmap(bmp,0,0,bmp.width,bmp.height,matrix,false)
                    bmp.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream)
                }
                bmp.recycle()
            }
            mPicCallback?.onTaken(file)
        }catch (e:IOException){
            Utils.showToast("save file error:" + e.message)
        }
    }
    private fun getPicFile(): File {
        val name = String.format(Locale.CHINA, "%s%s%s.jpg", getPicDir().absolutePath, File.separator,SimpleDateFormat("yyyyMMddHHmmssSS", Locale.CHINA).format(Date()))
        return File(name)
    }
    fun getPicDir():File{
        return File(VideoApp.getVideoDir() + File.separator +"pic").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    interface OnPicture{
        fun onTaken(file:File)
    }
    fun setPicCallback(callback:OnPicture?){
        mPicCallback = callback
    }

    fun tackPic(){
        if (mCameraDevice == null){
            return
        }

        if (null == mCameraCaptureSession) {
            return
        }

        try {
            val picCaptureRequestBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(mImageReader!!.surface)
                set(CaptureRequest.JPEG_ORIENTATION,mSensorOrientation)
                set(CaptureRequest.CONTROL_AF_MODE,CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
            }
            mCameraCaptureSession!!.capture(picCaptureRequestBuilder.build(),null,null)
        }catch (e:IllegalArgumentException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }catch (e:CameraAccessException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }catch (e:IllegalStateException ){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }
    }

    fun recodeVideo(){
        launch {
            try {
                privateRecordVideo()
            }catch (e:IllegalArgumentException){
                Utils.logInfo(e.message)
            }
        }
    }
    private fun initMediaRecorder(){
        if (mMediaRecorder == null){
            mRecordSurface = MediaCodec.createPersistentInputSurface()
            mMediaRecorder =
                    /*if (Build.VERSION.SDK_INT >  Build.VERSION_CODES.R) {
                MediaRecorder(VideoApp.getInstance())
            } else */MediaRecorder()
        }else{
            mMediaRecorder!!.reset()
        }
        mMediaRecorder!!.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)

            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                setOutputFile(getVideoFile())
            }else{
                setOutputFile(getVideoFile().absolutePath)
            }

            /*if (Build.VERSION.SDK_INT >  Build.VERSION_CODES.R){
                val encoderProfiles = CamcorderProfile.getAll(getValidCameraId(),CamcorderProfile.QUALITY_720P)
                encoderProfiles?.apply {
                    val videos = videoProfiles
                    if (videos.isNotEmpty()){
                        val  videoProfile = videos[0]
                        setVideoSize(videoProfile.width,videoProfile.height)
                        setVideoFrameRate(videoProfile.frameRate)
                        setVideoEncodingBitRate(videoProfile.bitrate)
                    }

                    val audios =  audioProfiles
                    if (audios.isNotEmpty()){
                        val audio = audios[0]
                        setAudioChannels(audio.channels)
                        setAudioSamplingRate(audio.sampleRate)
                        setAudioEncodingBitRate(audio.bitrate)
                    }
                }
            }else*/

            val  camcorderProfile =  CamcorderProfile.get(getValidCameraId().toInt(),CamcorderProfile.QUALITY_720P)

            Logger.d("camcorderProfile:$camcorderProfile")

            setVideoSize(camcorderProfile.videoFrameWidth,camcorderProfile.videoFrameHeight)
            setVideoFrameRate(camcorderProfile.videoFrameRate)
            setVideoEncodingBitRate(camcorderProfile.videoBitRate)

            setAudioChannels(camcorderProfile.audioChannels)
            setAudioSamplingRate(camcorderProfile.audioSampleRate)
            setAudioEncodingBitRate(camcorderProfile.audioBitRate)

            setOrientationHint(90);

            setInputSurface(mRecordSurface!!)

            setOnErrorListener { _, what, _ -> Utils.logInfo("mediaRecorder error:$what") }

            prepare()
        }
    }
    private fun startRecordRequest(){
        try {
            val picCaptureRequestBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(mPreviewSurface!!)
                addTarget(mRecordSurface!!)
                set(CaptureRequest.CONTROL_AF_MODE,CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            }
            mCameraCaptureSession!!.setRepeatingRequest(picCaptureRequestBuilder.build(),null,null)
        }catch (e:IllegalArgumentException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }catch (e:CameraAccessException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }catch (e:IllegalStateException ){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }
    }
    private fun privateRecordVideo(){
        try {
            mMediaRecorder?.apply {
                startRecordRequest()
                start()
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
    }
    fun stopRecord(){
        launch {
            try {
                mMediaRecorder!!.stop()
                createPreviewRequest()
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }

    private fun getVideoFile(): File {
        val file = File(VideoApp.getVideoDir() + File.separator +"video")
        if (!file.exists()) {
            file.mkdirs()
        }
        val name = String.format(Locale.CHINA, "%s%s%s.mp4", file.absolutePath, File.separator,SimpleDateFormat("yyyyMMddHHmmssSS", Locale.CHINA).format(Date()))
        return File(name)
    }


    private fun getOpenErrorMsg(code:Int):String{
        return when(code){
            ERROR_CAMERA_IN_USE->{
                "camera device is in use already."
            }
            ERROR_MAX_CAMERAS_IN_USE ->{
                "too many other open camera devices."
            }
            ERROR_CAMERA_DISABLED ->{
                "camera device could not be opened."
            }
            ERROR_CAMERA_DEVICE ->{
                "camera device has encountered a fatal error."
            }
            ERROR_CAMERA_SERVICE ->{
                "camera service has encountered a fatal error."
            }
            else -> {
                "other error:$code"
            }
        }
    }

    private fun getValidCameraId():String{
        return if (hasBack){
            mCameraBackId
        }else mCameraFaceId
    }

    fun releaseResource(){
        Utils.logInfo("start release resource.")
        releaseRecorder()
        releaseCamera()
    }
    private fun releaseCamera(){
        if (mPreviewCaptureRequestBuilder != null && mPreviewSurface != null){
            mPreviewCaptureRequestBuilder!!.removeTarget(mPreviewSurface!!)
            mPreviewCaptureRequestBuilder = null
        }

        if (mCameraCaptureSession != null){
            mCameraCaptureSession!!.close()
            mCameraCaptureSession = null
        }

        if (mCameraDevice != null){
            mCameraDevice!!.close()
            mCameraDevice = null
        }

        if (mImageReader != null){
            mImageReader!!.close()
            mImageReader = null
        }
    }

    private fun releaseRecorder(){
        try {
            if (mMediaRecorder != null){
                mMediaRecorder!!.release()
                mMediaRecorder = null
            }
            if (mRecordSurface != null){
                mRecordSurface!!.release()
                mRecordSurface = null
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    /*
    * w,h are preview's size
    * */
    fun updateFocusRegion(w: Int, h: Int, l: Int, t: Int, r: Int, b: Int){
        if (mCameraCaptureSession != null && mPreviewCaptureRequestBuilder != null){
            mPreviewCaptureRequestBuilder!!.get(CaptureRequest.SCALER_CROP_REGION)?.apply {

                val wRatio = width() * 1f / h * 1f
                val hRatio = height() * 1f / w * 1f

                val left = if (l < 0) 0 else (l * wRatio).toInt()
                val right = if (r < 0) 0 else (r * wRatio).toInt()
                val top = if (t < 0) 0 else (t * hRatio).toInt()
                val bottom = if (b < 0) 0 else (b * hRatio).toInt()

                val focusRect = Rect(left,top,right,bottom)
                if (mSensorOrientation == 90 || mSensorOrientation == 270){
                    focusRect.set(top,left,bottom,right)
                }
                Utils.logInfo("focusRect:$focusRect,cropRegion:$this")

                mPreviewCaptureRequestBuilder!!.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(MeteringRectangle(focusRect, 1000)))
                mCameraCaptureSession!!.setRepeatingRequest(mPreviewCaptureRequestBuilder!!.build(),null,null)
            }
        }
    }
    fun switchCamera(){
        if (mPreviewSurface != null){
            hasBack = !hasBack
            openCamera(mPreviewSurface!!)
        }
    }
}