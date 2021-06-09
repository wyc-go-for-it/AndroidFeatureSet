package com.wyc.androidfeatureset.camera;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.wyc.logger.Logger;

import java.io.IOException;

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
    private static final String TAG  = "CameraManager";
    private final Context mContext;
    private Camera mCamera;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK ;
    public CameraManager(Context context){
        mContext = context;
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
            mCamera.autoFocus(autoFocusCallback);
        }
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
    };

    private boolean checkCameraHardware() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    public void setPreviewDisplay(SurfaceHolder holder) {
        if (openSuccess()) {
            setCameraDisplayOrientation();
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException | RuntimeException e) {
                e.printStackTrace();
                Logger.e("Error setting camera preview:%s",e.getMessage());
            }
        }
    }

    private void setCameraDisplayOrientation() {
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
        mCamera.setDisplayOrientation(result);
    }

    private boolean openSuccess(){
        return mCamera != null;
    }
}
