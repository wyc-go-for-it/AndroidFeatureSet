package com.wyc.video.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.wyc.video.R;
import com.wyc.video.camera.CameraGLRenderer;
import com.wyc.video.camera.GLVideoCameraManager;
import com.wyc.video.camera.ICamera;
import com.wyc.video.camera.VideoCameraManager;

import java.nio.ByteBuffer;

public class CameraGLSurfaceViewActivity extends BaseCameraViewActivity {
    private CameraGLRenderer mCameraGLRenderer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMiddleText(getString(R.string.useGlSurfaceView));
        initGlSurface();

    }


    private void initGlSurface(){
        final GLSurfaceView preview_surface = findViewById(R.id.preview_surface);
        mCameraGLRenderer = new CameraGLRenderer();
        preview_surface.setEGLContextClientVersion(3);
        preview_surface.setRenderer(mCameraGLRenderer);

        final ImageView imageView = findViewById(R.id.imageView);

        mCameraGLRenderer.setReadPixelListener(new CameraGLRenderer.OnReadPixel() {
            @Override
            public void onRead(int w, int h, @NonNull byte[] byteBuffer) {
                final Bitmap bitmap = Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(byteBuffer));
                runOnUiThread(new Runnable() {
                    @SuppressLint("ResourceType")
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bitmap);
                    }
                });

            }
        });
    }


    @Override
    public int getContentLayoutId() {
        return R.layout.activity_camera_glsurface_view;
    }

    public static void start(Activity c){
        c.startActivity(new Intent(c,CameraGLSurfaceViewActivity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraGLRenderer != null){
            mCameraGLRenderer.clear();
        }
    }

    @Override
    protected ICamera generateCamera() {
        return GLVideoCameraManager.getInstance();
    }
}