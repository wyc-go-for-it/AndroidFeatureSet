package com.wyc.video.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.wyc.video.R;
import com.wyc.video.camera.ICamera;
import com.wyc.video.camera.VideoCameraManager;

public final class CameraSurfaceViewActivity extends BaseCameraViewActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMiddleText(getString(R.string.useSurfaceView));
    }

    @Override
    protected ICamera generateCamera() {
        return VideoCameraManager.getInstance();
    }

    @Override
    public int getContentLayoutId() {
        return R.layout.activity_camera_surface_view;
    }

    public static void start(Activity c){
        c.startActivity(new Intent(c,CameraSurfaceViewActivity.class));
    }
}