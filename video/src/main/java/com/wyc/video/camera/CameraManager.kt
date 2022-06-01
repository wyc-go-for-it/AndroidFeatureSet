package com.wyc.video.camera

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCharacteristics.LENS_FACING
import android.hardware.camera2.CameraDevice.StateCallback.*
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.InputConfiguration
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import com.wyc.logger.Logger
import com.wyc.video.ToastUtils
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

class CameraManager(c: Context) {
    private var mCameraFaceId:String = ""
    private var mCameraBackId:String = ""
    private var mCameraDevice:CameraDevice? = null
    private var mCameraCaptureSession:CameraCaptureSession? = null
    private var mBackgroundHandler:Handler? = null
    private var mBackgroundThread:HandlerThread? = null
    private var hasBack = true
    private var mPreviewSurface:Surface? = null
    private var mExecutor: ExecutorService? = null
    private var mPreviewSize:Size = Size(-1,-1)
    private var mPreFormat:Int = ImageFormat.YUV_420_888

    private val mContext: Context = c

    init {
        initCamera()
    }

    private fun initCamera(){
        val cManager = mContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val camIdList = cManager.cameraIdList
        if (camIdList.isEmpty()){
            logError("not find any camera with this device.")
            return
        }
        try {
            if (camIdList.size >=2 ){
                mCameraFaceId = camIdList[1]
                mCameraBackId = camIdList[0]
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
            ToastUtils.showToast(e.message)
            logError(e.message)
        }catch (e: IllegalArgumentException){
            ToastUtils.showToast(e.message)
            logError(e.message)
        }
    }

    private fun startBackgroundThread(){
        stopBackgroundThread()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P){
            mBackgroundThread = HandlerThread("cameraBackgroundThread")
            mBackgroundThread!!.start()
            mBackgroundHandler = Handler(mBackgroundThread!!.looper)
        }else{
            mExecutor = Executors.newFixedThreadPool(2)
        }
    }
    private fun stopBackgroundThread(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P){
            if (mBackgroundThread != null){
                mBackgroundThread!!.quitSafely()
                try {
                    mBackgroundThread!!.join()
                    mBackgroundThread = null
                    mBackgroundHandler = null
                }catch (e:InterruptedException ){
                    logError("stop background thread error:$e")
                }
            }
        }else if (mExecutor != null){
            mExecutor!!.shutdown()
            mExecutor = null
        }
    }

    fun openCamera(surface:Surface?){
        if (mPreviewSize.width < 0 || mPreviewSize.height < 0){
            logError("preview size error:$mPreviewSize")
            return
        }

        if (surface == null){
            return
        }

        if (mCameraBackId.isBlank() && mCameraFaceId.isBlank()){
            logError("backId and faceId both are empty.")
            return
        }

        releaseCamera()
        startBackgroundThread()

        mPreviewSurface = surface

        val cManager = mContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cManager.openCamera(getValidCameraId(),mStateCallback,mBackgroundHandler)
        }catch (e:SecurityException){
            ToastUtils.showToast(e.message)
            logError(e.message)
        }catch (e: CameraAccessException){
            ToastUtils.showToast(e.message)
            logError(e.message)
        }catch (e:IllegalArgumentException){
            ToastUtils.showToast(e.message)
            logError(e.message)
        }
    }
    private fun logError(errMsg:String?){
        if (errMsg != null) {
            //Log.e(this::class.simpleName,errMsg)
            Logger.d(this::class.simpleName + ":" + errMsg)
        }
    }

    fun calPreViewSize(width:Int,height:Int,format:Int):Size{
        Logger.d("preview size width:%d,height:%d",width,height)

        if (width == mPreviewSize.width && mPreviewSize.height == height && mPreFormat == format){
            return mPreviewSize
        }

        mPreFormat = format

        var calWidth = 0
        var calHeight = 0
        try {
            (mContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager).getCameraCharacteristics(getValidCameraId())
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.apply {
                    val sizes = getOutputSizes(SurfaceHolder::class.java)
                    logError( "sizes" + Arrays.toString(sizes))

                    sizes.forEach {
                        if (it.width <= width && it.height <= height && it.width >= calWidth && it.height >= calHeight){
                            calWidth = it.width
                            calHeight = it.height
                        }
                    }

                    val sizeTexture =  getOutputSizes(SurfaceTexture::class.java)
                    val sizeImageReader =  getOutputSizes(ImageReader::class.java)

                    logError( "sizeTexture:" + Arrays.toString(sizeTexture))
                    logError( "sizeImageReader:" + Arrays.toString(sizeImageReader))
                }
            if (calWidth != 0 && calHeight != 0){
                mPreviewSize = Size(calWidth,calHeight)

                logError(mPreviewSize.toString())
            }
        }catch (e: CameraAccessException){
            ToastUtils.showToast(e.message)
            logError(e.message)
        }catch (e:IllegalArgumentException){
            ToastUtils.showToast(e.message)
            logError(e.message)
        }
        return mPreviewSize
    }
    private val mStateCallback = object  : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            mCameraDevice = camera
            createCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            ToastUtils.showToast("camera has disconnected.")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            ToastUtils.showToast("camera open error:" + getOpenErrorMsg(error))
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

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P){
                mCameraDevice!!.createCaptureSession(listOf(mPreviewSurface),mCaptureSessionCallback,mBackgroundHandler)
            }else {
                val outputConfiguration = OutputConfiguration(mPreviewSurface!!)
                val config = SessionConfiguration(SessionConfiguration.SESSION_REGULAR,listOf(outputConfiguration),mExecutor!!,mCaptureSessionCallback)
                //config.inputConfiguration = InputConfiguration(1920,1080, ImageFormat.YUV_420_888)
                mCameraDevice!!.createCaptureSession(config)
            }
        }catch (e: CameraAccessException){
            ToastUtils.showToast(e.message)
            logError(e.message)
        }catch (e:IllegalArgumentException){
            ToastUtils.showToast(e.message)
            logError(e.message)
        }
    }

    private val mCaptureSessionCallback = object : CameraCaptureSession.StateCallback(){
        override fun onConfigured(session: CameraCaptureSession) {
            if (mCameraDevice == null){
                return
            }
            mCameraCaptureSession = session
            try {
                val captureRequest = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(mPreviewSurface!!)

                captureRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                captureRequest.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)

                mCameraCaptureSession!!.setRepeatingRequest(captureRequest.build(),null,mBackgroundHandler)
            }catch (e:IllegalArgumentException){
                ToastUtils.showToast(e.message)
                logError(e.message)
            }catch (e:CameraAccessException){
                ToastUtils.showToast(e.message)
                logError(e.message)
            }
        }
        override fun onConfigureFailed(session: CameraCaptureSession) {
            ToastUtils.showToast("CaptureSession configure failure.")
        }

        override fun onClosed(session: CameraCaptureSession) {
            stopBackgroundThread()
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

    private fun getValidCameraId():String{
        return if (hasBack){
            mCameraBackId
        }else mCameraFaceId
    }

    fun releaseCamera(){
        mCameraCaptureSession?.apply {
            close()
            mCameraCaptureSession = null
        }
        mCameraDevice?.apply {
            close()
            mCameraDevice = null
        }
        logError("release resource.")
    }
}