package com.wyc.video.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;

import com.wyc.video.R;
import com.wyc.video.recorder.AudioTool;

public class AudioActivity extends BaseActivity {
    private final AudioTool audioTool = new AudioTool();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMiddleText(getString(R.string.recording_play));
        initView();
    }

    private void initView(){
        final CheckBox record = findViewById(R.id.record);
        final CheckBox playback = findViewById(R.id.playback);
        record.setOnCheckedChangeListener((buttonView, isChecked) -> audioTool.recordingAudio(isChecked));
        playback.setOnCheckedChangeListener((buttonView, isChecked) -> audioTool.playingAudio(isChecked));
    }

    @Override
    protected void onResume() {
        super.onResume();
        audioTool.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        audioTool.stop();
    }

    @Override
    public int getContentLayoutId() {
        return R.layout.activity_audio;
    }

    public static void start(Context context){
        context.startActivity(new Intent(context, AudioActivity.class));
    }
}