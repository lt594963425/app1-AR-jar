package com.mvp.lt.arrr.arlib.bean;

/**
 * $activityName
 *
 * @author LiuTao
 * @date 2018/7/27/027
 */


public class PSKVector3 {
    public float x;
    public float y;
    public float z;

    public PSKVector3() {
        this.x = 0.0F;
        this.y = 0.0F;
        this.z = 0.0F;
    }

    public PSKVector3(PSKVector3 other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    public PSKVector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public PSKVector3(double x, double y, double z) {
        this.x = (float)x;
        this.y = (float)y;
        this.z = (float)z;
    }

    public static PSKVector3 Zero() {
        return new PSKVector3();
    }

    public static PSKVector3 One() {
        return new PSKVector3(1.0F, 1.0F, 1.0F);
    }

    public void toOne() {
        this.x = 1.0F;
        this.y = 1.0F;
        this.z = 1.0F;
    }

    public void toZero() {
        this.x = 0.0F;
        this.y = 0.0F;
        this.z = 0.0F;
    }

    public float magnitude() {
        return (float) Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public float sqrMagnitude() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    public static float magnitude(float x, float y, float z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public PSKVector3 normalize() {
        float s = 1.0F / this.magnitude();
        return new PSKVector3(s * this.x, s * this.y, s * this.z);
    }

    public void normalize(float _x, float _y, float _z) {
        float s = 1.0F / magnitude(_x, _y, _z);
        this.x = s * _x;
        this.y = s * _y;
        this.z = s * _z;
    }

    public void normalize(float _x, float _y, float _z, float _m) {
        this.x = _x / _m;
        this.y = _y / _m;
        this.z = _z / _m;
    }

    public float angleTowards(PSKVector3 vector) {
        PSKVector3 towards = vector.normalize();
        PSKVector3 from = this.normalize();
        return (float) Math.toDegrees(Math.atan2((double)towards.x, (double)towards.z) - Math.atan2((double)from.x, (double)from.z));
    }

    public float angle() {
        PSKVector3 from = this.normalize();
        return (float) Math.toDegrees(Math.atan2((double)from.y, (double)from.x));
    }

    public float elevation() {
        PSKVector3 from = this.normalize();
        return (float) Math.toDegrees(Math.atan2(Math.sqrt((double)(from.y * from.x + from.y + from.y)), (double)from.z));
    }

    @Override
    public String toString() {
        return String.format("x=%f, y=%f, z=%f", new Object[]{Float.valueOf(this.x), Float.valueOf(this.y), Float.valueOf(this.z)});
    }

    public String toApiParam() {
        return String.format("%f,%f,%f", new Object[]{Float.valueOf(this.x), Float.valueOf(this.y), Float.valueOf(this.z)});
    }
}
