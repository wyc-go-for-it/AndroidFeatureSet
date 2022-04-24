package com.wyc.androidfeatureset.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.wyc.androidfeatureset.R;
import com.wyc.androidfeatureset.camera.CaptureActivity;
import com.wyc.androidfeatureset.provider.ProviderActivity;
import com.wyc.label.LabelDesignActivity;
import com.wyc.label.LabelPrintSettingActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }
    @OnClick(R.id.camera_feature_btn)
    void camera(){
        CaptureActivity.start(this);
    }
    @OnClick(R.id.provider_btn)
    void provider(){
        ProviderActivity.start(this);
    }
    @OnClick(R.id.label_design)
    void label(){
        LabelPrintSettingActivity.start(this);
    }
}