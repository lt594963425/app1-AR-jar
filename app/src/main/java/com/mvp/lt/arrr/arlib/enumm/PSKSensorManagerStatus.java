package com.mvp.lt.arrr.arlib.enumm;

/**
 * $activityName
 *
 * @author LiuTao
 * @date 2018/7/27/027
 */



public enum PSKSensorManagerStatus {
    PSKSensorManagerUnavailable,
    PSKSensorManagerIdle,
    PSKSensorManagerRestricted,
    PSKSensorManagerDenied,
    PSKSensorManagerRunning;

    private PSKSensorManagerStatus() {
    }
}