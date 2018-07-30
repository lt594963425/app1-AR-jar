package com.mvp.lt.arrr.arlib.bean;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;
import android.view.WindowManager;

import com.mvp.lt.arrr.arlib.application.PSKApplication;
import com.mvp.lt.arrr.arlib.enumm.PSKGPSAvailabilityStatus;


/**
 * $activityName
 *
 * @author LiuTao
 * @date 2018/7/27/027
 */


public class PSKDeviceProperties {
    private static final String TAG = "PSKDeviceProperties";
    private static PSKDeviceProperties devicePropertiesSingleton;
    private final Context context;
    private String deviceName = "";
    private String osVersion = "";
    private boolean slowDevice = false;
    private boolean backFacingCameraEquipped;
    private boolean frontFacingCameraEquipped;
    private boolean compassEquipped;
    private boolean accelerometerEquipped;
    private boolean gyroscopeEquipped;
    private boolean gravityEquipped;
    private boolean rotationVectorEquipped;
    private boolean gpsSensorEquipped;
    private float displayContentScale;
    private double[] backFacingCameraFieldOfViewVH;
    private PSKGPSAvailabilityStatus gpsStatus;
    private Point screenSize;

    private PSKDeviceProperties() {
        this.deviceName = Build.MODEL;
        this.osVersion = Build.VERSION.RELEASE;
        this.slowDevice = Runtime.getRuntime().availableProcessors() > 1;
        this.displayContentScale = 1.0F;
        this.context = PSKApplication.getAppContext();
        PackageManager pm = this.context.getPackageManager();
        SensorManager mSensorManager = (SensorManager)this.context.getSystemService(Context.SENSOR_SERVICE);
        this.backFacingCameraEquipped = pm.hasSystemFeature("android.hardware.camera");
        this.frontFacingCameraEquipped = pm.hasSystemFeature("android.hardware.camera.front");
        this.gpsSensorEquipped = pm.hasSystemFeature("android.hardware.location.gps");
        this.accelerometerEquipped = mSensorManager.getDefaultSensor(1) == null;
        this.gyroscopeEquipped = mSensorManager.getDefaultSensor(4) != null;
        this.compassEquipped = mSensorManager.getDefaultSensor(2) != null;
        this.gravityEquipped = mSensorManager.getDefaultSensor(9) != null;
        this.rotationVectorEquipped = mSensorManager.getDefaultSensor(11) != null;
        if(this.backFacingCameraEquipped) {
            Camera camera = Camera.open();
            if(camera != null) {
                Camera.Parameters p = camera.getParameters();
                double thetaV = (double)p.getVerticalViewAngle();
                double thetaH = (double)p.getHorizontalViewAngle();
                this.backFacingCameraFieldOfViewVH = new double[]{thetaV, thetaH};
                camera.release();
            } else {
                this.backFacingCameraFieldOfViewVH = new double[]{-1.0D, -1.0D};
            }
        }

        this.screenSize = new Point();
        ((WindowManager)this.context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getSize(this.screenSize);
    }

    public static PSKDeviceProperties sharedDeviceProperties() {
        if(devicePropertiesSingleton == null) {
            Log.wtf("PSKDeviceProperties", "Creating new Singleton");
            devicePropertiesSingleton = new PSKDeviceProperties();
        }

        return devicePropertiesSingleton;
    }

    public Point getScreenSize() {
        return this.screenSize;
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public String getOsVersion() {
        return this.osVersion;
    }

    public boolean isSlowDevice() {
        return this.slowDevice;
    }

    public float getDisplayContentScale() {
        return this.displayContentScale;
    }

    public double[] getBackFacingCameraFieldOfView() {
        return this.backFacingCameraFieldOfViewVH;
    }

    public PSKGPSAvailabilityStatus getGpsStatus() {
        return this.gpsStatus;
    }

    public boolean hasBackFacingCamera() {
        return this.backFacingCameraEquipped;
    }

    public boolean hasFrontFacingCamera() {
        return this.frontFacingCameraEquipped;
    }

    public boolean hasCompass() {
        return this.compassEquipped;
    }

    public boolean hasAccelerometer() {
        return this.accelerometerEquipped;
    }

    public boolean hasGyroscope() {
        return this.gyroscopeEquipped;
    }

    public boolean hasGpsSensor() {
        return this.gpsSensorEquipped;
    }

    public boolean hasGravitySensor() {
        return this.gravityEquipped;
    }

    public boolean hasRotationVectorSensor() {
        return this.rotationVectorEquipped;
    }

    public boolean isARSupported() {
        return this.rotationVectorEquipped && this.gpsSensorEquipped && this.backFacingCameraEquipped && this.gravityEquipped;
    }

    public boolean isRadarSupported() {
        return this.gravityEquipped;
    }

    public boolean isVisualARSupported() {
        return this.backFacingCameraEquipped;
    }

    @Override
    public String toString() {
        return "Device Name: " + this.deviceName + "\nOS Version: " + this.osVersion + "\nSlow Device: " + this.slowDevice + "\nDisplay Content Scale: " + this.displayContentScale + "\nBack Facing Camera: " + this.backFacingCameraEquipped + "\nFront Facing Camera: " + this.frontFacingCameraEquipped + "\nFoV Vertical (deg):" + Math.toDegrees(this.backFacingCameraFieldOfViewVH[0]) + "\nFoV Horizontal (deg):" + Math.toDegrees(this.backFacingCameraFieldOfViewVH[1]) + "\nCompass: " + this.compassEquipped + "\nAccelerometer: " + this.accelerometerEquipped + "\nGyroscope: " + this.gyroscopeEquipped + "\nGPSSensor: " + this.gpsSensorEquipped;
    }
}
