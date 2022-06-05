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
import com.wyc.video.Utils;
import com.wyc.video.camera.AdaptiveSurfaceView;
import com.wyc.video.camera.CameraManager;
import com.wyc.video.camera.RecordBtn;

import java.util.List;

public class CameraSurfaceViewActivity extends BaseActivity {
    private CameraManager mCameraManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMiddleText(getString(R.string.useSurfaceView));

        initSurface();
        initRecordBtn();
    }
    private void initSurface(){
        mCameraManager = new CameraManager();
        AdaptiveSurfaceView surfaceView = findViewById(R.id.preview_surface);
        surfaceView.setCameraManager(mCameraManager);
    }

    private void initRecordBtn(){
        final RecordBtn btn = findViewById(R.id.recordBtn);
        btn.setCallback(new RecordBtn.ActionCallback() {
            @Override
            public void startRecord() {
                Utils.showToast("startRecord");
            }

            @Override
            public void finishRecord(long recordTime) {
                Utils.showToast("finishRecord:" + recordTime);
            }

            @Override
            public void takePicture() {
                Utils.showToast("takePicture");
            }
        });
    }

    @Override
    public int getContentLayoutId() {
        return R.layout.activity_camera_surface_view;
    }

    public static void start(Activity c){
        c.startActivity(new Intent(c,CameraSurfaceViewActivity.class));
    }
}