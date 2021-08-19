package com.android.wyc.notificationdemo;

import android.app.Application;
import android.os.StrictMode;

import com.squareup.leakcanary.LeakCanary;

public class CustomApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannels.createAllNotificationChannels(this);
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
        }
        LeakCanary.install(this);
    }
}
