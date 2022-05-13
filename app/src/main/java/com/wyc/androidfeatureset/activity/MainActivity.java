package com.wyc.androidfeatureset.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.wyc.androidfeatureset.R;
import com.wyc.androidfeatureset.SM2.SM2;
import com.wyc.androidfeatureset.SM2.SM2Demo;
import com.wyc.androidfeatureset.camera.CaptureActivity;
import com.wyc.androidfeatureset.provider.ProviderActivity;
import com.wyc.label.LabelPrintSettingActivity;
import com.wyc.label.printer.LabelPrintUtils;
import com.wyc.logger.Logger;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            if (!Environment.isExternalStorageManager()){
                final Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        }else
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

    @OnClick(R.id.sm2)
    void sing(){
        SM2 clz = SM2.getInstance();
        byte [] sourceData = "8003 300370100000013 2022051010180188620220510 20220510101801 0.01 100370100000697".getBytes();

        /* 密管密钥测试 */
        String x = "95510BADCE29F70BE07DF6E2B0CE75BE124A56C08E82435E72B4AA6C17679F45";
        String y = "5A6892AADDE2A6B7A58CA7B0E10CA78D3811FF27E9F728CD80D53C1B9A6461DB";
        String d = "D3F24D61BB2816882B8474B778DD7C3166D665F9455DC9D551C989C161E76AB0";

        {   // 密管密钥自签自验
            byte[] sign = clz.SM2Sign(SM2Demo.hexStringToBytes(d), sourceData);
            Logger.d("SignData：" + new String(sign));
            boolean verify = clz.SM2Verify(SM2Demo.hexStringToBytes(x), SM2Demo.hexStringToBytes(y), sourceData, sign);
            Logger.d("VerifyResult：" + verify);
        }
    }
}