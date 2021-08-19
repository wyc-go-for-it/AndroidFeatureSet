package com.android.wyc.aidlservicedemo;

import android.app.Application;

import com.android.wyc.aidlservicedemo.Database.DBHelper;

public class CustomApplication extends Application {
    private DBHelper mDBHelper;
    @Override
    public void onCreate() {
        super.onCreate();

        mDBHelper = DBHelper.getInstance(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        mDBHelper.close();
    }
}
