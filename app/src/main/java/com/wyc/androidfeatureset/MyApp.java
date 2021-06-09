package com.wyc.androidfeatureset;

import android.app.Application;

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
        Logger.addLogAdapter(new AndroidLogAdapter());
    }
}
