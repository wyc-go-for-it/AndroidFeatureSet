package com.wyc.androidfeatureset.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.wyc.androidfeatureset.R;
import com.wyc.androidfeatureset.camera.CaptureActivity;
import com.wyc.androidfeatureset.provider.ProviderActivity;
import com.wyc.label.DataItem;
import com.wyc.label.LabelPrintSettingActivity;
import com.wyc.label.printer.LabelPrintUtils;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        LabelPrintUtils.openPrint();
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
    @OnClick(R.id.label_print)
    void print(){
        LabelPrintUtils.print(DataItem.testGoods());
    }
}