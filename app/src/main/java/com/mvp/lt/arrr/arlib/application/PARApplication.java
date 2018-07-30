package com.mvp.lt.arrr.arlib.application;


import com.mvp.lt.arrr.arlib.control.PARController;

/**
 * $activityName
 *
 * @author LiuTao
 * @date 2018/7/27/027
 */


public class PARApplication extends PSKApplication {
    public PARApplication() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PARController.getInstance().init(PSKApplication.getAppContext());
    }

    public String setApiKey() {
        return "Override the setApiKey method in your PARApplication class!";
    }
}
