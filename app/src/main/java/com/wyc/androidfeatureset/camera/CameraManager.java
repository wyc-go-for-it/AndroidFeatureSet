package com.wyc.androidfeatureset.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.wyc.logger.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;

/**
 * @ProjectName: AndroidFeatureSet
 * @Package: com.wyc.androidfeatureset.camera
 * @ClassName: CameraManager
 * @Description: 相机管理
 * @Author: wyc
 * @CreateDate: 2021-06-09 14:53
 * @UpdateUser: 更新者
 * @UpdateDate: 2021-06-09 14:53
 * @UpdateRemark: 更新说明
 * @Version: 1.0
 */
public class CameraManager {
    private static final int MIN_PREVIEW_PIXELS = 480 * 320; // normal screen
    private static final double MAX_ASPECT_DISTORTION = 0.15;

    private final Context mContext;
    private Camera mCamera;
    private int mCameraId = CAMERA_FACING_BACK ;
    private OnFocusSuccessListener mFocusSuccessListener;
    private OnPictureTakenListener mPictureTakenListener;
    private volatile boolean capturing;

    public CameraManager(Context context){
        mContext = context;
    }

    public void focus(float x,float y,int area){
        if (openSuccess()){
            Camera.Parameters params = mCamera.getParameters();
            final Rect r_focus = new Rect();
            int area_half = area / 2;

            int area_x = (int) (x * 2000f - 1000f);
            int area_y = (int) (y * 2000f - 1000f);

            //Area到屏幕的映射区域是从左上角的-1000,-1000到右下角的1000,1000，中心点是0,0
            r_focus.left = Math.max(area_x - area_half,-1000);
            r_focus.top = Math.max(area_y - area_half,-1000);
            r_focus.right = Math.min(area_x + area_half,1000);
            r_focus.bottom = Math.min(area_y + area_half,1000);

            Logger.d("r_focus:%s",r_focus);

            Camera.Area cameraArea = new Camera.Area(r_focus, 1000);

            if (params.getMaxNumFocusAreas() > 0){
                List<Camera.Area> focusAreas = new ArrayList<>();
                focusAreas.add(cameraArea);
                params.setFocusMode(Camera.Parameters.FLASH_MODE_AUTO);
                params.setFocusAreas(focusAreas);
            }

            if (params.getMaxNumMeteringAreas() > 0){
                List<Camera.Area> meteringAreas = new ArrayList<>();
                meteringAreas.add(cameraArea);
                params.setMeteringAreas(meteringAreas);
            }

            mCamera.cancelAutoFocus();
            mCamera.setParameters(params);
            mCamera.autoFocus(autoFocusCallback);
        }
    }


    private void clearCameraFocus() {
        if (openSuccess()){
            mCamera.cancelAutoFocus();
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFocusAreas(null);
            parameters.setMeteringAreas(null);
            try {
                mCamera.setParameters(parameters);
            } catch (Exception e) {
                e.printStackTrace();
                Logger.d("failed to set focus parameters:%s",e.getMessage());
            }
        }
    }

    public void stopPreview(){
        if (openSuccess()){
            try {
                mCamera.stopPreview();
            }catch (RuntimeException e){
                e.printStackTrace();
                Logger.d("Error stop camera:%s",e.getMessage());
            }
        }
    }

    public void startPreview(){
        if (openSuccess()){
            mCamera.startPreview();
        }
    }

    public void autoFocus(){
        if (openSuccess()){
            mCamera.autoFocus(autoFocusCallback);
        }
    }

    public void cancelFocus(){
        if (openSuccess())mCamera.cancelAutoFocus();
    }

    public void releaseCamera(){
        if (openSuccess()){
            mCamera.release();
        }
    }

    public void initCamera(){
        if (checkCameraHardware()){
            try {
                mCamera = Camera.open(mCameraId);
            }catch (Exception e){
                e.printStackTrace();
                Logger.e("Error open camera:%s",e.getMessage());
            }
        }
    }

    private void switchCameraId(){
        int c = Camera.getNumberOfCameras();
        if (c >= 2){
            if (mCameraId == CAMERA_FACING_BACK){
                mCameraId = CAMERA_FACING_FRONT;
            }else if (mCameraId == CAMERA_FACING_FRONT){
                mCameraId = CAMERA_FACING_BACK;
            }
        }
    }

    private final Camera.AutoFocusCallback autoFocusCallback = (success, camera) -> {
        Logger.d("AutoFocus:%s",success);
        if (success){
            if (mFocusSuccessListener != null)
                mFocusSuccessListener.success(camera);
        }
    };

    public void switchCamera(SurfaceHolder holder) {
        releaseCamera();
        switchCameraId();
        initCamera();
        setPreviewDisplay(holder);
        startPreview();
    }

    public interface OnFocusSuccessListener{
        void success(Camera camera);
    }

    public void setFocusSuccessListener(OnFocusSuccessListener listener) {
        this.mFocusSuccessListener = listener;
    }

    private final Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            if (mPictureTakenListener != null){
/*                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2;*/
                mPictureTakenListener.pictureTaken(adjustPhotoRotation(BitmapFactory.decodeByteArray(data,0,data.length),getCameraDisplayOrientation()));
                camera.startPreview();
                capturing = false;
            }
        }
    };

    Bitmap adjustPhotoRotation(Bitmap bitmap, final int orientationDegree) {
        Matrix matrix = new Matrix();
        int w = bitmap.getWidth(),h = bitmap.getHeight();

        if (mCameraId == CAMERA_FACING_FRONT){
            switch (orientationDegree){
                case 0:
                    matrix.setScale(-1, 1);
                    break;
                case 90:
                    matrix.setRotate(90,w >> 1,h >>1);
                    matrix.postScale(1, -1);
                    break;
                case 180:
                    matrix.postScale(1, -1);
                    break;
                case 270:
                    matrix.setRotate(90,w >> 1,h >>1);
                    matrix.postScale(-1, 1);
                    break;
            }
        }else
            matrix.setRotate(orientationDegree,w >> 1,h >>1);

        Logger.d("bitmap of width:%d,height:%d",w,h);
        return Bitmap.createBitmap(bitmap,0,0,w,h,matrix,true);
    }

    public interface OnPictureTakenListener {
        void pictureTaken(Bitmap pic);
    }

    private void setPictureTakenListener(OnPictureTakenListener l){
        this.mPictureTakenListener = l;
    }

    public void takePicture(OnPictureTakenListener listener){
        if (!capturing && openSuccess()){
            capturing = true;
            setPictureTakenListener(listener);
            setPictureSize();
            mCamera.takePicture(null,null,pictureCallback);
        }
    }

    private boolean checkCameraHardware() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    public void setPreviewDisplay(SurfaceHolder holder) {
        if (openSuccess()) {
            try {
                mCamera.setDisplayOrientation(getCameraDisplayOrientation());
                mCamera.setPreviewDisplay(holder);
            } catch (IOException | RuntimeException e) {
                e.printStackTrace();
                Logger.e("Error setting camera preview:%s",e.getMessage());
            }
        }
    }

    public void setPreviewSize(int w,int h){
        if (openSuccess()){
            Camera.Parameters param = mCamera.getParameters();
            Point point = findBestPreviewSizeValue(param,new Point(w,h));
            param.setPreviewSize(point.x,point.y);
            param.setPreviewFrameRate(5);//一秒5帧
        }
    }

    private Point findBestPreviewSizeValue(Camera.Parameters parameters, Point screenResolution) {
        List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {
            Camera.Size defaultSize = parameters.getPreviewSize();
            if (defaultSize == null) {
                throw new IllegalStateException("Parameters contained no preview size!");
            }
            Logger.d("Device returned no supported preview sizes; using default; width:%d,heigh:%d",defaultSize.width,defaultSize.height);
            return new Point(defaultSize.width, defaultSize.height);
        }

        double screenAspectRatio = screenResolution.x / (double) screenResolution.y;

        formatPreviewSizes(rawSupportedSizes);

        // Find a suitable size, with max resolution
        int maxResolution = 0;
        Camera.Size maxResPreviewSize = null;
        for (Camera.Size size : rawSupportedSizes) {
            int realWidth = size.width;
            int realHeight = size.height;
            int resolution = realWidth * realHeight;
            if (resolution < MIN_PREVIEW_PIXELS) {
                continue;
            }

            boolean isCandidatePortrait = realWidth < realHeight;
            int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
            int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
            double aspectRatio = maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                continue;
            }

            if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
                Point exactPoint = new Point(realWidth, realHeight);
                Logger.d("Found preview size exactly matching screen size: " + exactPoint);
                return exactPoint;
            }

            // Resolution is suitable; record the one with max resolution
            if (resolution > maxResolution) {
                maxResolution = resolution;
                maxResPreviewSize = size;
            }
        }

        // If no exact match, use largest preview size. This was not a great idea on older devices because
        // of the additional computation needed. We're likely to get here on newer Android 4+ devices, where
        // the CPU is much more powerful.
        if (maxResPreviewSize != null) {
            Point largestSize = new Point(maxResPreviewSize.width, maxResPreviewSize.height);
            Logger.d("Using largest suitable preview size: " + largestSize);
            return largestSize;
        }

        // If there is nothing at all suitable, return current preview size
        Camera.Size defaultPreview = parameters.getPreviewSize();
        if (defaultPreview == null) {
            throw new IllegalStateException("Parameters contained no preview size!");
        }
        Point defaultSize = new Point(defaultPreview.width, defaultPreview.height);
        Logger.d("No suitable preview sizes, using default: " + defaultSize);
        return defaultSize;
    }

    private void formatPreviewSizes(List<Camera.Size> list){
        final StringBuilder sb = new StringBuilder();
        sb.append("PreviewSizes:").append(list.size()).append("\r\n");
        for (Camera.Size s : list){
            sb.append(String.format(Locale.CHINA,"width:%d,height:%d",s.width,s.height)).append("\r\n");
        }
        Logger.d(sb);
    }


    public void setPictureSize(){
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPictureFormat(PixelFormat.JPEG);
        List<Camera.Size> pic_sizes = parameters.getSupportedPictureSizes();
        formatPictureSizes(pic_sizes);
        Camera.Size sizeObj;
        int size = pic_sizes.size();
        if (size >= 3){
            sizeObj = pic_sizes.get(size - 3);
        }else {
            sizeObj = pic_sizes.get(size - 1);
        }
        parameters.setPictureSize(sizeObj.width, sizeObj.height);
        mCamera.setParameters(parameters);
    }

    private void formatPictureSizes(List<Camera.Size> list){
        final StringBuilder sb = new StringBuilder();
        sb.append("PictureSizes:").append(list.size()).append("\r\n");
        for (Camera.Size s : list){
            sb.append(String.format(Locale.CHINA,"width:%d,height:%d",s.width,s.height)).append("\r\n");
        }
        Logger.d(sb);
    }

    private int getCameraDisplayOrientation() {
        final Camera.CameraInfo cameraInfo = new android.hardware.Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, cameraInfo);
        Logger.d("cameraInfo facing:%d,cameraInfo orientation:%d",cameraInfo.facing,cameraInfo.orientation);
        int rotation = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        Logger.d("display rotation :%d",rotation);
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        int result;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }
        Logger.d("result:%d",result);
        return result;
    }

    private boolean openSuccess(){
        return mCamera != null;
    }
}
