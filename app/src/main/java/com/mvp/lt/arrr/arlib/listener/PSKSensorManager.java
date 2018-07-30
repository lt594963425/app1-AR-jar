package com.mvp.lt.arrr.arlib.listener;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;

import com.mvp.lt.arrr.arlib.bean.PSKDeviceProperties;
import com.mvp.lt.arrr.arlib.application.PSKApplication;
import com.mvp.lt.arrr.arlib.enumm.PSKDeviceOrientation;

import java.util.concurrent.TimeUnit;

/**
 * $activityName
 *
 * @author LiuTao
 * @date 2018/7/27/027
 */


public class PSKSensorManager implements PSKEventListener {
    private static final String TAG = "PSKSensorManager";
    private static PSKSensorManager sensorManagerSingleton = null;
    private Context context = PSKApplication.getAppContext();
    private float headingAccuracyMin = -1.0F;
    private float headingAccuracyMax = -1.0F;
    private SensorManager sensorManager = null;
    private boolean isGPSEnabled;
    private boolean isWLANEnabled;
    private PSKDeviceAttitude _deviceAttitude;
    private boolean hasSimulatedLocation = false;
    private Location simulatedLocation = null;
    private Location lastRealLocation = null;
    private int sensorDelayRotationVector = 1;
    private int sensorDelayGravity = 1;
    private boolean hasSimulatedRotation = false;
    private float[] simulatedRotation = null;
    private PSKEventListener eventListener;
    private PSKSensorManagerListener sensorListener;
    private LocationManager locationManager;
    private static final long LOCATIONMANAGER_MIN_DISTANCE_CHANGE_FOR_UPDATES = 1L;
    private static final long LOCATIONMANAGER_MIN_TIME_BW_UPDATES = 500L;
    private final SensorEventListener internalSensorListener;
    private final GpsStatus.Listener gpsStatusListener;
    private final OrientationEventListener orientationListener;
    private final LocationListener internalLocationListener;

    public PSKSensorManager() {
        this.locationManager = (LocationManager)this.context.getSystemService(Context.LOCATION_SERVICE);
        this.internalSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if(PSKSensorManager.this._deviceAttitude != null) {
                    switch(event.sensor.getType()) {
                        case 9:
                            PSKSensorManager.this._deviceAttitude.onGravityChanged(event.values, event.timestamp);
                            if(PSKSensorManager.this.sensorListener != null) {
                                PSKSensorManager.this.sensorListener.onGravityChanged(event.values, event.timestamp);
                            }
                            break;
                        case 11:
                            float[] values = event.values;
                            if(PSKSensorManager.this.hasSimulatedRotation) {
                                values = PSKSensorManager.this.simulatedRotation;
                            }

                            if(values.length > 4) {
                                if(PSKSensorManager.this.headingAccuracyMin == -1.0F || PSKSensorManager.this.headingAccuracyMin > values[4]) {
                                    PSKSensorManager.this.headingAccuracyMin = values[4];
                                }

                                if(PSKSensorManager.this.headingAccuracyMax == -1.0F || PSKSensorManager.this.headingAccuracyMax < values[4]) {
                                    PSKSensorManager.this.headingAccuracyMax = values[4];
                                }
                            }

                            PSKSensorManager.this._deviceAttitude.onRotationVectorChanged(values, event.timestamp);
                            if(PSKSensorManager.this.sensorListener != null) {
                                PSKSensorManager.this.sensorListener.onRotationVectorChanged(values, event.timestamp);
                            }
                    }
                }

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                if(PSKSensorManager.this._deviceAttitude != null) {
                    switch(sensor.getType()) {
                        case 9:
                            PSKSensorManager.this._deviceAttitude.onGravityAccuracyChanged(accuracy);
                            if(PSKSensorManager.this.sensorListener != null) {
                                PSKSensorManager.this.sensorListener.onGravityAccuracyChanged(accuracy);
                            }
                            break;
                        case 11:
                            PSKSensorManager.this._deviceAttitude.onRotationVectorAccuracyChanged(accuracy);
                            if(PSKSensorManager.this.sensorListener != null) {
                                PSKSensorManager.this.sensorListener.onRotationVectorAccuracyChanged(accuracy);
                            }
                    }
                }

            }
        };
        this.gpsStatusListener = new GpsStatus.Listener() {
            @Override
            public void onGpsStatusChanged(int i) {
                PSKSensorManager.this._deviceAttitude.onGpsStatusChanged(i);
                if(PSKSensorManager.this.sensorListener != null) {
                    PSKSensorManager.this.sensorListener.onGpsStatusChanged(i);
                }

            }
        };
        this.orientationListener = new OrientationEventListener(this.context) {
            @Override
            public void onOrientationChanged(int orientation) {
                PSKSensorManager.this._deviceAttitude.onOrientationChanged(orientation);
                if(PSKSensorManager.this.sensorListener != null) {
                    PSKSensorManager.this.sensorListener.onOrientationChanged(orientation);
                }

            }
        };
        this.internalLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                PSKSensorManager.this.lastRealLocation = location;
                if(PSKSensorManager.this.hasSimulatedLocation) {
                    location = PSKSensorManager.this.simulatedLocation;
                }

                PSKSensorManager.this._deviceAttitude.onLocationChanged(location);
                PSKSensorManager.sensorManagerSingleton.onLocationChangedEvent(location);
                if(PSKSensorManager.this.sensorListener != null) {
                    PSKSensorManager.this.sensorListener.onLocationChanged(location);
                }

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
                PSKSensorManager.this._deviceAttitude.onStatusChanged(s, i, bundle);
                if(PSKSensorManager.this.sensorListener != null) {
                    PSKSensorManager.this.sensorListener.onStatusChanged(s, i, bundle);
                }

            }

            @Override
            public void onProviderEnabled(String s) {
                PSKSensorManager.this._deviceAttitude.onProviderEnabled(s);
                if(PSKSensorManager.this.sensorListener != null) {
                    PSKSensorManager.this.sensorListener.onProviderEnabled(s);
                }

            }

            @Override
            public void onProviderDisabled(String s) {
                PSKSensorManager.this._deviceAttitude.onProviderDisabled(s);
                if(PSKSensorManager.this.sensorListener != null) {
                    PSKSensorManager.this.sensorListener.onProviderDisabled(s);
                }

            }
        };
        this.sensorManager = (SensorManager)this.context.getSystemService(Context.SENSOR_SERVICE);
        this._deviceAttitude = PSKDeviceAttitude.sharedDeviceAttitude();
    }

    public static PSKSensorManager getSharedSensorManager() {
        if(sensorManagerSingleton == null) {
            Log.wtf("PSKSensorManager", "Creating new Singleton");
            sensorManagerSingleton = new PSKSensorManager();
        }

        return sensorManagerSingleton;
    }

    public float getHeadingAccuracyMin() {
        return this.headingAccuracyMin;
    }

    public float getHeadingAccuracyMax() {
        return this.headingAccuracyMax;
    }

    public PSKEventListener getEventListener() {
        return this.eventListener;
    }

    public void setEventListener(PSKEventListener eventListener) {
        this.eventListener = eventListener;
    }

    public int getSensorDelayRotationVector() {
        return this.sensorDelayRotationVector;
    }

    public int getSensorDelayGravity() {
        return this.sensorDelayGravity;
    }

    public void setSensorDelayRotationVector(int delay) {
        this.sensorDelayRotationVector = delay;
    }

    public void setSensorDelayGravity(int delay) {
        this.sensorDelayGravity = delay;
    }

    public void setSensorDelayAllSensors(int delay) {
        this.sensorDelayGravity = delay;
        this.sensorDelayRotationVector = delay;
    }

    @Override
    public void onLocationChangedEvent(Location location) {
        if(this.eventListener != null) {
            this.eventListener.onLocationChangedEvent(location);
        }

    }

    @Override
    public void onDeviceOrientationChanged(PSKDeviceOrientation newOrientation) {
        if(this.eventListener != null) {
            this.eventListener.onDeviceOrientationChanged(newOrientation);
        }

    }

    public boolean startListening() {
        if(PSKDeviceProperties.sharedDeviceProperties().isARSupported() && PSKDeviceProperties.sharedDeviceProperties().hasGravitySensor()) {
            Log.wtf("PSKSensorManager", "Registering Gravity...");
            this.sensorManager.registerListener(this.internalSensorListener, this.sensorManager.getDefaultSensor(9), this.sensorDelayGravity);
            Log.wtf("PSKSensorManager", "Registering RotationVector...");
            this.sensorManager.registerListener(this.internalSensorListener, this.sensorManager.getDefaultSensor(11), this.sensorDelayRotationVector);
            Log.wtf("PSKSensorManager", "Starting Orientation Listener...");
            this.orientationListener.enable();
            Log.wtf("PSKSensorManager", "Registering GPS...");
            return this.startLocationServices();
        } else {
            Log.e("PSKSensorManager", "AR NOT SUPPORTED!");
            return false;
        }
    }

    public void stopListening() {
        this.sensorManager.unregisterListener(this.internalSensorListener);
        Log.wtf("PSKSensorManager", "Stopping Orientation Listener");
        this.orientationListener.disable();
        Log.wtf("PSKSensorManager", "Stopping Location Service");
        this.stopLocationServices();
    }

    private boolean startLocationServices() {
        try {
            this.isGPSEnabled = this.locationManager.isProviderEnabled("gps");
            this.isWLANEnabled = this.locationManager.isProviderEnabled("network");
            if(!this.isGPSEnabled && !this.isWLANEnabled) {
                return false;
            } else {
                if(this.isWLANEnabled) {
                    this.locationManager.requestLocationUpdates("network", 500L, 1.0F, this.internalLocationListener);
                }

                if(this.isGPSEnabled) {
                    this.locationManager.requestLocationUpdates("gps", 500L, 1.0F, this.internalLocationListener);
                }

                this.locationManager.addGpsStatusListener(this.gpsStatusListener);
                return true;
            }
        } catch (SecurityException var2) {
            Log.wtf("PSKSensorManager", "No permission granted for LocationManager.GPS_PROVIDER");
            return false;
        } catch (Exception var3) {
            Log.wtf("PSKSensorManager", "Exception: " + var3.getLocalizedMessage());
            return false;
        }
    }

    private void stopLocationServices() {
        this.locationManager.removeUpdates(this.internalLocationListener);
    }

    protected void setSimulatedLocation(double latitude, double longitude, double altitude) {
        this.hasSimulatedLocation = true;
        this.simulatedLocation = new Location("Simulated Location");
        this.simulatedLocation.setLatitude(latitude);
        this.simulatedLocation.setLongitude(longitude);
        this.simulatedLocation.setAltitude(altitude);
        this.simulatedLocation.setAccuracy(20.0F);
        this.simulatedLocation.setTime(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()));
        this.internalLocationListener.onLocationChanged(this.simulatedLocation);
    }

    public void clearSimulatedLocation() {
        this.hasSimulatedLocation = false;
        this.simulatedLocation = null;
        if(this.lastRealLocation != null) {
            this.internalLocationListener.onLocationChanged(this.lastRealLocation);
        }

    }

    protected void setSimulatedLocation(float[] rotation) {
        this.hasSimulatedRotation = true;
        this.simulatedRotation = rotation;
    }

    public void clearSimulatedRotation() {
        this.hasSimulatedRotation = false;
        this.simulatedRotation = null;
    }

    public PSKSensorManagerListener getSensorListener() {
        return this.sensorListener;
    }

    public void setSensorListener(PSKSensorManagerListener sensorListener) {
        this.sensorListener = sensorListener;
    }

    public LocationManager getLocationManager() {
        return this.locationManager;
    }

    public boolean isGPSEnabled() {
        return this.isGPSEnabled;
    }
}

