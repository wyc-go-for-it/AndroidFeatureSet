package com.wyc.androidfeatureset.camera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.Toast;

import com.wyc.androidfeatureset.R;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CaptureActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION = 1000;
    @BindView(R.id.preview)
    CameraPreview preview;
    @BindView(R.id.pic_view)
    CircleImage pic_view;
    private CameraManager cameraManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_activy);
        ButterKnife.bind(this);

        cameraManager = new CameraManager(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            cameraManager.initCamera();
        }
        preview.setCamera(cameraManager);

        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    }

    @OnClick(R.id.pic_btn)
    void takePicture(){
        preview.takePicture(new CameraPreview.Subscribe(preview) {
            @Override
            public void pictureTaken(Bitmap pic) {
                pic_view.setImageBitmap(pic);
            }
        });
    }
    @OnClick(R.id.pic_view)
    void clickPic(){
        Toast.makeText(this,"hello",Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.swt_btn)
    void switchCamera(){
        preview.switchCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkCameraPermission();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraManager.releaseCamera();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void checkCameraPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if ((ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA))) {
                Toast.makeText(this,"APP不能使用相机,请设置允许APP访问权限",Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA}, CAMERA_PERMISSION);
            }
        }
    }

    public static void start(Context context){
        final Intent intent = new Intent();
        intent.setClass(context,CaptureActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

}