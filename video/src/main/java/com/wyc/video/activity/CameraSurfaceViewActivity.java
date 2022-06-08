package com.wyc.video.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.wyc.permission.OnPermissionCallback;
import com.wyc.permission.Permission;
import com.wyc.permission.XXPermissions;
import com.wyc.video.R;
import com.wyc.video.Utils;
import com.wyc.video.VideoApp;
import com.wyc.video.camera.AdaptiveSurfaceView;
import com.wyc.video.camera.CameraManager;
import com.wyc.video.camera.CircleImage;
import com.wyc.video.camera.RecordBtn;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class CameraSurfaceViewActivity extends VideoBaseActivity {
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
        mThumbnails.setOnClickListener(v -> {
            final Object obj = v.getTag();
            if (obj instanceof Uri){
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType((Uri) obj, "image/*");
                startActivity(intent);
            }
        });
        mCameraManager.setPicCallback(this::decodeImgFile);
    }

    private Uri getImageContentUri(File imageFile) {
        final String filePath = imageFile.getAbsolutePath();
        try(Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DATA + "=? ", new String[]{filePath}, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                Uri baseUri = Uri.parse("content://media/external/images/media");
                return Uri.withAppendedPath(baseUri, "" + id);
            } else {
                if (imageFile.exists()) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DATA, filePath);
                    return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                } else {
                    return null;
                }
            }
        }
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
                XXPermissions.with(CameraSurfaceViewActivity.this)
                        .permission(Permission.RECORD_AUDIO)
                        .request(new OnPermissionCallback() {
                            @Override
                            public void onGranted(List<String> permissions, boolean all) {
                                mCameraManager.recodeVideo();
                            }
                            @Override
                            public void onDenied(List<String> permissions, boolean never) {
                                if (never){

                                }
                            }
                        });
            }

            @Override
            public void finishRecord(long recordTime) {
                Utils.showToast("finishRecord:" + recordTime);
                mCameraManager.stopRecord();
            }

            @Override
            public void takePicture() {
                mCameraManager.tackPic();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadImg();
    }

    private void loadImg(){
        new Thread(() -> {
            File picDir = mCameraManager.getPicDir();
            File[] pics = picDir.listFiles();
            if (pics != null && pics.length > 0){
                Arrays.sort(pics, (o1, o2) -> Long.compare(o1.lastModified(), o2.lastModified()));
                decodeImgFile(pics[pics.length - 1]);
            }else {
                runOnUiThread(()-> mThumbnails.setImageBitmap(null));
            }
        }).start();
    }
    private void decodeImgFile(@NonNull File file){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(),options);
        mThumbnails.setTag(getImageContentUri(file));
        runOnUiThread(()-> mThumbnails.setImageBitmap(bitmap));
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