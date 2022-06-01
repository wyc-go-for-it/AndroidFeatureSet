package com.wyc.video.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.wyc.video.R;


public class VideoRelatedActivity extends BaseActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMiddleText(getString(R.string.video_related));
        findViewById(R.id.surfaceView).setOnClickListener(this);
    }

    @Override
    public int getContentLayoutId() {
        return R.layout.video_activity_main;
    }

    public static void start(Context context){
        context.startActivity(new Intent(context, VideoRelatedActivity.class));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.surfaceView) {
            CameraSurfaceViewActivity.start(this);
        }
    }
}