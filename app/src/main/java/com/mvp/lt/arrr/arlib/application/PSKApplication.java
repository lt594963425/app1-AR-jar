package com.mvp.lt.arrr.arlib.application;

import android.app.Application;
import android.content.Context;

/**
 * $activityName
 *
 * @author LiuTao
 * @date 2018/7/27/027
 */


public class PSKApplication extends Application {
    private static Context context;

    public PSKApplication() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this.getApplicationContext();
    }

    public static Context getAppContext() {
        return context;
    }
}
