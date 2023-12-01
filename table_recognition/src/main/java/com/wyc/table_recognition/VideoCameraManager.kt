package com.wyc.table_recognition

import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.CameraDevice.StateCallback.*
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.CamcorderProfile
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.AccessController.getContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.min


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

internal class VideoCameraManager : CoroutineScope by CoroutineScope(Dispatchers.IO),ICamera {

    private var mCameraFaceId:String = ""
    private var mCameraBackId:String = ""
    private var mCameraDevice:CameraDevice? = null
    private var mCameraCaptureSession:CameraCaptureSession? = null
    private var mBackgroundHandler:Handler? = null
    private var mBackgroundThread:HandlerThread? = null
    private var hasBack = true
    private var mPreviewCaptureRequestBuilder:CaptureRequest.Builder? = null
    private var mExecutor: ExecutorService? = null
    private var mAspectRatio = 0.75f
    private var mPicImageReader : ImageReader? = null

    private var mPicCallback:OnPicture? = null
    private var mMode = MODE.PICTURE
    private val mPreviewList = mutableListOf<Surface>()

    private var mBestFPSRange: Range<Int> =  Range(30,30)

    private var vWidth = 1280
    private var vHeight = 720

    private var mScreenWidth = 0
    private var mPreviewWidth = 0
    private var mPreviewHeight = 0

    override fun getBastFPS(): Int {
        return mBestFPSRange.upper
    }

    init {
        initCamera()
    }

    companion object{
        private var sCameraManager:ICamera? = null
        @JvmStatic
        fun getInstance():ICamera{
            if (sCameraManager == null){
                synchronized(VideoCameraManager::class){
                    if (sCameraManager == null){
                        sCameraManager = VideoCameraManager()
                    }
                }
            }
            return sCameraManager!!
        }
        @JvmStatic
        fun clear(){
            if (sCameraManager != null){
                sCameraManager!!.clearCallback()
                sCameraManager!!.releaseResource()
                sCameraManager = null
            }
        }
       const val REQUEST_CAMERA_PERMISSIONS = 88
    }

    override fun getOrientation():Int{
        val cManager =  App.getInstance().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val characteristic = cManager.getCameraCharacteristics(getValidCameraId())
        return characteristic.get(CameraCharacteristics.SENSOR_ORIENTATION)?:0
    }

    private fun initCamera(){
        val cManager = App.getInstance().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val camIdList = cManager.cameraIdList
        if (camIdList.isEmpty()){
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

            val fpsAeRegion = characteristic.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            selectBaseFPS(fpsAeRegion)

            val capabilities = characteristic.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

            val configMap = characteristic.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            var  outputSizes = configMap?.getOutputSizes(ImageFormat.YUV_420_888)
            val  camcorderProfile =  CamcorderProfile.get(getValidCameraId().toInt(), CamcorderProfile.QUALITY_720P)
            if (outputSizes != null && outputSizes.isNotEmpty()){
                val s = outputSizes[0]
                vWidth = min(camcorderProfile.videoFrameWidth,s.width)
                vHeight = min(camcorderProfile.videoFrameHeight,s.height)
            }else{
                vWidth = camcorderProfile.videoFrameWidth
                vHeight = camcorderProfile.videoFrameHeight
                outputSizes = arrayOf(Size(0, 0))
            }
            val fpsRegion = configMap?.highSpeedVideoFpsRanges

            val code = CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)
            val sensorOrientation = characteristic.get(CameraCharacteristics.SENSOR_ORIENTATION)?:0
            var  outputSizesJpg = configMap?.getOutputSizes(ImageFormat.JPEG)

            activeRect?.apply {
                mAspectRatio = height() * 1f / width() * 1f
            }
            val metrics = DisplayMetrics()
            val windowManager = App.getInstance().getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getRealMetrics(metrics)
            mScreenWidth = metrics.widthPixels
        }catch (e:CameraAccessException){
            e.printStackTrace()
        }catch (e: IllegalArgumentException){
            e.printStackTrace()
        }
    }
    private fun selectBaseFPS(range: Array<Range<Int>>?){
        range?.apply {
            mBestFPSRange = this[this.size - 1]
        }
    }

    private fun startBackgroundThread(){
        stopBackgroundThread()
        mBackgroundThread = HandlerThread("cameraBackgroundThread")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
        mExecutor = Executors.newFixedThreadPool(2)
    }
    private fun stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread!!.quitSafely()
            try {
                mBackgroundThread!!.join()
                mBackgroundThread = null
                mBackgroundHandler = null
            } catch (e: InterruptedException) {
            }
        }
        if (mExecutor != null) {
            mExecutor!!.shutdown()
            mExecutor = null
        }
    }

    override fun openCamera(){

        if (mPreviewList.isEmpty()){
            return
        }

        if (mCameraBackId.isBlank() && mCameraFaceId.isBlank()){
            return
        }

        releaseResource()
        startBackgroundThread()

        val cManager = App.getInstance().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cManager.openCamera(getValidCameraId(),mStateCallback,mBackgroundHandler)
        }catch (e:SecurityException){
            e.printStackTrace()
        }catch (e: CameraAccessException){
            e.printStackTrace()
        }catch (e:IllegalArgumentException){
            e.printStackTrace()
        }
    }

    override fun calPreViewAspectRatio():Float{
        return  mAspectRatio
    }
    private val mStateCallback = object  : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            mCameraDevice = camera
            when(mMode){
                MODE.PICTURE ->{
                    createPhotographSession()
                }
                MODE.RECORD,MODE.SHORT_RECORD ->{
                    createRecordSession()
                }
            }
        }

        override fun onDisconnected(camera: CameraDevice) {
            mCameraDevice?.apply {
                close()
                mCameraDevice = null
            }
        }

        override fun onError(camera: CameraDevice, error: Int) {
            mCameraDevice?.apply {
                close()
                mCameraDevice = null
            }
        }
    }

    private fun createPhotographSession(){
        if (mCameraDevice == null){
            return
        }
        
        if (mPicImageReader == null){
            mPicImageReader = ImageReader.newInstance(mPreviewHeight,mPreviewWidth,ImageFormat.JPEG,1)
            mPicImageReader!!.setOnImageAvailableListener(mPicImageAvailableListener,mBackgroundHandler)
        }

        try {

            val listSurface = mutableListOf<Surface>()
            listSurface.addAll(mPreviewList)
            listSurface.add(mPicImageReader!!.surface)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P){
                mCameraDevice!!.createCaptureSession(listSurface,mCaptureSessionCallback,mBackgroundHandler)
            }else {
                val listConfig = mutableListOf<OutputConfiguration>()
                listSurface.forEach {
                    listConfig.add(OutputConfiguration(it))
                }
                val config = SessionConfiguration(SessionConfiguration.SESSION_REGULAR,listConfig,mExecutor!!,mCaptureSessionCallback)

                mCameraDevice!!.createCaptureSession(config)
            }
        }catch (e: CameraAccessException){
            e.printStackTrace()
        }catch (e:IllegalArgumentException){
            e.printStackTrace()
        }
    }

    private fun createRecordSession(){
        if (mCameraDevice == null){
            return
        }

        try {
            val listSurface = mutableListOf<Surface>()
            listSurface.addAll(mPreviewList)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P){
                mCameraDevice!!.createCaptureSession(listSurface,mCaptureSessionCallback,mBackgroundHandler)
            }else {
                val listConfig = mutableListOf<OutputConfiguration>()
                listSurface.forEach {
                    listConfig.add(OutputConfiguration(it))
                }
                val config = SessionConfiguration(SessionConfiguration.SESSION_REGULAR,listConfig,mExecutor!!,mCaptureSessionCallback)

                mCameraDevice!!.createCaptureSession(config)
            }
        }catch (e: CameraAccessException){
            e.printStackTrace()
        }catch (e:IllegalArgumentException){
            e.printStackTrace()
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
            Log.d("VideoCameraManager","CaptureSession configure failure.")
        }

        override fun onClosed(session: CameraCaptureSession) {
            stopBackgroundThread()
        }
    }
    private fun createPreviewRequest(){
        try {
            if (mCameraCaptureSession != null){
                if (mPreviewCaptureRequestBuilder != null){
                    mCameraCaptureSession!!.setRepeatingRequest(mPreviewCaptureRequestBuilder!!.build(),null,mBackgroundHandler)
                }else{
                    mPreviewCaptureRequestBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                        mPreviewList.forEach {
                            addTarget(it)
                        }

                        set(CaptureRequest.CONTROL_AF_MODE,CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                        set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                        set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, mBestFPSRange)

                        mCameraCaptureSession!!.setRepeatingRequest(build(),mCaptureCallback,mBackgroundHandler)
                    }
                }
            }
        }catch (e:IllegalArgumentException){
            Log.d("VideoCameraManager",e.message?:"")
        }catch (e:CameraAccessException){
            Log.d("VideoCameraManager",e.message?:"")
        }catch (e:IllegalStateException){
            Log.d("VideoCameraManager",e.message?:"")
        }
    }

    private val mPicImageAvailableListener = ImageReader.OnImageAvailableListener {
        Log.d("VideoCameraManager","width:${it.width},height:${it.height},format:${it.imageFormat}")
        val image = it.acquireLatestImage()
        val plane = image.planes[0]
        val byteBuffer = plane.buffer
        val bmpBuffer  = ByteArray(byteBuffer.remaining())
        byteBuffer.get(bmpBuffer)

        image.close()

        var bmp = BitmapFactory.decodeByteArray(bmpBuffer,0,bmpBuffer.size)
        if (mScreenWidth != bmp.width){
            val  ratio = mScreenWidth.toFloat() / mPreviewHeight.toFloat()
            bmp = Bitmap.createBitmap(bmp,0,0,(ratio * bmp.height).toInt(),bmp.height)
        }
        mPicCallback?.onTaken(bmp)
    }
    private fun getPicFile(): File {
        val name = String.format(Locale.CHINA, "%s%s%s.jpg", getPicDir().absolutePath, File.separator,SimpleDateFormat("yyyyMMddHHmmssSS", Locale.CHINA).format(Date()))
        return File(name)
    }

    override fun getPicDir():File{
        return File(App.getPicDir() + File.separator +"pic").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    interface OnPicture{
        fun onTaken(bmp:Bitmap?)
    }

    override fun setPicCallback(callback:OnPicture?){
        mPicCallback = callback
    }

    override fun tackPic(){
        if (mCameraDevice == null){
            return
        }

        if (null == mCameraCaptureSession) {
            return
        }

        try {
            val picCaptureRequestBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(mPicImageReader!!.surface)
                set(CaptureRequest.JPEG_ORIENTATION,getOrientation())

                set(CaptureRequest.CONTROL_AF_MODE,CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                set(CaptureRequest.CONTROL_AF_TRIGGER,CameraMetadata.CONTROL_AF_TRIGGER_START)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
            }
            mCameraCaptureSession!!.capture(picCaptureRequestBuilder.build(),mCaptureCallback,mBackgroundHandler)
        }catch (e:IllegalArgumentException){
            Log.e("VideoCameraManager tackPic"," " + e.message)
        }catch (e:CameraAccessException){
            Log.e("VideoCameraManager tackPic", " " +e.message)
        }catch (e:IllegalStateException ){
            Log.e("VideoCameraManager tackPic"," " + e.message)
        }
    }

    override fun recodeVideo(){
        launch {
            if (mCameraDevice == null){
                openCamera()
            }
        }
    }

    private val mCaptureCallback = object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureStarted(
            session: CameraCaptureSession,
            request: CaptureRequest, timestamp: Long, frameNumber: Long
        ) {

        }
        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest, partialResult: CaptureResult
        ) {

        }
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest, result: TotalCaptureResult
        ) {
            val  afState = result.get(CaptureResult.CONTROL_AF_STATE)
        }
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

     override fun getValidCameraId():String{
        return if (hasBack){
            mCameraBackId
        }else mCameraFaceId
    }

    override fun releaseResource(){
        releaseCamera()
        cancel()
    }

    override fun clearCallback() {
        mPicCallback = null
    }

    private fun releaseCamera(){
        if (mPreviewCaptureRequestBuilder != null && mPreviewList.isNotEmpty()){
            mPreviewList.forEach {
                mPreviewCaptureRequestBuilder!!.removeTarget(it)
            }
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

        if (mPicImageReader != null){
            mPicImageReader!!.close()
            mPicImageReader = null
        }
    }

    override fun setDisplaySize(w:Int, h: Int){
        mPreviewWidth = w
        mPreviewHeight = h
    }

    /*
    * w,h are preview's size
    * */
    override fun updateFocusRegion(w: Int, h: Int, l: Int, t: Int, r: Int, b: Int){
        if (mCameraCaptureSession != null && mPreviewCaptureRequestBuilder != null){

            mPreviewCaptureRequestBuilder?.apply {
                get(CaptureRequest.SCALER_CROP_REGION)?.apply {

                    val wRatio = width() * 1f / h * 1f
                    val hRatio = height() * 1f / w * 1f

                    val left = if (l < 0) 0 else (l * wRatio).toInt()
                    val right = if (r < 0) 0 else (r * wRatio).toInt()
                    val top = if (t < 0) 0 else (t * hRatio).toInt()
                    val bottom = if (b < 0) 0 else (b * hRatio).toInt()

                    val focusRect = Rect(left,top,right,bottom)
                    val orientation = getOrientation()
                    if (orientation == 90 || orientation == 270){
                        focusRect.set(top,left,bottom,right)
                    }

                    set(CaptureRequest.CONTROL_AF_MODE,CameraMetadata.CONTROL_AF_MODE_AUTO)
                    set(CaptureRequest.CONTROL_AF_TRIGGER,CameraMetadata.CONTROL_AF_TRIGGER_START)
                    set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(MeteringRectangle(focusRect, 1000)))

                    mCameraCaptureSession!!.setRepeatingRequest(build(),mCaptureCallback,mBackgroundHandler)
                }
            }
        }
    }

    override fun addSurface(surface: Surface){
        mPreviewList.add(surface)
    }

    enum class MODE {
        RECORD, PICTURE, SHORT_RECORD
    }
    enum class RECORD_STATUS {
        START, STOP
    }

}