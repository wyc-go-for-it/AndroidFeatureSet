package com.wyc.video.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
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
import com.wyc.video.camera.VideoCameraManager;
import com.wyc.video.camera.CircleImage;
import com.wyc.video.camera.RecordBtn;
import com.wyc.video.recorder.AbstractRecorder;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class CameraSurfaceViewActivity extends VideoBaseActivity {
    private RecordBtn mRecord;
    private CircleImage mThumbnails;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMiddleText(getString(R.string.useSurfaceView));

        initRecordBtn();
        initCameraReverse();
        initThumbnails();
        initCaptureMode();
    }

    private void initCaptureMode(){
        Button btn = findViewById(R.id.button);
        Button btn1 = findViewById(R.id.button2);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRecord.setCaptureMode(VideoCameraManager.MODE.PICTURE);
            }
        });
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRecord.setCaptureMode(VideoCameraManager.MODE.RECORD);
            }
        });
        findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRecord.setCaptureMode(VideoCameraManager.MODE.SHORT_RECORD);
            }
        });
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
        VideoCameraManager.getInstance().setPicCallback(this::decodeImgFile);
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

    private Uri getVideoContentUri(File imageFile) {
        final String filePath = imageFile.getAbsolutePath();
        try(Cursor cursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Video.Media._ID},
                MediaStore.Video.Media.DATA + "=? ", new String[]{filePath}, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                Uri baseUri = Uri.parse("content://media/external/images/media");
                return Uri.withAppendedPath(baseUri, "" + id);
            } else {
                if (imageFile.exists()) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Video.Media.DATA, filePath);
                    return getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                } else {
                    return null;
                }
            }
        }
    }

    private void initCameraReverse(){
        final Button camera_reverse = findViewById(R.id.camera_reverse);
        camera_reverse.setOnClickListener(v -> {
            VideoCameraManager.getInstance().switchCamera();
/*            if (!VideoCameraManager.getInstance().hasRecording()){
                VideoCameraManager.getInstance().switchCamera();
            }else Utils.showToast("recording video has been doing,Please stop recording first.");*/
        });
    }

    private void initRecordBtn(){
        mRecord = findViewById(R.id.recordBtn);
        mRecord.setCallback(new RecordBtn.ActionCallback() {
            @Override
            public void startRecord() {
                Utils.showToast("startRecord");
                XXPermissions.with(CameraSurfaceViewActivity.this)
                        .permission(Permission.RECORD_AUDIO)
                        .request(new OnPermissionCallback() {
                            @Override
                            public void onGranted(List<String> permissions, boolean all) {
                                VideoCameraManager.getInstance().recodeVideo();
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
                VideoCameraManager.getInstance().stopRecord(true);
            }

            @Override
            public void takePicture() {
                VideoCameraManager.getInstance().tackPic();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadImg();
        mRecord.postDelayed(()->{

            VideoCameraManager.getInstance().openCamera();

        },1000);

    }

    private void loadImg(){
        new Thread(() -> {
            File lastVideo = null,lastPic = null;
            File[] pics = VideoCameraManager.getInstance().getPicDir().listFiles();
            File[] videos = AbstractRecorder.getVideoDir().listFiles();

            if (videos != null && videos.length > 0){
                Arrays.sort(videos, (o1, o2) -> Long.compare(o1.lastModified(), o2.lastModified()));
                lastVideo = videos[videos.length - 1];
            }

            if (pics != null && pics.length > 0){
                Arrays.sort(pics, (o1, o2) -> Long.compare(o1.lastModified(), o2.lastModified()));
                lastPic = pics[pics.length - 1];

            }else {
                runOnUiThread(()-> mThumbnails.setImageBitmap(null));
            }

            if (lastPic != null && lastVideo != null){
                if (lastPic.lastModified() > lastVideo.lastModified()){
                    mThumbnails.setTag(getVideoContentUri(lastVideo));
                    final Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(lastVideo.getPath(), MediaStore.Images.Thumbnails.MINI_KIND);
                    runOnUiThread(()-> mThumbnails.setImageBitmap(bitmap));
                }else {
                    decodeImgFile(lastPic);
                }
            }else if (lastPic != null){
                decodeImgFile(lastPic);
            }else if (lastVideo != null){
                mThumbnails.setTag(getVideoContentUri(lastVideo));
                final Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(lastVideo.getPath(), MediaStore.Images.Thumbnails.MINI_KIND);
                runOnUiThread(()-> mThumbnails.setImageBitmap(bitmap));
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
    protected void onStop(){
        super.onStop();
        VideoCameraManager.getInstance().setPicCallback(null);
        mRecord.stopRecord();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int getContentLayoutId() {
        return R.layout.activity_camera_surface_view;
    }

    public static void start(Activity c){
        c.startActivity(new Intent(c,CameraSurfaceViewActivity.class));
    }
}