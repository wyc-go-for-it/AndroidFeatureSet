package com.wyc.video.camera

import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.CameraDevice.StateCallback.*
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import com.wyc.logger.Logger
import com.wyc.video.Utils
import com.wyc.video.VideoApp
import com.wyc.video.recorder.VideoMediaRecorder
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
import java.util.concurrent.Semaphore


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

class VideoCameraManager private constructor() : CoroutineScope by CoroutineScope(Dispatchers.IO) {

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
    private var mSensorOrientation = 90
    private var mImageReader : ImageReader? = null
    private var mPicCallback:OnPicture? = null
    private var mMediaRecorder:VideoMediaRecorder? = null
    private var mMode = MODE.PICTURE
    private var mRecordStatus = RECORD_STATUS.STOP

    private var hasSwitchCamera = false

    private val mPreviewList = mutableListOf<Surface>()

    init {
        initCamera()
    }

    companion object{
        private var sCameraManager:VideoCameraManager? = null
        @JvmStatic
        fun getInstance():VideoCameraManager{
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
                sCameraManager!!.releaseResource()
                sCameraManager = null
            }
        }
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
            val capabilities = characteristic.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)

            mSensorOrientation = characteristic.get(CameraCharacteristics.SENSOR_ORIENTATION)?:0

            Utils.logInfo("sensorOrientation:$mSensorOrientation,physicalRect:$physicalRect,pixelRect:$pixelRect" +
                    ",activeRect:$activeRect,afRegionCount:$afRegion,fpsRegion:" + Arrays.toString(fpsRegion)+
                    ",capabilities:" + Arrays.toString(capabilities))

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

    fun sycCaptureMode(mode:MODE){
        if (mMode != mode){
            mMode = mode
            hasSwitchCamera = false
            openCamera()
        }
    }
    fun sycRecordingState(state:RECORD_STATUS){
        mRecordStatus = state
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

    fun openCamera(){

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

    fun calPreViewAspectRatio():Float{
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

    private fun createPhotographSession(){
        if (mCameraDevice == null){
            return
        }
        
        if (mImageReader == null){
            mImageReader = ImageReader.newInstance(4000,3000,ImageFormat.JPEG,1)
            mImageReader!!.setOnImageAvailableListener(mPicImageAvailableListener,mBackgroundHandler)
        }

        try {

            val listSurface = mutableListOf<Surface>()
            listSurface.addAll(mPreviewList)
            listSurface.add(mImageReader!!.surface)

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

    private fun createRecordSession(){
        if (mCameraDevice == null){
            return
        }

        if (mMediaRecorder == null){
            mMediaRecorder = VideoMediaRecorder()
        }
        if (!hasSwitchCamera)mMediaRecorder!!.configure()

        try {
            val listSurface = mutableListOf<Surface>()
            listSurface.addAll(mPreviewList)
            listSurface.add(mMediaRecorder!!.getSurface())

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
            if (hasSwitchCamera){
                hasSwitchCamera = false
                startRecordRequest()
            }else createPreviewRequest()
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
                        mPreviewList.forEach {
                            addTarget(it)
                        }

                        set(CaptureRequest.CONTROL_AF_MODE,CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                        set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)

                        mCameraCaptureSession!!.setRepeatingRequest(build(),mCaptureCallback,mBackgroundHandler)
                    }
                }
            }
        }catch (e:IllegalArgumentException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }catch (e:CameraAccessException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }catch (e:IllegalStateException){
            Utils.showToast(e.message)
            Utils.logInfo(e.message)
        }
    }

    private val mPicImageAvailableListener = ImageReader.OnImageAvailableListener {
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
                set(CaptureRequest.CONTROL_AF_TRIGGER,CameraMetadata.CONTROL_AF_TRIGGER_START)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
            }
            mCameraCaptureSession!!.capture(picCaptureRequestBuilder.build(),mCaptureCallback,mBackgroundHandler)
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
            mMediaRecorder?.apply {
                start()
                startRecordRequest()
            }
        }
    }

    private fun startRecordRequest(){
        try {
            if (mCameraDevice == null){
                return
            }

            if (null == mCameraCaptureSession) {
                return
            }

            val recordCaptureRequestBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                mPreviewList.forEach {
                    addTarget(it)
                }

                addTarget(mMediaRecorder!!.getSurface())

                set(CaptureRequest.CONTROL_AF_MODE,CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
            }
            mCameraCaptureSession!!.setRepeatingRequest(recordCaptureRequestBuilder.build(),mCaptureCallback,mBackgroundHandler)
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

    fun stopRecord(preview:Boolean){
        if (mMediaRecorder != null && mMode != MODE.PICTURE && mRecordStatus == RECORD_STATUS.START){
            mRecordStatus = RECORD_STATUS.STOP
            launch {
                mMediaRecorder!!.stop()
                if (preview)createPreviewRequest()
            }
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

    fun getValidCameraId():String{
        return if (hasBack){
            mCameraBackId
        }else mCameraFaceId
    }
    fun getOrientation():Int{
        return mSensorOrientation
    }

    fun hasRecording():Boolean{
        return mRecordStatus == RECORD_STATUS.START
    }

    private fun releaseResource(){
        Utils.logInfo("start release resource.")
        releaseRecorder()
        releaseCamera()
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

        if (mImageReader != null){
            mImageReader!!.close()
            mImageReader = null
        }
    }

    private fun releaseRecorder(){
        if (hasSwitchCamera)return

        if (mMediaRecorder != null){
            mMediaRecorder!!.release()
            mMediaRecorder = null
        }
    }

    /*
    * w,h are preview's size
    * */
    fun updateFocusRegion(w: Int, h: Int, l: Int, t: Int, r: Int, b: Int){
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
                    if (mSensorOrientation == 90 || mSensorOrientation == 270){
                        focusRect.set(top,left,bottom,right)
                    }
                    Utils.logInfo("focusRect:$focusRect,cropRegion:$this")

                    set(CaptureRequest.CONTROL_AF_MODE,CameraMetadata.CONTROL_AF_MODE_AUTO)
                    set(CaptureRequest.CONTROL_AF_TRIGGER,CameraMetadata.CONTROL_AF_TRIGGER_START)
                    set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(MeteringRectangle(focusRect, 1000)))

                    mCameraCaptureSession!!.setRepeatingRequest(build(),mCaptureCallback,mBackgroundHandler)
                }
            }
        }
    }
    fun switchCamera(){
        if (mPreviewList.isNotEmpty()){
            hasBack = !hasBack
            if (mMode != MODE.PICTURE)hasSwitchCamera = true
            openCamera()
        }
    }

    fun addSurface(surface: Surface){
        mPreviewList.add(surface)
    }

    enum class MODE {
        RECORD, PICTURE, SHORT_RECORD
    }
    enum class RECORD_STATUS {
        START, STOP
    }
}