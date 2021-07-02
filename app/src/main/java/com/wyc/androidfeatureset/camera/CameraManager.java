package com.wyc.androidfeatureset.camera;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.wyc.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadPoolExecutor;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.schedulers.Schedulers;

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
    private int mCameraId = CAMERA_FACING_BACK;
    private OnFocusSuccessListener mFocusSuccessListener;
    private OnPictureTakenListener mPictureTakenListener;
    private OnPreviewListener mPreviewListener;
    private volatile boolean capturing;
    private Camera.Size mPreviewSize;
    private int mPreviewFormat;
    private byte[] mBuffer;

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
            int size = mPreviewSize.height * mPreviewSize.width;
            size = size * ImageFormat.getBitsPerPixel(mPreviewFormat) / 8;
            mBuffer = new byte[size];
            mCamera.addCallbackBuffer(mBuffer);
            mCamera.setPreviewCallbackWithBuffer((data, camera) -> {
                if (mPreviewListener != null)
                    mPreviewListener.preview(data,mPreviewFormat,mPreviewSize.width,mPreviewSize.height);

                if (mCamera != null)mCamera.addCallbackBuffer(mBuffer);
            });

            mCamera.startPreview();
        }
    }

    public void autoFocus(){
        if (openSuccess()){
            mCamera.autoFocus(autoFocusCallback);
        }
    }

    public void releaseCamera(){
        if (openSuccess()){
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            Logger.d("Camera released...");
        }
    }

    public void initCamera(){
        if (checkCameraHardware() && mCamera == null){
            try {
                mCamera = Camera.open(mCameraId);
                setPictureSize();
                Logger.d("Camera opened...");
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
                mFocusSuccessListener.onFocusSuccess(camera);
        }
    };

    public void switchCamera(SurfaceHolder holder) {
        releaseCamera();
        switchCameraId();
        initCamera();
        setPreviewDisplay(holder);
        startPreview();
        autoFocus();
    }

    public interface OnFocusSuccessListener{
        void onFocusSuccess(Camera camera);
    }

    public void setFocusSuccessListener(OnFocusSuccessListener listener) {
        this.mFocusSuccessListener = listener;
    }

    private final Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Observable.create((ObservableOnSubscribe<PictureInfo>) emitter -> {
                Bitmap bitmap = adjustPhotoRotation(BitmapFactory.decodeByteArray(data,0,data.length),getCameraDisplayOrientation());
                Uri uri = picUri();
                final PictureInfo info = new PictureInfo(bitmap,uri);
                emitter.onNext(info);
                savePic(bitmap,uri);
            }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(info -> {
                if (mPictureTakenListener != null){
                    mPictureTakenListener.pictureTaken(info);
                    mPictureTakenListener.finish();
                }
                startPreview();
                capturing = false;
            }, throwable ->{
                throwable.printStackTrace();
                Toast.makeText(mContext,throwable.getMessage(),Toast.LENGTH_SHORT).show();
            });
        }
    };
    public static class PictureInfo implements Serializable{
        private final Bitmap bitmap;
        private final Uri uri;
        public PictureInfo(Bitmap b,Uri r){
            bitmap = b;
            uri = r;
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

        public Uri getUri() {
            return uri;
        }
    }
    private void savePic(Bitmap pic,Uri uri) throws IOException{
        try (OutputStream outputStream = mContext.getContentResolver().openOutputStream(uri)){
            pic.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
        }
        Logger.d("savePic of uri:%s",uri);
    }
    private Uri picUri(){
        final String file_name = new SimpleDateFormat("yyyyMMddHHmmss",Locale.CHINA).format(new Date()) + ".jpg";
        return Uri.fromFile(new File(getPicDir(),file_name));
    }
    public String getPicDir(){
        return mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath();
    }

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
        void start();
        void pictureTaken(PictureInfo info);
        void finish();
    }

    private void setPictureTakenListener(OnPictureTakenListener l){
        this.mPictureTakenListener = l;
    }

    public interface OnPreviewListener{
        void preview(byte[] data,int format,int w,int h);
    }
    private void setPreviewListener(OnPreviewListener l){
        mPreviewListener = l;
    }

    public void takePicture(OnPictureTakenListener listener){
        if (!capturing && openSuccess()){
            capturing = true;
            listener.start();
            setPictureTakenListener(listener);
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
            param.setPreviewSize(w,h);
            //param.setPreviewFormat(ImageFormat.YV12);
            mCamera.setParameters(param);

            mPreviewSize = mCamera.getParameters().getPreviewSize();
            mPreviewFormat = param.getPreviewFormat();
        }
    }

    public Point findBestPreviewSizeValue(int x,int y) {
        if (!openSuccess())return new Point(-1,-1);

        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {
            Camera.Size defaultSize = parameters.getPreviewSize();
            if (defaultSize == null) {
                throw new IllegalStateException("Parameters contained no preview size!");
            }
            Logger.d("Device returned no supported preview sizes; using default; width:%d,heigh:%d",defaultSize.width,defaultSize.height);
            return new Point(defaultSize.width, defaultSize.height);
        }

        double screenAspectRatio = x / (double) y;

        formatPreviewSizes(rawSupportedSizes);

        boolean isCandidatePortrait = x < y;

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

            int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
            int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
            double aspectRatio = maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                continue;
            }

            if (maybeFlippedWidth == x && maybeFlippedHeight == y) {
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
        Camera.Size sizeObj = pic_sizes.get(0);

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
        int rotation =  ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
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

    public int getCameraId() {
        return mCameraId;
    }

    public void setCameraId(int id) {
        this.mCameraId = id;
    }
    public void setPreviewCb(OnPreviewListener l){
        setPreviewListener(l);
    }
    public int getPreviewFormat(){
        return mPreviewFormat;
    }
}
