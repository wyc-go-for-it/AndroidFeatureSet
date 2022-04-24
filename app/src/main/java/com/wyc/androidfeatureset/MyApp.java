package com.wyc.androidfeatureset;

import android.app.Application;
import android.os.StrictMode;

import com.squareup.leakcanary.LeakCanary;
import com.wyc.label.App;
import com.wyc.logger.AndroidLogAdapter;
import com.wyc.logger.Logger;

/**
 * @ProjectName: AndroidFeatureSet
 * @Package: com.wyc.androidfeatureset
 * @ClassName: MyApp
 * @Description: application
 * @Author: wyc
 * @CreateDate: 2021-06-09 17:37
 * @UpdateUser: 更新者
 * @UpdateDate: 2021-06-09 17:37
 * @UpdateRemark: 更新说明
 * @Version: 1.0
 */
public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
        }
        App.initApp(this);
        LeakCanary.install(this);
        Logger.addLogAdapter(new AndroidLogAdapter());
    }
}
