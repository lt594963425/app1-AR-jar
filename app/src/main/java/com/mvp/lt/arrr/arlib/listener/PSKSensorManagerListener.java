package com.mvp.lt.arrr.arlib.listener;

import android.location.GpsStatus;
import android.location.LocationListener;

/**
 * $activityName
 *
 * @author LiuTao
 * @date 2018/7/27/027
 */


public interface PSKSensorManagerListener extends LocationListener, GpsStatus.Listener {
    void onRotationVectorChanged(float[] var1, long var2);

    void onRotationVectorAccuracyChanged(int var1);

    void onGravityChanged(float[] var1, long var2);

    void onGravityAccuracyChanged(int var1);

    void onOrientationChanged(int var1);
}

