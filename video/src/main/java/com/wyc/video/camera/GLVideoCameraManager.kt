package com.wyc.video.camera

import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.CamcorderProfile
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Range
import android.util.Size
import android.view.Surface
import com.wyc.logger.Logger
import com.wyc.video.Utils
import com.wyc.video.VideoApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.min


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.video.camera
 * @ClassName:      GLVideoCameraManager
 * @Description:    gl 照相机
 * @Author:         wyc
 * @CreateDate:     2022/12/27 14:12
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/12/27 14:12
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class GLVideoCameraManager : CoroutineScope by CoroutineScope(Dispatchers.IO),ICamera  {
    private var mCameraFaceId:String = ""
    private var mCameraBackId:String = ""
    private var mCameraDevice: CameraDevice? = null
    private var mCameraCaptureSession: CameraCaptureSession? = null
    private var mBackgroundHandler: Handler? = null
    private var mBackgroundThread: HandlerThread? = null
    private var hasBack = true
    private var mPreviewCaptureRequestBuilder: CaptureRequest.Builder? = null
    private var mExecutor: ExecutorService? = null
    private var mAspectRatio = 0.75f

    private var mMode = MODE.PICTURE
    private var mRecordStatus = RECORD_STATUS.STOP

    private val mPreviewList = mutableListOf<Surface>()

    private var mBestFPSRange: Range<Int> =  Range(30,30)

    private var vWidth = 1280
    override fun getVWidth(): Int {
         return vWidth
    }

    private var vHeight = 720
    override fun getVHeight(): Int {
        return vHeight
    }

    override fun getBastFPS(): Int {
         return 30
    }

    init {
        initCamera()
    }

    companion object{
        private var sCameraManager:ICamera? = null
        @JvmStatic
        fun getInstance():ICamera{
            if (sCameraManager == null){
                synchronized(GLVideoCameraManager::class){
                    if (sCameraManager == null){
                        sCameraManager = GLVideoCameraManager()
                    }
                }
            }
            return sCameraManager!!
        }
        @JvmStatic
        fun clear(){
            if (sCameraManager != null){
                sCameraManager!!.releaseResource()
                sCameraManager = null
            }
        }
    }

    override fun getOrientation():Int{
        val cManager =  VideoApp.getInstance().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val characteristic = cManager.getCameraCharacteristics(getValidCameraId())
        return characteristic.get(CameraCharacteristics.SENSOR_ORIENTATION)?:0
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
                outputSizes = arrayOf(Size(0,0,))
            }
            val fpsRegion = configMap?.highSpeedVideoFpsRanges

            val code = CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)
            Logger.d("code:%s",code)

            val sensorOrientation = characteristic.get(CameraCharacteristics.SENSOR_ORIENTATION)?:0

            Utils.logInfo("sensorOrientation:$sensorOrientation,physicalRect:$physicalRect,pixelRect:$pixelRect" +
                    ",activeRect:$activeRect,afRegionCount:$afRegion,\nfpsAeRegion:" + Arrays.toString(fpsAeRegion)+
                    ",capabilities:" + Arrays.toString(capabilities) + "vWidth:$vWidth" +",vHeight:$vHeight" +"," +
                    "\noutputSizes:" + Arrays.toString(outputSizes) + "\nfpsRegion:" + Arrays.toString(fpsRegion))

            activeRect?.apply {
                mAspectRatio = height() * 1f / width() * 1f
                Logger.d("aspectRatio:%f",mAspectRatio)
            }

        }catch (e: CameraAccessException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }catch (e: IllegalArgumentException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }
    }
    private fun selectBaseFPS(range: Array<Range<Int>>?){
        range?.apply {
            mBestFPSRange = this[this.size - 1]
        }
    }

    fun sycCaptureMode(mode:MODE){
        if (mMode != mode){
            mMode = mode
            openCamera()
        }
    }
    fun sycRecordingState(state:RECORD_STATUS){
        mRecordStatus = state
    }

    private fun startBackgroundThread(){
        stopBackgroundThread()
        Utils.logInfo("start backgroundThread.")
        mBackgroundThread = HandlerThread("cameraBackgroundThread")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
        mExecutor = Executors.newFixedThreadPool(2)
    }
    private fun stopBackgroundThread() {
        if (mBackgroundThread != null) {
            Utils.logInfo("stop backgroundThread.")

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

    override fun openCamera(){
        openBackCamera()
    }

    private fun openBackCamera(){

        if (mPreviewList.isEmpty()){
            return
        }

        if (mCameraBackId.isBlank() && mCameraFaceId.isBlank()){
            Utils.logInfo("backId and faceId both are empty.")
            return
        }

        releaseResource()
        startBackgroundThread()

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

    override fun calPreViewAspectRatio():Float{
        return  mAspectRatio
    }
    private val mStateCallback = object  : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            mCameraDevice = camera
            createSession()
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

    private fun createSession(){
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
                    mCameraCaptureSession!!.setRepeatingRequest(mPreviewCaptureRequestBuilder!!.build(),null,mBackgroundHandler)
                }else{
                    mPreviewCaptureRequestBuilder = mCameraDevice!!.createCaptureRequest(
                        CameraDevice.TEMPLATE_PREVIEW).apply {
                        mPreviewList.forEach {
                            addTarget(it)
                        }

                        set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                        set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                        set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, mBestFPSRange)

                        mCameraCaptureSession!!.setRepeatingRequest(build(),mCaptureCallback,mBackgroundHandler)
                    }
                }
            }
        }catch (e:IllegalArgumentException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }catch (e: CameraAccessException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }catch (e:IllegalStateException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }
    }

    private fun getPicFile(): File {
        val name = String.format(
            Locale.CHINA, "%s%s%s.jpg", getPicDir().absolutePath, File.separator,
            SimpleDateFormat("yyyyMMddHHmmssSS", Locale.CHINA).format(Date()))
        return File(name)
    }

    override fun getPicDir(): File {
        return File(VideoApp.getVideoDir() + File.separator +"pic").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    interface OnPicture{
        fun onTaken(file: File)
    }

    override fun tackPic(){

    }

    override fun sycCaptureMode(mode: VideoCameraManager.MODE) {

    }

    override fun recodeVideo(){
        launch {
            if (mCameraDevice == null){
                openCamera()
            }
        }
    }

    private fun startRecordRequest(){

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

    override fun stopRecord(preview:Boolean){
        if (  mMode != MODE.PICTURE && mRecordStatus == RECORD_STATUS.START){
            mRecordStatus = RECORD_STATUS.STOP
            launch {

            }
        }
    }



    private fun getOpenErrorMsg(code:Int):String{
        return when(code){
            CameraDevice.StateCallback.ERROR_CAMERA_IN_USE ->{
                "camera device is in use already."
            }
            CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE ->{
                "too many other open camera devices."
            }
            CameraDevice.StateCallback.ERROR_CAMERA_DISABLED ->{
                "camera device could not be opened."
            }
            CameraDevice.StateCallback.ERROR_CAMERA_DEVICE ->{
                "camera device has encountered a fatal error."
            }
            CameraDevice.StateCallback.ERROR_CAMERA_SERVICE ->{
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

    override fun sycRecordingState(state: VideoCameraManager.RECORD_STATUS) {

    }

    override fun isBack():Boolean{
        return hasBack
    }

    fun hasRecording():Boolean{
        return mRecordStatus == RECORD_STATUS.START
    }

    override fun releaseResource(){
        Utils.logInfo("start release resource.")
        releaseRecorder()
        releaseCamera()
    }

    override fun clearCallback() {

    }

    override fun setPicCallback(callback: VideoCameraManager.OnPicture?) {

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

    }

    private fun releaseRecorder(){

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
                    Utils.logInfo("focusRect:$focusRect,cropRegion:$this")

                    set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO)
                    set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
                    set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(MeteringRectangle(focusRect, 1000)))

                    mCameraCaptureSession!!.setRepeatingRequest(build(),mCaptureCallback,mBackgroundHandler)
                }
            }
        }
    }
    override fun switchCamera(){
        if (mPreviewList.isNotEmpty()){
            hasBack = !hasBack
            openCamera()
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