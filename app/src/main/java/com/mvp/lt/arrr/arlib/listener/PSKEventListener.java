package com.mvp.lt.arrr.arlib.listener;

import android.location.Location;

import com.mvp.lt.arrr.arlib.enumm.PSKDeviceOrientation;


/**
 * $activityName
 *
 * @author LiuTao
 * @date 2018/7/27/027
 */


public interface PSKEventListener {
    void onLocationChangedEvent(Location var1);

    void onDeviceOrientationChanged(PSKDeviceOrientation var1);
}
