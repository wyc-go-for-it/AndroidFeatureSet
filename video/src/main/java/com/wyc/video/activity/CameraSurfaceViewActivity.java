package com.wyc.video.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Bundle;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.wyc.permission.OnPermissionCallback;
import com.wyc.permission.Permission;
import com.wyc.permission.XXPermissions;
import com.wyc.video.R;
import com.wyc.video.Utils;
import com.wyc.video.camera.AdaptiveSurfaceView;
import com.wyc.video.camera.CameraManager;
import com.wyc.video.camera.CircleImage;
import com.wyc.video.camera.RecordBtn;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class CameraSurfaceViewActivity extends BaseActivity {
    private CameraManager mCameraManager;
    private CircleImage mThumbnails;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMiddleText(getString(R.string.useSurfaceView));

        initSurface();
        initRecordBtn();
        initCameraReverse();
        initThumbnails();
    }

    private void initThumbnails(){
        mThumbnails = findViewById(R.id.thumbnails);
        mThumbnails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        new Thread(() -> {
            File picDir = mCameraManager.getPicDir();
            File[] pics = picDir.listFiles();
            if (pics != null && pics.length > 0){
                Arrays.sort(pics, (o1, o2) -> Long.compare(o1.lastModified(), o2.lastModified()));
                File pic = pics[pics.length - 1];
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                Bitmap bitmap = BitmapFactory.decodeFile(pic.getAbsolutePath(),options);
                runOnUiThread(()-> mThumbnails.setImageBitmap(bitmap));
            }
        }).start();
        mCameraManager.setPicCallback(bmp -> runOnUiThread(()-> mThumbnails.setImageBitmap(bmp)));
    }

    private void initCameraReverse(){
        final Button camera_reverse = findViewById(R.id.camera_reverse);
        camera_reverse.setOnClickListener(v -> mCameraManager.switchCamera());
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
                mCameraManager.tackPic();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraManager.setPicCallback(null);
    }

    @Override
    public int getContentLayoutId() {
        return R.layout.activity_camera_surface_view;
    }

    public static void start(Activity c){
        c.startActivity(new Intent(c,CameraSurfaceViewActivity.class));
    }
}