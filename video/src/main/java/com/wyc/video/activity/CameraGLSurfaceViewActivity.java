package com.wyc.video.activity;

import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.SurfaceView;

import com.wyc.video.R;
import com.wyc.video.camera.CameraGLRenderer;
import com.wyc.video.camera.GLVideoCameraManager;
import com.wyc.video.camera.ICamera;
import com.wyc.video.camera.VideoCameraManager;

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