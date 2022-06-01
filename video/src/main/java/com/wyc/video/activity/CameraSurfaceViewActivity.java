package com.wyc.video.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.wyc.permission.OnPermissionCallback;
import com.wyc.permission.Permission;
import com.wyc.permission.XXPermissions;
import com.wyc.video.R;
import com.wyc.video.camera.CameraManager;

import java.util.List;

public class CameraSurfaceViewActivity extends BaseActivity implements SurfaceHolder.Callback {
    private CameraManager mCameraManager;
    private SurfaceView mSurfaceView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMiddleText(getString(R.string.useSurfaceView));

        mSurfaceView = findViewById(R.id.preview_surface);
        mSurfaceView.getHolder().addCallback(this);

        mCameraManager = new CameraManager(this);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        Size preViewSize = mCameraManager.calPreViewSize(mSurfaceView.getWidth(),mSurfaceView.getHeight(),format);
        XXPermissions.with(this)
                .permission(Permission.CAMERA)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        mCameraManager.openCamera(holder.getSurface());
                    }
                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        if (never){

                        }
                    }
                });
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        mCameraManager.releaseCamera();
    }

    public static void start(Activity c){
        c.startActivity(new Intent(c,CameraSurfaceViewActivity.class));
    }

    @Override
    public int getContentLayoutId() {
        return R.layout.activity_camera_surface_view;
    }
}