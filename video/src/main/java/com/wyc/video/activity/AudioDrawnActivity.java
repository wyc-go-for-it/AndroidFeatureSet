package com.wyc.video.activity;

import androidx.annotation.NonNull;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Choreographer;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;

import com.wyc.logger.Logger;
import com.wyc.permission.OnPermissionCallback;
import com.wyc.permission.Permission;
import com.wyc.permission.XXPermissions;
import com.wyc.video.R;
import com.wyc.video.opengl.MyGLRenderer;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class AudioDrawnActivity extends BaseActivity {

    private final int audioSource = MediaRecorder.AudioSource.MIC;
    private final int sampleRateInHz = 48000;
    private final int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
    private final int audioFormat = AudioFormat.ENCODING_PCM_FLOAT;

    private AudioRecord mAudioRecord;
    private volatile boolean isStart = false;
    private SurfaceHolder mSurfaceHolder;

    private final int size = 1024;
    private final float[][] mBufferContainer = new float[][]{ new float[size],new float[size]};
    private float[] mBuffer;

    private int mWidth = 0;
    private int mHeight = 0;
    private final ReentrantLock mLock = new ReentrantLock(true);
    private final Condition mEmpty = mLock.newCondition();
    private volatile boolean isEmpty = true;


    private final int showTimeLen = 2;//(s)
    private float showStep = 1.0f;
    private final MyGLRenderer myGLRenderer = new MyGLRenderer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMiddleText(getString(R.string.drawAudio));
        initStart();
        initDraw();
        initGlView();
    }
    private void initGlView(){
        final GLSurfaceView glSurfaceView = findViewById(R.id.gl_view);
        glSurfaceView.setEGLContextClientVersion(3);
        glSurfaceView.setRenderer(myGLRenderer);
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
                mBuffer = new float[sampleRateInHz * showTimeLen];

                showStep = 1f/ ((float)mBuffer.length / (float) mWidth) * 2f;

                Logger.d("showStep:%f",showStep);
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
        if (mAudioRecord != null && !isStart){
            isStart = true;

            new Thread(() -> {

                mAudioRecord.startRecording();

                while (isStart){
                    final float[] buffer = mBufferContainer[1];
                     mAudioRecord.read(buffer,0,size, AudioRecord.READ_BLOCKING);

                    try {
                        mLock.lock();
                        while (isStart && !isEmpty){
                            mEmpty.awaitUninterruptibly();
                        }

                        final float[] t = mBufferContainer[0];
                        mBufferContainer[0] = mBufferContainer[1];
                        mBufferContainer[1] = t;

                        myGLRenderer.updateData(mBufferContainer[0]);

                        isEmpty = false;
                        mEmpty.signalAll();
                    } finally {
                        mLock.unlock();
                    }

                }
            }).start();

            new Thread(() -> {

                final Paint paint = new Paint();
                paint.setColor(Color.RED);
                float index;

                float h = mHeight * 0.5f;
                float l_h = h * 0.3333f;
                float r_h = h * 0.999f;

                while (isStart){
                    final Canvas canvas = mSurfaceHolder.lockHardwareCanvas();
                    if (canvas == null)break;

                    canvas.drawColor(Color.BLACK );

                    try {
                        mLock.lock();
                        while (isStart && isEmpty){
                            mEmpty.awaitUninterruptibly();
                        }

                        combine();

                        index = 0;

                        for (int i = 0,l = 0,r = 1;i < (mBuffer.length >> 1);i ++,l+=2,r+=2){
                            index += showStep;

                            paint.setColor(Color.RED);
                            canvas.drawPoint(index, l_h + mBuffer[l] * 500, paint);

                            paint.setColor(Color.GREEN);
                            canvas.drawPoint(index, r_h + mBuffer[r] * 500, paint);

                            if (!isStart) break;

                        }

                        isEmpty = true;
                        mEmpty.signalAll();

                    } finally {
                        mLock.unlock();
                    }

                    mSurfaceHolder.unlockCanvasAndPost(canvas);
                }

            }).start();

        }
    }
    private void combine(){
        final float[] data = mBufferContainer[0];

        int size = mBuffer.length;
        if (data.length >= size){
            System.arraycopy(data,0,mBuffer,0,size);
        }else {
             int diff = size - data.length;
            System.arraycopy(mBuffer,size - diff,mBuffer,0,diff);
            System.arraycopy(data,0,mBuffer,diff,data.length);
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

        try {
            mLock.lock();
            mEmpty.signalAll();

        }finally {
            mLock.unlock();
        }
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