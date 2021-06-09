package com.wyc.androidfeatureset.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.io.IOException;

/**
 * @ProjectName: AndroidFeatureSet
 * @Package: com.wyc.androidfeatureset.camera
 * @ClassName: CameraPreview
 * @Description: 相机预览类
 * @Author: wyc
 * @CreateDate: 2021-06-09 15:05
 * @UpdateUser: 更新者
 * @UpdateDate: 2021-06-09 15:05
 * @UpdateRemark: 更新说明
 * @Version: 1.0
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraPreview";
    private CameraManager mCameraManager;
    public CameraPreview(Context context) {
        this(context,null);
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr,0);
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        getHolder().addCallback(this);
    }

    public void setCamera(CameraManager c){
        mCameraManager = c;
    }


    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        mCameraManager.setPreviewDisplay(holder);
        mCameraManager.startPreview();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        if (holder.getSurface() == null){
            return;
        }
        try {
            mCameraManager.stopPreview();
        } catch (Exception e){
            e.printStackTrace();
        }
        mCameraManager.setPreviewDisplay(holder);
        mCameraManager.startPreview();
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        mCameraManager.releaseCamera();
    }
}
