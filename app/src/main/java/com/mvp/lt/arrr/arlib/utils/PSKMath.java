package com.mvp.lt.arrr.arlib.utils;

import android.graphics.PointF;

import com.mvp.lt.arrr.arlib.bean.PSKVector3;
import com.mvp.lt.arrr.arlib.bean.PSKVector3Double;

/**
 * $activityName
 *
 * @author LiuTao
 * @date 2018/7/27/027
 */


public class PSKMath {
    private static String TAG = "PSKMath";
    public static final double DEGREES_TO_RADIANS = 0.017453292519943295D;
    public static final double RADIANS_TO_DEGREES = 57.29577951308232D;
    public static final float DEGREES_TO_RADIANS_FLOAT = 0.017453292F;
    public static final float RADIANS_TO_DEGREES_FLOAT = 57.295776F;
    public static final float[] identityMatrix4x4 = new float[]{1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F};
    public static final double WGS84_A = 6378137.0D;
    public static final double WGS84_E = 0.081819190842622D;

    public PSKMath() {
    }

    public static float Mile2Km(float miles) {
        return miles * 1.609344F;
    }

    public static float Mile2Meters(float miles) {
        return miles * 1.609344F * 1000.0F;
    }

    public static double Km2Mile(double _d) {
        return _d * 0.621371192D;
    }

    public static float Km2Mile(float _d) {
        return _d * 0.6213712F;
    }

    public static float Feet2Meter(float feet) {
        return feet * 0.3048F;
    }

    public static float Meter2Feet(float meters) {
        return meters * 3.28084F;
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * Math.min(t, 1.0D);
    }

    public static float lerpf(float a, float b, float t) {
        return a + (b - a) * Math.min(t, 1.0F);
    }

    public static double lerpAngle(double a, double b, double f) {
        double tmp;
        if(b - a > 180.0D) {
            tmp = a + 360.0D;
        } else if(b - a < -180.0D) {
            tmp = a - 360.0D;
        } else {
            tmp = a;
        }

        return tmp + f * (b - tmp);
    }

    public static float lerpAnglef(float a, float b, float f) {
        float tmp;
        if((double)(b - a) > 180.0D) {
            tmp = a + 360.0F;
        } else if((double)(b - a) < -180.0D) {
            tmp = a - 360.0F;
        } else {
            tmp = a;
        }

        return tmp + f * (b - tmp);
    }

    public static int repeat(int value, int max) {
        return value < 0?max + value:value % max;
    }

    public static float frepeat(float value, float max) {
        return value < 0.0F?max + value:value % max;
    }

    public static double drepeat(double value, double max) {
        return value < 0.0D?max + value:value % max;
    }

    public static float signf(float n) {
        return n < 0.0F?-1.0F:1.0F;
    }

    public static double sign(double n) {
        return n < 0.0D?-1.0D:1.0D;
    }

    public static float relativeAngle(float angle) {
        float cAngle = angle % 180.0F;
        float aAngle = Math.abs(cAngle);
        return aAngle > 90.0F?(180.0F - aAngle) * signf(angle):cAngle;
    }

    public static double relativeAngle(double angle) {
        double cAngle = angle % 180.0D;
        double aAngle = Math.abs(cAngle);
        return aAngle > 90.0D?(180.0D - aAngle) * sign(angle):cAngle;
    }

    public static float linsin(float angle) {
        return relativeAngle(angle) / 90.0F * (float)(Math.abs(angle) > 180.0F?-1:1);
    }

    public static double linsin(double angle) {
        return relativeAngle(angle) / 90.0D * (double)(Math.abs(angle) > 180.0D?-1:1);
    }

    public static float lincos(float angle) {
        return (90.0F - Math.abs(relativeAngle(angle))) / 90.0F * (float)(Math.abs(angle) > 90.0F && Math.abs(angle) < 270.0F?-1:1);
    }

    public static double lincos(double angle) {
        return (90.0D - Math.abs(relativeAngle(angle))) / 90.0D * (double)(Math.abs(angle) > 90.0D && Math.abs(angle) < 270.0D?-1:1);
    }

    public static float deltaAngle(float a, float b) {
        float difference = (b - a) % 360.0F;
        if(difference > 180.0F) {
            difference -= 360.0F;
        }

        if(difference < -180.0F) {
            difference += 360.0F;
        }

        return difference;
    }

    public static float clampf(float v, float min, float max) {
        return v > max?max:(v < min?min:v);
    }

    public static PSKVector3 PSKConvertLatLonToEcef(double lat, double lon, double alt) {
        double clat = Math.cos(lat * 0.017453292519943295D);
        double slat = Math.sin(lat * 0.017453292519943295D);
        double clon = Math.cos(lon * 0.017453292519943295D);
        double slon = Math.sin(lon * 0.017453292519943295D);
        double N = 6378137.0D / Math.sqrt(1.0D - 0.0066943799901414D * slat * slat);
        double x = (N + alt) * clat * clon;
        double y = (N + alt) * clat * slon;
        double z = (N * 0.9933056200098586D + alt) * slat;
        return new PSKVector3(x, y, z);
    }

    public static PSKVector3Double PSKEcefToEnu(double latPoi, double lonPoi, PSKVector3 ecefDevice, PSKVector3 poiECEF) {
        double clat = Math.cos(latPoi * 0.017453292519943295D);
        double slat = Math.sin(latPoi * 0.017453292519943295D);
        double clon = Math.cos(lonPoi * 0.017453292519943295D);
        double slon = Math.sin(lonPoi * 0.017453292519943295D);
        double dx = (double)(ecefDevice.x - poiECEF.x);
        double dy = (double)(ecefDevice.y - poiECEF.y);
        double dz = (double)(ecefDevice.z - poiECEF.z);
        return new PSKVector3Double(-slon * dx + clon * dy, -slat * clon * dx - slat * slon * dy + clat * dz, clat * clon * dx + clat * slon * dy + slat * dz);
    }

    public static double[] PSKMatrix4x4MultiplyWithVector4(float[] m, double[] v) {
        double[] resultVector = new double[4];
        int x = 0;
        int y = 1;
        int z = 2;
        int w = 3;
        resultVector[x] = (double)m[0] * v[x] + (double)m[4] * v[y] + (double)m[8] * v[z] + (double)m[12] * v[w];
        resultVector[y] = (double)m[1] * v[x] + (double)m[5] * v[y] + (double)m[9] * v[z] + (double)m[13] * v[w];
        resultVector[z] = (double)m[2] * v[x] + (double)m[6] * v[y] + (double)m[10] * v[z] + (double)m[14] * v[w];
        resultVector[w] = (double)m[3] * v[x] + (double)m[7] * v[y] + (double)m[11] * v[z] + (double)m[15] * v[w];
        return resultVector;
    }

    public static void PSKMatrixSetYRotationUsingRadians(float[] mout, float rad) {
        mout[0] = (float) Math.cos((double)rad);
        mout[2] = (float) Math.sin((double)rad);
        mout[8] = -mout[2];
        mout[10] = mout[0];
        mout[1] = mout[3] = mout[4] = mout[6] = mout[7] = 0.0F;
        mout[9] = mout[11] = mout[13] = mout[12] = mout[14] = 0.0F;
        mout[5] = mout[15] = 1.0F;
    }

    public static void PSKMatrixSetYRotationUsingDegrees(float[] mout, float degrees) {
        PSKMatrixSetYRotationUsingRadians(mout, degrees * 3.1415927F / 180.0F);
    }

    public static float PSKVector4Angle(float[] vec) {
        return 57.295776F * (float) Math.atan2((double)vec[1], (double)vec[0]);
    }

    public static float[] PSKRadarCoordinatesFromVectorWithGravity(float[] v, float[] g) {
        boolean a0 = false;
        boolean a1 = true;
        boolean a2 = true;
        return new float[]{v[1] * g[0] - v[0] * g[1] - v[0] * g[2], v[2] * Math.abs(g[0]) + v[2] * Math.abs(g[1]) + v[1] * g[2]};
    }

    public static float[] PSKMatrixFastMultiplyWithMatrix(float[] m1, float[] m2) {
        float[] result = new float[]{m1[0] * m2[0] + m1[4] * m2[1] + m1[8] * m2[2] + m1[12] * m2[3], m1[1] * m2[0] + m1[5] * m2[1] + m1[9] * m2[2] + m1[13] * m2[3], m1[2] * m2[0] + m1[6] * m2[1] + m1[10] * m2[2] + m1[14] * m2[3], m1[3] * m2[0] + m1[7] * m2[1] + m1[11] * m2[2] + m1[15] * m2[3], m1[0] * m2[4] + m1[4] * m2[5] + m1[8] * m2[6] + m1[12] * m2[7], m1[1] * m2[4] + m1[5] * m2[5] + m1[9] * m2[6] + m1[13] * m2[7], m1[2] * m2[4] + m1[6] * m2[5] + m1[10] * m2[6] + m1[14] * m2[7], m1[3] * m2[4] + m1[7] * m2[5] + m1[11] * m2[6] + m1[15] * m2[7], m1[0] * m2[8] + m1[4] * m2[9] + m1[8] * m2[10] + m1[12] * m2[11], m1[1] * m2[8] + m1[5] * m2[9] + m1[9] * m2[10] + m1[13] * m2[11], m1[2] * m2[8] + m1[6] * m2[9] + m1[10] * m2[10] + m1[14] * m2[11], m1[3] * m2[8] + m1[7] * m2[9] + m1[11] * m2[10] + m1[15] * m2[11], m1[0] * m2[12] + m1[4] * m2[13] + m1[8] * m2[14] + m1[12] * m2[15], m1[1] * m2[12] + m1[5] * m2[13] + m1[9] * m2[14] + m1[13] * m2[15], m1[2] * m2[12] + m1[6] * m2[13] + m1[10] * m2[14] + m1[14] * m2[15], m1[3] * m2[12] + m1[7] * m2[13] + m1[11] * m2[14] + m1[15] * m2[15]};
        return result;
    }

    public static float[] PSKAtittudePitchCoordinates(float[] vectorV, float[] vectorG) {
        float[] result = new float[]{vectorV[0] * vectorG[0] + vectorV[1] * vectorG[1], vectorV[2] * Math.abs(vectorG[0]) + vectorV[2] * Math.abs(vectorG[1])};
        return result;
    }

    public static PointF RotatedPointAboutOrigin(float x, float y, float sin, float cos) {
        return new PointF(cos * x - sin * y, sin * x + cos * y);
    }

    public static double[] PSKMatrix4x4MultiplyWithMatrix4x4(double[] a, double[] b) {
        double[] c = new double[16];

        for(int col = 0; col < 3; ++col) {
            for(int row = 0; row < 3; ++row) {
                for(int i = 0; i < 3; ++i) {
                    c[col * 3 + row] += a[i * 3 + row] * b[col * 3 + i];
                }
            }
        }

        return c;
    }

    public static float[] PSKMatrixCreateProjection(double fovy, float aspect, float zNear, float zFar) {
        float[] mout = new float[16];
        float f = 1.0F / (float) Math.tan(fovy / 2.0D);
        mout[0] = f / aspect;
        mout[1] = 0.0F;
        mout[2] = 0.0F;
        mout[3] = 0.0F;
        mout[4] = 0.0F;
        mout[5] = f;
        mout[6] = 0.0F;
        mout[7] = 0.0F;
        mout[8] = 0.0F;
        mout[9] = 0.0F;
        mout[10] = (zFar + zNear) / (zNear - zFar);
        mout[11] = -1.0F;
        mout[12] = 0.0F;
        mout[13] = 0.0F;
        mout[14] = 2.0F * zFar * zNear / (zNear - zFar);
        mout[15] = 0.0F;
        return mout;
    }

    public static PSKVector3 PSKMatrix3x3MultiplyWithVector3(float[] m, PSKVector3 v) {
        PSKVector3 vout = new PSKVector3();
        vout.x = m[0] * v.x + m[4] * v.y + m[8] * v.z;
        vout.y = m[1] * v.x + m[5] * v.y + m[9] * v.z;
        vout.z = m[2] * v.x + m[6] * v.y + m[10] * v.z;
        return vout;
    }

    public static float[] lowPass(float[] newValues, float[] previousValues, float alpha) {
        newValues[0] = alpha * previousValues[0] + (1.0F - alpha) * newValues[0];
        newValues[1] = alpha * previousValues[1] + (1.0F - alpha) * newValues[1];
        newValues[2] = alpha * previousValues[2] + (1.0F - alpha) * newValues[2];
        return newValues;
    }

    public static float lowPass(float newValue, float previousValue, float alpha) {
        return alpha * previousValue + (1.0F - alpha) * newValue;
    }
}
