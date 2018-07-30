package com.mvp.lt.arrr.arlib.poi;

import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.location.Location;
import android.opengl.Matrix;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mvp.lt.arrr.arlib.PARFragment;
import com.mvp.lt.arrr.arlib.utils.PSKMath;
import com.mvp.lt.arrr.arlib.bean.PSKVector3;
import com.mvp.lt.arrr.arlib.bean.PSKVector3Double;
import com.mvp.lt.arrr.arlib.control.PARController;
import com.mvp.lt.arrr.arlib.listener.PSKDeviceAttitude;
import com.mvp.lt.arrr.arlib.view.PARRadarView;


/**
 * $activityName
 *
 * @author LiuTao
 * @date 2018/7/27/027
 */


public class PARPoi{
    private static float[] deviceGravity;
    protected int _backgroundImageResource = -1;
    protected Context ctx = null;
    protected boolean observed;
    protected int radarResourceId;
    protected Location location;
    protected Point offset = new Point(0, 0);
    public double distanceToUser = 0.0D;
    protected RelativeLayout _labelView;
    public boolean isHidden = false;
    public boolean isClippedByDistance = false;
    protected boolean isClippedByViewport = false;
    protected View radarView;
    protected boolean isDebug = false;
    private String TAG = "PARPoi";
    private double lastDistanceToUser;
    private float angleToUser;
    private float[] toUserRotationMatrix = new float[16];
    private float[] worldPositionVector4 = new float[]{0.0F, 0.0F, 0.0F, 0.0F};
    private float[] worldToScreenSpaceVector4 = new float[]{0.0F, 0.0F, 0.0F, 0.0F};
    private PointF relativeScreenPosition = new PointF();
    private RectF relativeViewportBounds = new RectF(-0.25F, -0.25F, 1.5F, 1.5F);
    private boolean hadLocationUpdate;
    private PSKVector3 ecefCoordinatesDevice;
    private PSKVector3 ecefCoordinatesPOI;
    private static float viewSin = 0.0F;
    private static float viewCos = 0.0F;
    private PSKVector3 worldToRadarSpace;
    private float[] radarSpace;
    protected PointF halfSizeOfView = new PointF();
    protected boolean isAddedToController;
    protected boolean addedToView;
    protected boolean addedToRadar;

    public PARPoi() {
        this.ctx = PARController.getContext();
    }

    public PARPoi(Location atLocation) {
        this.setLocation(atLocation);
        this.ctx = PARController.getContext();
    }

    public static void setDeviceGravity(float[] gravity) {
        deviceGravity = gravity;
    }

    public View getRadarView() {
        return this.radarView;
    }

    public boolean isHidden() {
        return this.isHidden;
    }

    public boolean isClippedByDistance() {
        return this.isClippedByDistance;
    }

    public View getView() {
        return this._labelView;
    }

    public void renderInRadar(PARRadarView radar) {
        if(this.hadLocationUpdate) {
            if(this.isAddedToController) {
                if(this.radarView == null) {
                    this.radarView = new ImageView(this.ctx);
                    this.radarView.setBackgroundResource(this.radarResourceId);
                    this.radarView.setVisibility(View.VISIBLE);
                }

                float range = radar.getRadarRange();
                float radius = radar.getRadarRadiusForRendering();
                float distanceOnRadar = Math.min((float)this.distanceToUser / range, 1.0F) * radius;
                this.worldToRadarSpace = PSKVector3.Zero();
                PSKVector3 v = new PSKVector3(0.0F, 0.0F, distanceOnRadar);
                float[] finalRotation = PSKMath.PSKMatrixFastMultiplyWithMatrix(radar.getRadarMatrix().getArray(), this.toUserRotationMatrix);
                this.worldToRadarSpace = PSKMath.PSKMatrix3x3MultiplyWithVector3(finalRotation, v);
                float[] tempWorldToRadarSpace = new float[]{this.worldToRadarSpace.x, this.worldToRadarSpace.y, this.worldToRadarSpace.z};
                this.radarSpace = PSKMath.PSKRadarCoordinatesFromVectorWithGravity(tempWorldToRadarSpace, PSKDeviceAttitude.sharedDeviceAttitude().getNormalizedGravity());
                float var10001 = this.radarSpace[1];
                float x = radar.getCenter().x + PSKMath.clampf(var10001, -radius, radius);
                var10001 = this.radarSpace[0];
                float y = radar.getCenter().y - PSKMath.clampf(var10001, -radius, radius);
                this.radarView.setX(x - (float)this.radarView.getMeasuredWidth() * 0.5F);
                this.radarView.setY(y - (float)this.radarView.getMeasuredHeight() * 0.5F);
                if(!this.addedToRadar) {
                    this.addToRadar(radar);
                }
            }
        }
    }

    public PointF getRelativeScreenPosition() {
        return this.relativeScreenPosition != null?this.relativeScreenPosition:new PointF(0.0F, 0.0F);
    }

    public static void setViewRotation(float angle) {
        viewSin = PSKMath.linsin(angle);
        viewCos = PSKMath.lincos(angle);
    }

    public void updateLocation() {
        PSKDeviceAttitude deviceAttitude = PSKDeviceAttitude.sharedDeviceAttitude();
        if(deviceAttitude != null) {
            Location userLocation = deviceAttitude.getLocation();
            if(userLocation != null) {
                this.lastDistanceToUser = this.distanceToUser;
                this.ecefCoordinatesDevice = deviceAttitude.getEcefCoordinates();
                PSKVector3Double enuCoordinates = PSKMath.PSKEcefToEnu(this.getLocation().getLatitude(), this.getLocation().getLongitude(), this.ecefCoordinatesDevice, this.ecefCoordinatesPOI);
                if(this instanceof PARPoiLabelAdvanced) {
                    this.worldPositionVector4 = new float[]{(float)enuCoordinates.x, (float)enuCoordinates.y, (float)enuCoordinates.z, 1.0F};
                } else {
                    this.worldPositionVector4 = new float[]{(float)enuCoordinates.x, (float)enuCoordinates.y, 0.0F, 1.0F};
                }

                this.distanceToUser = (double)this.getLocation().distanceTo(userLocation);
                float[] distanceResults = new float[3];
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), this.getLocation().getLatitude(), this.getLocation().getLongitude(), distanceResults);
                this.angleToUser = distanceResults[2];
                PSKMath.PSKMatrixSetYRotationUsingDegrees(this.toUserRotationMatrix, this.angleToUser);
                if(this.distanceToUser < (double)PARController.CLIP_POIS_NEARER_THAN) {
                    this.isClippedByDistance = true;
                } else if(this.distanceToUser > (double)PARController.CLIP_POIS_FARER_THAN) {
                    this.isClippedByDistance = true;
                } else {
                    this.isClippedByDistance = false;
                }

                if(this.isClippedByDistance) {
                    if(this.addedToView) {
                        this.removeFromView();
                    }

                    if(this.addedToRadar) {
                        this.removeFromRadar();
                    }
                }

                this.hadLocationUpdate = true;
                if(this.distanceToUser != this.lastDistanceToUser) {
                    this.updateContent();
                }

            }
        }
    }

    public void updateContent() {
    }

    public boolean isInView(float[] perspectiveMatrix) {
        int x = 0;
        int y = 1;
        int z = 2;
        int w = 3;
        this.worldToScreenSpaceVector4 = new float[]{0.0F, 0.0F, 0.0F, 0.0F};
        Matrix.multiplyMV(this.worldToScreenSpaceVector4, 0, perspectiveMatrix, 0, this.worldPositionVector4, 0);
        if(this.worldToScreenSpaceVector4[z] >= 0.0F) {
            return false;
        } else {
            PointF p = PSKMath.RotatedPointAboutOrigin(this.worldToScreenSpaceVector4[x] / this.worldToScreenSpaceVector4[w], this.worldToScreenSpaceVector4[y] / this.worldToScreenSpaceVector4[w], viewSin, viewCos);
            this.relativeScreenPosition.x = (p.x + 1.0F) * 0.5F;
            this.relativeScreenPosition.y = (p.y + 1.0F) * 0.5F;
            return this.relativeViewportBounds.contains(this.relativeScreenPosition.x, this.relativeScreenPosition.y);
        }
    }

    protected Point getOffset() {
        return this.offset;
    }

    public void renderInView(PARFragment parent) {
        if(this.hadLocationUpdate) {
            if(this.isAddedToController) {
                this.isClippedByViewport = !this.isInView(parent.getPerspectiveCameraMatrix());
                if(this.isClippedByViewport) {
                    if(this.addedToView) {
                        this.removeFromView();
                    }

                } else {
                    if(this._labelView == null) {
                        this.createView();
                    }

                    Point screenSize = parent.getScreenSize();
                    int x = (int)((float)screenSize.x * this.relativeScreenPosition.x);
                    int y = (int)((float)screenSize.y * (1.0F - this.relativeScreenPosition.y));
                    float finalX = (float)x - (float)this._labelView.getMeasuredWidth() * 0.5F + (float)this.offset.x;
                    float finalY = (float)y - (float)this._labelView.getMeasuredHeight() * 0.5F - (float)this.offset.y;
                    this._labelView.setX(finalX);
                    this._labelView.setY(finalY);
                    if(this.isObserved()) {
                        Log.d(this.TAG, "relativeScreenPosition: " + this.relativeScreenPosition.toString() + " x/y: " + x + ", " + y + " final x/y: " + finalX + ", " + finalY + " size: " + this._labelView.getMeasuredWidth() + "x" + this._labelView.getMeasuredHeight() + " screenMargin: " + (float)parent.getScreenMarginX() * 0.5F + " " + (float)parent.getScreenMarginY() * 0.5F);
                    }

                    if(!this.addedToView) {
                        this.addToView(parent);
                    }
                }
            }
        }
    }

    public void createView() {
    }

    public void onAddedToARController() {
        this.isAddedToController = true;
    }

    public void onRemovedFromARController() {
        this.isAddedToController = false;

        try {
            if(this._labelView != null && this._labelView.getParent() != null) {
                ((ViewGroup)this._labelView.getParent()).removeView(this._labelView);
            }
        } catch (NullPointerException var2) {
            var2.printStackTrace();
        } catch (Exception var3) {
            var3.printStackTrace();
        }

        if(this.addedToRadar) {
            this.removeFromRadar();
        }

    }

    public int getBackgroundImageResource() {
        return this._backgroundImageResource;
    }

    public void setBackgroundImageResource(int backgroundImageResource) {
        this._backgroundImageResource = backgroundImageResource;
    }

    public Location getLocation() {
        return this.location;
    }

    public void setLocation(Location atLocation) {
        this.location = atLocation;
        this.ecefCoordinatesPOI = PSKMath.PSKConvertLatLonToEcef(atLocation.getLatitude(), atLocation.getLongitude(), atLocation.getAltitude());
    }

    public boolean isClippedByViewport() {
        return this.isClippedByViewport;
    }

    void addToView(PARFragment theView) {
        this._labelView.setVisibility(View.VISIBLE);
        theView.getARView().addView(this._labelView);
        this.addedToView = true;
    }

    void removeFromView() {
        try {
            if(this._labelView != null && this._labelView.getParent() != null) {
                ((ViewGroup)this._labelView.getParent()).removeView(this._labelView);
                this._labelView.setVisibility(View.INVISIBLE);
                this.addedToView = false;
            }
        } catch (NullPointerException var2) {
            var2.printStackTrace();
        } catch (Exception var3) {
            var3.printStackTrace();
        }

    }

    void addToRadar(PARRadarView theRadar) {
        theRadar.addView(this.radarView);
        this.radarView.setVisibility(View.VISIBLE);
        this.addedToRadar = true;
    }

    void removeFromRadar() {
        try {
            if(this.radarView != null && this.radarView.getParent() != null) {
                ((ViewGroup)this.radarView.getParent()).removeView(this.radarView);
            }
        } catch (NullPointerException var2) {
            var2.printStackTrace();
        } catch (Exception var3) {
            var3.printStackTrace();
        }

        this.radarView.setVisibility(View.INVISIBLE);
        this.addedToRadar = false;
    }

    public boolean isObserved() {
        return this.observed;
    }

    public void setObserved(boolean observed) {
        this.observed = observed;
    }

    public boolean isHadLocationUpdate() {
        return this.hadLocationUpdate;
    }
}