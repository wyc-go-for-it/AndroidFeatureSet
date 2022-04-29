package com.wyc.androidfeatureset.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.wyc.androidfeatureset.R;
import com.wyc.androidfeatureset.camera.CaptureActivity;
import com.wyc.androidfeatureset.provider.ProviderActivity;
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
        LabelPrintUtils.openPrinter();
    }

    @Override
    public void onResume(){
        super.onResume();
        checkSelfPermission();
    }

    private void checkSelfPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if ((ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }
        }
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