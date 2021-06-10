package com.wyc.androidfeatureset.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
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
    private final Context mContext;
    private Camera mCamera;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK ;
    private OnFocusSuccessListener mFocusSuccessListener;
    private OnPictureTakenListener mPictureTakenListener;
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
                mCamera = Camera.open();
            }catch (Exception e){
                e.printStackTrace();
                Logger.e("Error open camera:%s",e.getMessage());
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
            }
        }
    };

    Bitmap adjustPhotoRotation(Bitmap bm, final int orientationDegree) {
        Matrix m = new Matrix();

        Logger.d("orientationDegree:%d",orientationDegree);

        m.setRotate(orientationDegree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);

        int ok_w = bm.getWidth(),ok_h = bm.getHeight();
        float targetX, targetY;
        if (orientationDegree == 90) {
            targetX = ok_w;
            targetY = 0;
        } else {
            targetX = ok_h;
            targetY = ok_w;
        }


        final float[] values = new float[9];
        m.getValues(values);


        float x1 = values[Matrix.MTRANS_X];
        float y1 = values[Matrix.MTRANS_Y];

        m.postTranslate(targetX - x1, targetY - y1);

        final Bitmap ok = Bitmap.createBitmap(ok_w,ok_h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(ok);
        canvas.drawBitmap(bm, m, null);

        return ok;
    }

    public interface OnPictureTakenListener {
        void pictureTaken(Bitmap pic);
    }

    private void setPictureTakenListener(OnPictureTakenListener l){
        this.mPictureTakenListener = l;
    }

    public void takePicture(OnPictureTakenListener listener){
        if (openSuccess()){
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

    public void setPictureSize(){
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPictureFormat(PixelFormat.JPEG);
        List<Camera.Size> pic_sizes = parameters.getSupportedPictureSizes();
        formatSize(pic_sizes);
        parameters.setPictureSize(720, 480);
        mCamera.setParameters(parameters);
    }

    private void formatSize(List<Camera.Size> list){
        final StringBuilder sb = new StringBuilder();
        for (Camera.Size s : list){
            sb.append(String.format(Locale.CHINA,"%s of width:%d,height:%d",s,s.width,s.height)).append("\r\n");
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
        return result;
    }

    private boolean openSuccess(){
        return mCamera != null;
    }
}
