package com.wyc.video.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.wyc.video.R;

public class VideoRelatedActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMiddleText(getString(R.string.video_related));
    }

    @Override
    public int getContentLayoutId() {
        return R.layout.video_activity_main;
    }

    public static void start(Context context){
        context.startActivity(new Intent(context, VideoRelatedActivity.class));
    }
}