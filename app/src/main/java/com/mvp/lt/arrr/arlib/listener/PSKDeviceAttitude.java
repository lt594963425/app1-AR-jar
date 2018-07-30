package com.mvp.lt.arrr.arlib.listener;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.location.Location;
import android.opengl.Matrix;
import android.os.Bundle;
import android.renderscript.Matrix4f;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.mvp.lt.arrr.arlib.utils.PSKMath;
import com.mvp.lt.arrr.arlib.utils.PSKUtils;
import com.mvp.lt.arrr.arlib.bean.PSKVector3;
import com.mvp.lt.arrr.arlib.application.PSKApplication;
import com.mvp.lt.arrr.arlib.enumm.PSKDeviceOrientation;

import java.util.concurrent.TimeUnit;

/**
 * $activityName
 *
 * @author LiuTao
 * @date 2018/7/27/027
 */


public class PSKDeviceAttitude implements PSKSensorManagerListener {
    public static final int kPSKErrorRestricted = 10024;
    public static final float kPSKHumanEyeHeight = 1.62F;
    private static final String TAG = "PSKDeviceAttitude";
    private static PSKDeviceAttitude PSKDeviceAttitudeSingleton;
    private static final float LOWPASS_SENSOR_ALPHA = 0.95F;
    private static final float LOWPASS_ORIENTATION_ROLL_ALPHA = 0.7F;
    private Context context;
    private Display display;
    private int gravityAccuracy;
    private int rotationVectorAccuracy;
    private float locationAccuracy;
    private float[] gravity = new float[]{0.0F, 0.0F, 0.0F};
    private PSKVector3 attitudeSignedGravity = new PSKVector3();
    private boolean attitudeHasGravityOnX;
    private boolean attitudeHasGravityOnY;
    private boolean attitudeHasGravityOnZ;
    private float[] rotationVector = new float[]{0.0F, 0.0F, 0.0F};
    private Location location = null;
    private long gravityRawTimestamp;
    private long rotationVectorRawTimestamp;
    private long locationTimestamp;
    private float[] rotationVectorAttitudeMatrix = new float[16];
    private boolean needsToUpdateRotationVectorAttitudeMatrix = true;
    private float[] rotationVectorOrientation = new float[3];
    private boolean needsToUpdaterotationVectorOrientation = true;
    private PSKVector3 ecefCoordinates;
    private boolean needsToUpdateECEF = true;
    private float locationBearing;
    private boolean needsToUpdateLocationBearing = true;
    private float[] iosEquivalentGravity = new float[3];
    private boolean hasGravityRaw = false;
    private boolean hasRotationVectorRaw = false;
    private boolean hasLocation = false;
    private float rotateXAngle = -270.0F;
    private float rotateYAngle = 90.0F;
    private float[] rotateXMatrix;
    private float[] rotateYMatrix;
    private float[] tempMatrix = new float[16];
    private float[] headingMatrix = new float[16];
    private boolean needsToUpdateHeadingMatrix = true;
    private float[] tempVector = new float[4];
    private float[] pointInZ = new float[]{0.0F, 0.0F, 1.0F, 0.0F};
    private float[] pointInX = new float[]{1.0F, 0.0F, 0.0F, 0.0F};
    private int currentSurfaceRotation;
    private int previousSurfaceRotation = -1;
    private PSKDeviceOrientation currentInterfaceRotation;
    private PSKDeviceOrientation previousInterfaceOrientation;
    private PSKDeviceOrientation currentDeviceOrientation;
    private PSKDeviceOrientation previousDeviceOrientation;
    private boolean hadFirstOrientationRollEvent;
    private float orientationRollFrom;
    private final int ignoreFirstLowPassValues;
    private int ignoredEventCountGravity;
    private int ignoredEventCountRotationVector;
    private static final float GRAVITY_THRESHOLD_FACEUP = 0.7F;

    private PSKDeviceAttitude() {
        this.currentInterfaceRotation = PSKDeviceOrientation.Unknown;
        this.previousInterfaceOrientation = PSKDeviceOrientation.Unknown;
        this.currentDeviceOrientation = PSKDeviceOrientation.Unknown;
        this.previousDeviceOrientation = PSKDeviceOrientation.Unknown;
        this.ignoreFirstLowPassValues = 15;
        this.ignoredEventCountGravity = 0;
        this.ignoredEventCountRotationVector = 0;
        this.context = PSKApplication.getAppContext();
        this.initForAttitudeHeadingAndTilting();
        this.orientationRollFrom = 0.0F;
    }

    public static PSKDeviceAttitude sharedDeviceAttitude() {
        if (PSKDeviceAttitudeSingleton == null) {
            Log.wtf("PSKDeviceAttitude", "Creating new Singleton");
            PSKDeviceAttitudeSingleton = new PSKDeviceAttitude();
        }

        return PSKDeviceAttitudeSingleton;
    }

    private void initForAttitudeHeadingAndTilting() {
        this.rotateXMatrix = new float[16];
        this.rotateYMatrix = new float[16];
        Matrix.setIdentityM(this.rotateXMatrix, 0);
        Matrix.setIdentityM(this.rotateYMatrix, 0);
        Matrix.rotateM(this.rotateXMatrix, 0, this.rotateXAngle, 1.0F, 0.0F, 0.0F);
        Matrix.rotateM(this.rotateYMatrix, 0, this.rotateYAngle, 0.0F, 1.0F, 0.0F);
    }

    private void updateHeadingMatrixIfNecessary() {
        if (this.needsToUpdateHeadingMatrix) {
            this.tempMatrix = new float[16];
            this.headingMatrix = new float[16];
            Matrix.multiplyMM(this.tempMatrix, 0, this.getRotationVectorAttitudeMatrix(), 0, this.rotateXMatrix, 0);
            Matrix.multiplyMM(this.headingMatrix, 0, this.tempMatrix, 0, this.rotateYMatrix, 0);
            this.needsToUpdateHeadingMatrix = false;
        }

    }

    private PSKDeviceOrientation getPSKRotationHelper(int rotation) {
        switch (rotation) {
            case 0:
                return PSKDeviceOrientation.Normal;
            case 1:
                return PSKDeviceOrientation.Left;
            case 2:
                return PSKDeviceOrientation.UpsideDown;
            case 3:
                return PSKDeviceOrientation.Right;
            default:
                return PSKDeviceOrientation.Unknown;
        }
    }

    @Override
    public void onRotationVectorChanged(float[] values, long timestamp) {
        if (this.ignoredEventCountRotationVector < 15) {
            this.rotationVector[0] = values[0];
            this.rotationVector[1] = values[1];
            this.rotationVector[2] = values[2];
            ++this.ignoredEventCountRotationVector;
        } else {
            this.rotationVector = PSKMath.lowPass(values, this.rotationVector, 0.95F);
        }

        this.rotationVectorRawTimestamp = timestamp;
        this.hasRotationVectorRaw = true;
        this.needsToUpdateRotationVectorAttitudeMatrix = true;
        this.needsToUpdaterotationVectorOrientation = true;
        this.needsToUpdateHeadingMatrix = true;
    }

    @Override
    public void onRotationVectorAccuracyChanged(int accuracy) {
        this.rotationVectorAccuracy = accuracy;
    }

    @Override
    public void onGravityChanged(float[] values, long timestamp) {
        if (this.ignoredEventCountGravity < 15) {
            ++this.ignoredEventCountGravity;
            this.gravity[0] = values[0];
            this.gravity[1] = values[1];
            this.gravity[2] = values[2];
        } else {
            this.gravity = PSKMath.lowPass(values, this.gravity, 0.95F);
        }

        this.gravityRawTimestamp = timestamp;
        this.hasGravityRaw = true;
        this.iosEquivalentGravity[0] = -this.gravity[0] / 9.81F;
        this.iosEquivalentGravity[1] = -this.gravity[1] / 9.81F;
        this.iosEquivalentGravity[2] = -this.gravity[2] / 9.81F;
        this.getAttitudeSignedGravity().x = (float) Math.round(this.iosEquivalentGravity[0]);
        this.getAttitudeSignedGravity().y = (float) Math.round(this.iosEquivalentGravity[1]);
        this.getAttitudeSignedGravity().z = (float) Math.round(this.iosEquivalentGravity[2]);
        this.attitudeHasGravityOnX = this.getAttitudeSignedGravity().x != 0.0F;
        this.attitudeHasGravityOnY = this.getAttitudeSignedGravity().y != 0.0F;
        this.attitudeHasGravityOnZ = this.getAttitudeSignedGravity().z != 0.0F;
        this.setDeviceOrientationFromGravity();
    }

    @Override
    public void onGravityAccuracyChanged(int accuracy) {
        this.gravityAccuracy = accuracy;
    }

    @Override
    public void onOrientationChanged(int rollFromSensor) {
        float roll = -PSKMath.deltaAngle((float) rollFromSensor, 0.0F);
        if (!this.hadFirstOrientationRollEvent) {
            this.hadFirstOrientationRollEvent = true;
            this.orientationRollFrom = roll;
        } else {
            this.orientationRollFrom = PSKMath.lowPass(roll, this.orientationRollFrom, 0.7F);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        this.locationAccuracy = location.getAccuracy();
        this.locationTimestamp = location.getTime();
        this.hasLocation = true;
        this.needsToUpdateECEF = true;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }

    @Override
    public void onGpsStatusChanged(int i) {
    }

    public void getRotationMatrixSnapshot() {
        for (int i = 0; i < this.rotationVectorAttitudeMatrix.length; ++i) {
            Log.wtf("PSKDeviceAttitude", "i:" + i + " rotation: " + this.rotationVectorAttitudeMatrix[i]);
        }

    }

    public float[] getRotationVectorAttitudeMatrix() {
        if (!this.hasRotationVectorRaw) {
            Log.wtf("PSKDeviceAttitude", "Returning empty rotation matrix");
            return new float[]{
                    1.0F, 0.0F, 0.0F, 0.0F,
                    0.0F, 1.0F, 0.0F, 0.0F,
                    0.0F, 0.0F, 1.0F, 0.0F,
                    0.0F, 0.0F, 0.0F, 1.0F};
        } else {
            if (this.needsToUpdateRotationVectorAttitudeMatrix) {
                try {
                    SensorManager.getRotationMatrixFromVector(this.rotationVectorAttitudeMatrix, this.rotationVector);
                } catch (IllegalArgumentException var3) {
                    if (this.rotationVector.length > 3) {
                        float[] newVector = new float[]{this.rotationVector[0], this.rotationVector[1], this.rotationVector[2]};
                        SensorManager.getRotationMatrixFromVector(this.rotationVectorAttitudeMatrix, newVector);
                    }
                }

                this.needsToUpdateRotationVectorAttitudeMatrix = false;
            }

            return this.rotationVectorAttitudeMatrix;
        }
    }

    public PSKVector3 getEcefCoordinates() {
        if (this.needsToUpdateECEF) {
            this.ecefCoordinates = PSKMath.PSKConvertLatLonToEcef(this.location.getLatitude(), this.location.getLongitude(), this.location.getAltitude());
            this.needsToUpdateECEF = false;
        }

        return this.ecefCoordinates;
    }

    public float[] getNormalizedGravity() {
        if (this.hasGravityRaw) {
            return this.iosEquivalentGravity;
        } else {
            float[] fakeGrav = new float[]{0.0F, 0.0F, 0.0F};
            return fakeGrav;
        }
    }

    public float[] getRotationVectorRaw() {
        return this.rotationVector;
    }

    public Matrix4f getHeadingMatrix() {
        if (this.needsToUpdateHeadingMatrix) {
            this.updateHeadingMatrixIfNecessary();
        }

        return new Matrix4f(this.headingMatrix);
    }

    public int getDefaultDisplayOrientation() {
        if (this.display == null) {
            this.display = ((WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        }

        int rotation = this.display.getRotation();
        Configuration config = this.context.getResources().getConfiguration();
        return (rotation != 0 && rotation != 2 || config.orientation != 2) && (rotation != 1 && rotation != 3 || config.orientation != 1) ? 1 : 2;
    }

    public int getCurrentDisplayOrientation() {
        return this.context.getResources().getConfiguration().orientation;
    }

    public int getCurrentSurfaceRotation() {
        if (this.display == null) {
            this.display = ((WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        }

        int rotation = this.display.getRotation();
        this.previousSurfaceRotation = this.currentSurfaceRotation;
        this.currentSurfaceRotation = rotation;
        return this.currentSurfaceRotation;
    }

    public int getPreviousSurfaceRotation() {
        if (this.previousSurfaceRotation < 0) {
            this.getCurrentSurfaceRotation();
        }

        return this.previousSurfaceRotation;
    }

    public PSKDeviceOrientation getCurrentInterfaceRotation() {
        if (this.display == null) {
            this.display = ((WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        }

        int rotation = this.display.getRotation();
        this.previousInterfaceOrientation = this.currentInterfaceRotation;
        this.currentInterfaceRotation = this.getPSKRotationHelper(rotation);
        return this.currentInterfaceRotation;
    }

    public PSKDeviceOrientation getPreviousInterfaceRotation() {
        if (this.previousInterfaceOrientation == PSKDeviceOrientation.Unknown) {
            this.getCurrentInterfaceRotation();
        }

        return this.previousInterfaceOrientation;
    }

    private void setDeviceOrientationFromGravity() {
        PSKDeviceOrientation newDeviceOrientation = PSKDeviceOrientation.Unknown;
        if (this.attitudeHasGravityOnX) {
            if (this.attitudeSignedGravity.x > 0.0F) {
                newDeviceOrientation = PSKDeviceOrientation.Right;
            } else {
                newDeviceOrientation = PSKDeviceOrientation.Left;
            }
        } else if (this.attitudeHasGravityOnY) {
            if (this.attitudeSignedGravity.y > 0.0F) {
                newDeviceOrientation = PSKDeviceOrientation.UpsideDown;
            } else {
                newDeviceOrientation = PSKDeviceOrientation.Normal;
            }
        } else if (Math.abs(this.iosEquivalentGravity[2]) > 0.7F) {
            if (this.attitudeSignedGravity.z > 0.0F) {
                newDeviceOrientation = PSKDeviceOrientation.FaceDown;
            } else {
                newDeviceOrientation = PSKDeviceOrientation.FaceUp;
            }
        }

        if (newDeviceOrientation != this.getCurrentDeviceOrientation()) {
            this.previousDeviceOrientation = this.getCurrentDeviceOrientation();
            this.currentDeviceOrientation = newDeviceOrientation;
            PSKSensorManager.getSharedSensorManager().onDeviceOrientationChanged(newDeviceOrientation);
        }

    }

    public PSKDeviceOrientation getCurrentDeviceOrientation() {
        return this.currentDeviceOrientation;
    }

    public PSKDeviceOrientation getPreviousDeviceOrientation() {
        return this.previousDeviceOrientation;
    }

    public float getOrientationRoll() {
        return this.orientationRollFrom;
    }

    public Location getLocation() {
        return this.location;
    }

    public float[] getRotationVectorOrientation() {
        if (this.needsToUpdaterotationVectorOrientation) {
            SensorManager.getOrientation(this.rotationVectorAttitudeMatrix, this.rotationVectorOrientation);
            this.needsToUpdaterotationVectorOrientation = false;
        }

        return this.rotationVectorOrientation;
    }

    public float getLocationBearing() {
        if (this.needsToUpdateLocationBearing) {
            this.locationBearing = this.location.getBearing();
            this.needsToUpdateLocationBearing = false;
        }

        return this.locationBearing;
    }

    // 俯仰角
    public float getGravityPitchDegrees() {
        return this.hasGravityRaw ? (float) Math.toDegrees(Math.atan2((double) this.gravity[1] / 9.81D,
                (double) (-this.gravity[2]) / 9.81D)) : 0.0F;
    }

    //翻滚角
    public float getGravityRollDegrees() {
        return this.hasGravityRaw ? (float) (
                -Math.toDegrees(Math.atan2((double) this.gravity[0] / 9.81D, (double) (-this.gravity[1]) / 9.81D))) : 0.0F;
    }

    //方位角
    public double getAttitudeHeading() {
        if (this.headingMatrix != null) {
            this.updateHeadingMatrixIfNecessary();
            this.tempVector = new float[4];
            Matrix.multiplyMV(this.tempVector, 0, this.headingMatrix, 0, this.pointInZ, 0);
            this.tempVector[0] = -this.tempVector[0];
            this.tempVector[1] = -this.tempVector[1];
            this.tempVector[2] = -this.tempVector[2];
            float[] planarHeadingSpace = PSKMath.PSKRadarCoordinatesFromVectorWithGravity(this.tempVector, this.getNormalizedGravity());
            double heading = -57.29577951308232D * (Math.atan2((double) planarHeadingSpace[1], (double) planarHeadingSpace[0]) - 1.5707963267948966D);
            return PSKMath.drepeat(heading, 360.0D);
        } else {
            return 0.0D;
        }
    }

    //俯仰角
    public double getAttitudeTilting() {
        if (this.hasGravityRaw && this.hasRotationVectorRaw) {
            this.updateHeadingMatrixIfNecessary();
            this.tempVector = new float[4];
            Matrix.multiplyMV(this.tempVector, 0, this.headingMatrix, 0, this.pointInX, 0);
            float[] planarPitchSpace = PSKMath.PSKAtittudePitchCoordinates(this.tempVector, this.getNormalizedGravity());
            double pitch = Math.toDegrees(Math.atan2((double) planarPitchSpace[1], (double) planarPitchSpace[0]));
            return Math.abs(PSKMath.drepeat(pitch, 180.0D));
        } else {
            return -1.0D;
        }
    }

    public static String orientationToString(int orientation) {
        return orientation == 1 ? "Portrait" : (orientation == 2 ? "Landscape" : "Unknown");
    }

    public static String surfaceRotationToString(int orientation) {
        return orientation == 0 ? "ROTATION_0" : (orientation == 2 ? "ROTATION_180" : (orientation == 1 ? "ROTATION_90" : (orientation == 3 ? "ROTATION_270" : "ROTATION_INVALID")));
    }

    public static String rotationToString(PSKDeviceOrientation orientation) {
        return orientation == PSKDeviceOrientation.Normal ? "Normal" :
                (orientation == PSKDeviceOrientation.Left ? "Left" : (orientation == PSKDeviceOrientation.Right ? "Right" : (orientation == PSKDeviceOrientation.UpsideDown ? "UpsideDown" :
                        (orientation == PSKDeviceOrientation.FaceUp ? "FaceUp" :
                                (orientation == PSKDeviceOrientation.FaceDown ? "FaceDown" : "Unknown")))));
    }

    @Override
    public String toString() {
        long now = System.nanoTime();
        StringBuilder builder = new StringBuilder();

        try {
            if (this.hasGravityRaw) {
                builder.append("gravityRaw:\nX:" + this.gravity[0] + " Y:" + this.gravity[1] + " Z:" + this.gravity[2] + "\n(ACC: " + this.gravityAccuracy + " Age: " + TimeUnit.NANOSECONDS.toMillis(now - this.gravityRawTimestamp) + "ms)\n");
                float[] iosStyle = this.getNormalizedGravity();
                builder.append("gravityIos:\nX:" + iosStyle[0] + " Y:" + iosStyle[1] + " Z:" + iosStyle[2] + "\n");
                builder.append("gravityPitchAndRoll:\nPitch:" + this.getGravityPitchDegrees() + " Roll:" + this.getGravityRollDegrees() + "\n");
            } else {
                builder.append("gravityRaw not available\n");
            }

            if (this.hasRotationVectorRaw) {
                builder.append("rotationVectorRaw:\n" + this.rotationVector[0] + " " + this.rotationVector[1] + " " + this.rotationVector[2] + "\n(ACC: " + this.rotationVectorAccuracy + " Age: " + TimeUnit.NANOSECONDS.toMillis(now - this.rotationVectorRawTimestamp) + "ms)\n");
                builder.append("attitudeMatrix:\n" + PSKUtils.matrix16ToString(this.getRotationVectorAttitudeMatrix()) + "\n");
                builder.append("orientation:\n" + PSKUtils.floatArrayToStringWithRadToDeg(this.getRotationVectorOrientation()) + "\n");
            } else {
                builder.append("rotationVectorRaw not available\n");
            }

            if (this.hasLocation) {
                builder.append("Location: " + this.location.getLatitude() + " " + this.location.getLongitude() + "\n(ACC: " + this.locationAccuracy + " Age: " + TimeUnit.NANOSECONDS.toMillis(now - this.locationTimestamp) + "ms)\n");
                builder.append("ECEF:\n" + this.getEcefCoordinates().toString() + ")\n");
                builder.append("Bearing:\n" + this.getLocationBearing() + "\n");
            } else {
                builder.append("Location not available\n");
            }

            if (this.hasGravityRaw && this.hasRotationVectorRaw) {
                builder.append("attitudeHeading:\n" + this.getAttitudeHeading() + "\n");
                builder.append("attitudeTilting:\n" + this.getAttitudeTilting() + "\n");
            } else if (!this.hasGravityRaw && !this.hasRotationVectorRaw) {
                builder.append("Missing Gravity and RotationVector to compute attitudeHeading\n");
            } else if (this.hasGravityRaw && !this.hasRotationVectorRaw) {
                builder.append("Missing RotationVector to compute attitudeHeading\n");
            } else if (!this.hasGravityRaw && this.hasRotationVectorRaw) {
                builder.append("Missing Gravity to compute attitudeHeading\n");
            }

            builder.append("OrientationRoll:" + this.orientationRollFrom + "\n");
        } catch (NullPointerException var5) {
            Log.d("NULLPOINTER", "" + var5.getMessage());
        }

        return builder.toString();
    }

    public PSKVector3 getAttitudeSignedGravity() {
        return this.attitudeSignedGravity;
    }

    public boolean isAttitudeHasGravityOnX() {
        return this.attitudeHasGravityOnX;
    }

    public boolean isAttitudeHasGravityOnY() {
        return this.attitudeHasGravityOnY;
    }

    public boolean isAttitudeHasGravityOnZ() {
        return this.attitudeHasGravityOnZ;
    }
}
