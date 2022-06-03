package com.android.wyc.aidlservicedemo;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class ServiceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);
        moveTaskToBack(isFinishing());
        //hideDesktopIco();
        startMyService();
    }
    @Override
    public void onResume(){
        super.onResume();
        moveTaskToBack(isFinishing());
    }

    private void startMyService() {
        startService(new Intent(this,StudentManagerService.class));
    }
    private void hideDesktopIco(){
        PackageManager p = getPackageManager();
        p.setComponentEnabledSetting(getComponentName(), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }
}
