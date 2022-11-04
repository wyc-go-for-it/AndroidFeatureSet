package com.wyc.video.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.wyc.logger.Logger;
import com.wyc.permission.OnPermissionCallback;
import com.wyc.permission.Permission;
import com.wyc.permission.XXPermissions;
import com.wyc.video.R;
import com.wyc.video.camera.VideoCameraManager;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

public class AudioDrawnActivity extends BaseActivity {

    private final int audioSource = MediaRecorder.AudioSource.MIC;
    private final int sampleRateInHz = 44100;
    private final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private final int audioFormat = AudioFormat.ENCODING_PCM_FLOAT;

    private AudioRecord mAudioRecord;
    private volatile boolean isStart = false;
    private SurfaceHolder mSurfaceHolder;

    private final int size = 128;
    private final float[] mBuffer = new float[size];
    private int mWidth = 0;
    private int mHeight = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMiddleText(getString(R.string.tree_menu));
        initStart();
        initDraw();
    }

    private void initDraw(){
        final SurfaceView surfaceView = findViewById(R.id.surfaceView);
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                mWidth = width;
                mHeight = height;
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });
    }

    private void initStart(){
        final Button button = findViewById(R.id.start);
        button.setOnClickListener(v -> startAudioRecord());
    }

    private void startAudioRecord(){
        if (mAudioRecord != null){
            new Thread(() -> {
                isStart = true;

                mAudioRecord.startRecording();

                final Paint paint = new Paint();
                paint.setColor(Color.RED);
                int index = 0;

                while (isStart){
                    mAudioRecord.read(mBuffer,0,size, AudioRecord.READ_BLOCKING);

                    final Canvas canvas = mSurfaceHolder.lockCanvas();
                    if (canvas == null)break;

                    canvas.drawColor(Color.BLACK );
                    for (float f : mBuffer){
                        canvas.drawPoint(index ++,(mHeight >> 1) + f * 1000,paint);

                        if (!isStart)break;

                        if (index > mWidth){
                            index = 0;
                        }
                    }

                    mSurfaceHolder.unlockCanvasAndPost(canvas);

                }

            }).start();

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        XXPermissions.with(this)
                .permission(Permission.RECORD_AUDIO)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        mAudioRecord = createAudio();
                    }
                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        if (never){

                        }
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        isStart = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAudioRecord != null){
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }

    private AudioRecord createAudio() {
        int bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        try {
            return new AudioRecord.Builder().setAudioSource(audioSource).setAudioFormat(
                    new AudioFormat.Builder().setEncoding(audioFormat)
                            .setSampleRate(sampleRateInHz).setChannelMask(channelConfig).build()).setBufferSizeInBytes(bufferSizeInBytes).build();

        }catch (SecurityException e){
            e.printStackTrace();
        }
        return null;
    }

    public static void start(Context context){
        context.startActivity(new Intent(context, AudioDrawnActivity.class));
    }

    @Override
    public int getContentLayoutId() {
        return R.layout.activity_audio_drawn;
    }
}