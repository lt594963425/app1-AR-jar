package com.mvp.lt.arrr.arlib;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Point;
import android.location.Location;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mvp.lt.arrr.arlib.bean.PSKDeviceProperties;
import com.mvp.lt.arrr.arlib.enumm.PSKDeviceOrientation;
import com.mvp.lt.arrr.arlib.control.PARController;
import com.mvp.lt.arrr.arlib.listener.PSKDeviceAttitude;
import com.mvp.lt.arrr.arlib.listener.PSKEventListener;
import com.mvp.lt.arrr.arlib.listener.PSKSensorManager;
import com.mvp.lt.arrr.arlib.poi.PARPoi;
import com.mvp.lt.arrr.arlib.utils.PSKMath;
import com.mvp.lt.arrr.arlib.view.PARCameraView;
import com.mvp.lt.arrr.arlib.view.PARProgressBar;
import com.mvp.lt.arrr.arlib.view.PARRadarView;
import com.mvp.lt.arrr.arlib.view.PARView;

import java.util.ListIterator;

/**
 * $activityName
 *
 * @author LiuTao
 * @date 2018/7/27/027
 */


public class PARFragment extends Fragment implements PSKEventListener {
    private final float RENDER_PLANE_NEAR = 0.25F;
    private final float RENDER_PLANE_FAR = 10000.0F;
    private final int RENDER_INTERVAL = (int) Math.ceil(33.333335876464844D);
    protected static PARFragment activeFragment;
    private RelativeLayout _mainView;
    protected PARCameraView _cameraView;
    protected PARView _arView;
    private PARRadarView _arRadarView;
    protected int viewLayoutId;
    boolean startApp = true;
    private String TAG = "PARFragment";
    private PSKDeviceAttitude _deviceAttitude;
    private PSKSensorManager _sensorManager;
    private Runnable renderRunnable;
    private Handler renderLoopHandler = null;
    private float[] _perspectiveMatrix;
    private float[] _perspectiveCameraMatrix = new float[16];
    private boolean isDataTrackingExecuted = false;
    private boolean _hasProjectionMatrix = false;
    private Point _screenMargin = null;
    private int _screenOrientation;
    private int _screenOrientationPrevious;
    private float _screenOrientationOffsetAngle;
    private Point _cameraSize = new Point(0, 0);
    private Point _screenSize = new Point(0, 0);
    private TextView _debugTextView;
    private TextView _watermark;
    private boolean _needsWaterMark;
    private PARProgressBar progressBar;
    private boolean arViewShouldBeVisible;
    private boolean orientationHidesARView;
    private boolean hadLocationUpdate;
    protected boolean isInAirplaneMode = false;
    private int airplaneModeCounter = 0;
    private static final int AIRPLANEMODE_INTERVAL = 30;
    protected boolean hasAirplaneModeDialog = false;
    protected AlertDialog airplaneModeDialog = null;
    protected boolean isGPSEnabled = true;
    protected boolean hasGPSDialog = false;
    protected AlertDialog gpsDialog = null;

    public PARFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        this._deviceAttitude = PSKDeviceAttitude.sharedDeviceAttitude();
        this._sensorManager = PSKSensorManager.getSharedSensorManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(this.viewLayoutId, container, false);
        this._mainView = (RelativeLayout) view.findViewWithTag("arMainLayout");
        this._cameraView = (PARCameraView) this._mainView.findViewWithTag("arCameraView");
        this._cameraView.onCreateView();
        this._arView = (PARView) this._mainView.findViewWithTag("arContentView");
        this._arView.setVisibility(View.INVISIBLE);
        this.setARViewShouldBeVisible(true);
        this.orientationHidesARView = false;
        if (!PARController.getInstance().hasValidApiKey()) {
            this.createWatermark();
        }

        this.progressBar = this.createProgressBar();
        this._mainView.addView(this.getProgressBar().getMainLayout());
        if (PARController.DEBUG) {
            this._arView.setBackgroundColor(1073807104);
        } else {
            this._arView.setBackgroundColor(0);
        }

        this._debugTextView = (TextView) this._mainView.findViewWithTag("debugTextView");
        if (this._debugTextView != null && !PARController.DEBUG) {
            this._debugTextView.setVisibility(View.INVISIBLE);
        }

        this._arRadarView = (PARRadarView) this._mainView.findViewWithTag("arRadarView");
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this._arRadarView != null) {
            this._arRadarView.showRadarInMode(1, this);
        }

        activeFragment = this;
        if (!PSKDeviceProperties.sharedDeviceProperties().isARSupported()) {
            this.onARNotSupportedRaised();
        } else {
            this._arView.setVisibility(View.VISIBLE);
            this._sensorManager.startListening();
            this.progressBar.showWithText("Waiting for GPS Signal...");
            this._cameraView.onResume();
            this.startRendering();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        this.stopRendering();
        this.sendARDataAndSystemSpecs();
        this._cameraView.onPause();
        if (this.getRadarView() != null) {
            this.getRadarView().stop();
        }

        this._sensorManager.stopListening();
        this.hadLocationUpdate = false;
        if (activeFragment == this) {
            activeFragment = null;
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        (new Handler()).postDelayed(new Runnable() {
            public void run() {
                PARFragment.this._cameraView.determineDisplayOrientation();
                PARFragment.this._arRadarView.requestLayout();
                PARFragment.this._cameraView.requestLayout();
                PARFragment.this._mainView.requestLayout();
            }
        }, 200L);
    }

    private void onARNotSupportedRaised() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        PSKDeviceProperties props = PSKDeviceProperties.sharedDeviceProperties();
        String errorMsg = "Device does not support AR\n";
        if (props.hasAccelerometer()) {
            errorMsg = errorMsg + "Accelerometer\n";
        }

        if (props.hasCompass()) {
            errorMsg = errorMsg + "Compass\n";
        }

        if (props.hasGyroscope()) {
            errorMsg = errorMsg + "Gyroscope\n";
        }

        if (props.hasGravitySensor()) {
            errorMsg = errorMsg + "GravitySensor\n";
        }

        errorMsg = errorMsg + "is missing";
        builder.setTitle("AR not supported");
        builder.setMessage(errorMsg);
        builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                PARFragment.this.progressBar.hide();
                PARFragment.this._arRadarView.hideRadar();
            }
        });
        builder.show();
    }

    private void startRendering() {
        if (this.renderLoopHandler == null) {
            this.renderLoopHandler = new Handler();
            this.renderRunnable = new Runnable() {
                public void run() {
                    PARFragment.this.updateView();
                    PARFragment.this.renderLoopHandler.postDelayed(this, (long) PARFragment.this.RENDER_INTERVAL);
                }
            };
            this.renderLoopHandler.post(this.renderRunnable);
        }

    }

    private void stopRendering() {
        if (this.renderLoopHandler != null) {
            this.renderLoopHandler.removeCallbacks(this.renderRunnable);
            this.renderRunnable = null;
            this.renderLoopHandler = null;
        }

    }

    public PARProgressBar createProgressBar() {
        return new PARProgressBar(this.getActivity(), (AttributeSet) null, 16842871);
    }

    public static boolean isAirplaneModeOn(Context context) {
        return Build.VERSION.SDK_INT < 17 ? Settings.System.getInt(context.getContentResolver(), "airplane_mode_on", 0) != 0 : Settings.Global.getInt(context.getContentResolver(), "airplane_mode_on", 0) != 0;
    }

    private void sendARDataAndSystemSpecs() {
        if (!this.isDataTrackingExecuted) {
            int degreesLandscape = this._deviceAttitude.getCurrentSurfaceRotation();
            float headingAccuracyMin = PSKSensorManager.getSharedSensorManager().getHeadingAccuracyMin();
            float headingAccuracyMax = PSKSensorManager.getSharedSensorManager().getHeadingAccuracyMax();
//            PARController.getInstance();
//            PARController.dataCollector.addEntry(new BasicNameValuePair(URLEncoder.encode("entry.1937168601"), Integer.toString(degreesLandscape)));
//            PARController.getInstance();
//            PARController.dataCollector.addEntry(new BasicNameValuePair(URLEncoder.encode("entry.2001845173"), Float.toString(headingAccuracyMax)));
//            PARController.getInstance();
//            PARController.dataCollector.addEntry(new BasicNameValuePair(URLEncoder.encode("entry.1022928538"), Float.toString(headingAccuracyMin)));
//            try {
//                PARController.getInstance();
//                PARController.dataCollector.execute(new Void[0]);
//            } catch (IllegalStateException var5) {
//                ;
//            }
            this.isDataTrackingExecuted = true;
        }

    }

    private void updateView() {
        ++this.airplaneModeCounter;
        if (this.airplaneModeCounter > 30) {
            this.airplaneModeCounter = 0;
            this.checkForAirplaneMode();
            this.checkForGPSDisabled();
        }

        if (!this.isInAirplaneMode) {
            if (this.getCurrentARViewVisbility() != View.VISIBLE) {
                if (this._arView.getVisibility() != View.INVISIBLE) {
                    this._arView.setVisibility(View.INVISIBLE);
                }

            } else {
                if (this._arView.getVisibility() != View.VISIBLE) {
                    this._arView.setVisibility(View.VISIBLE);
                    this._arView.requestLayout();
                }

                this._cameraSize = this._cameraView.getViewSize();
                if (!this._hasProjectionMatrix) {
                    Point viewPort = new Point();
                    if (this._cameraSize.x > this._cameraSize.y) {
                        viewPort.set(this._cameraSize.x, this._cameraSize.y);
                    } else {
                        viewPort.set(this._cameraSize.y, this._cameraSize.x);
                    }

                    double fov = PSKDeviceProperties.sharedDeviceProperties().getBackFacingCameraFieldOfView()[0];
                    fov *= 0.017453292519943295D;
                    this._perspectiveMatrix = PSKMath.PSKMatrixCreateProjection(fov, (float) viewPort.x / (float) viewPort.y, 0.25F, 10000.0F);
                    this._hasProjectionMatrix = true;
                }

                this._screenOrientation = this._deviceAttitude.getCurrentSurfaceRotation();
                if (this._screenMargin == null || this._screenOrientation != this._screenOrientationPrevious) {
                    this._screenOrientationOffsetAngle = -PSKMath.deltaAngle(90.0F * (float) this._screenOrientation, 0.0F);
                    this._cameraView.determineDisplayOrientation();
                    this._cameraSize = this._cameraView.getViewSize();
                    int maxScreenSize = Math.max(this._cameraSize.x, this._cameraSize.y);
                    int margin = maxScreenSize - Math.min(this._cameraSize.x, this._cameraSize.y);
                    if (this._cameraSize.x <= this._cameraSize.y) {
                        this._screenMargin = new Point(0, margin);
                    } else {
                        this._screenMargin = new Point(margin, 0);
                    }

                    RelativeLayout.LayoutParams arViewLayoutParams = new RelativeLayout.LayoutParams(maxScreenSize, maxScreenSize);
                    arViewLayoutParams.addRule(13);
                    arViewLayoutParams.leftMargin = -this._screenMargin.x;
                    arViewLayoutParams.rightMargin = -this._screenMargin.x;
                    arViewLayoutParams.topMargin = -this._screenMargin.y;
                    arViewLayoutParams.bottomMargin = -this._screenMargin.y;
                    this._arView.setScreenSize(maxScreenSize);
                    this._arView.setLayoutParams(arViewLayoutParams);
                    this._arView.requestLayout();
                    this._screenOrientationPrevious = this._screenOrientation;
                    this._hasProjectionMatrix = false;
                    Log.i(this.TAG, "update AR View: " + maxScreenSize);
                }

                if (this._cameraSize.x < 1 || this._cameraSize.y < 1) {
                    this._screenMargin = null;
                }

                float orientationRoll = this._deviceAttitude.getOrientationRoll();
                float orientationWithOffset = orientationRoll + this._screenOrientationOffsetAngle;
                if (Math.abs(this._arView.getRotation() - orientationWithOffset) > 1.0F) {
                    this._arView.setRotation(-orientationWithOffset);
                    PARPoi.setViewRotation(this._screenOrientationOffsetAngle);
                }

                this._screenSize.x = this._arView.getWidth();
                this._screenSize.y = this._arView.getHeight();
                if (this._hasProjectionMatrix) {
                    this.drawLabels();
                }

                if (this._needsWaterMark) {
                    this.ensureWatermarkIntegrity();
                    this._watermark.setX((float) (this._screenSize.x - this._watermark.getWidth()) * 0.5F);
                    this._watermark.setY((float) (this._screenSize.y - this._watermark.getHeight()) * 0.5F);
                }

                if (PARController.DEBUG) {
                    this.onUpdateDebugLabel();
                }

            }
        }
    }

    private void ensureWatermarkIntegrity() {
        if (this._watermark.getParent() == null) {
            this._arView.addView(this._watermark);
        } else if (this._watermark.getParent() != this._arView) {
            ((ViewGroup) this._arView.getParent()).removeView(this._watermark);
            this._arView.addView(this._watermark);
        }

        if (this._watermark.getText() != "PanicAR Demo") {
            this._watermark.setText("PanicAR Demo");

        }
        this._watermark.setVisibility(View.INVISIBLE);
    }

    protected void onUpdateDebugLabel() {
        String s = "";
        float[] gravity = this._deviceAttitude.getNormalizedGravity();
        float[] rotationVector = this._deviceAttitude.getRotationVectorRaw();
        s = "DefaultOrientation: " + PSKDeviceAttitude.orientationToString(this._deviceAttitude.getDefaultDisplayOrientation()) + " / CurrentNativeOrientation: " + PSKDeviceAttitude.orientationToString(this._deviceAttitude.getCurrentDisplayOrientation()) + " / CurrentNativeRotation: " + PSKDeviceAttitude.surfaceRotationToString(this._deviceAttitude.getCurrentSurfaceRotation()) + " / CurrentInterfaceOrientation: " + PSKDeviceAttitude.rotationToString(this._deviceAttitude.getCurrentInterfaceRotation()) + "\nCurrentDeviceOrientation: " + PSKDeviceAttitude.rotationToString(this._deviceAttitude.getCurrentDeviceOrientation()) + " / OrientationRoll: " + this._deviceAttitude.getOrientationRoll();
        s = s + "\ngravity (x,y,z) = " + gravity[0] + " / " + gravity[1] + " / " + gravity[2];
        s = s + "\nrotationVector (x,y,z) = " + rotationVector[0] + " / " + rotationVector[1] + " / " + rotationVector[2];
        this._debugTextView.setText(s);
    }

    protected void checkForAirplaneMode() {
        boolean newAirplaneMode = isAirplaneModeOn(this.getActivity());
        if (newAirplaneMode != this.isInAirplaneMode) {
            this.isInAirplaneMode = newAirplaneMode;
            this.onAirplaneModeDetected(newAirplaneMode);
        }

    }

    public void onAirplaneModeDetected(boolean airplaneMode) {
        if (airplaneMode) {
            if (!this.hasAirplaneModeDialog) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
                builder.setTitle("Airplane mode on");
                builder.setMessage("Disable airplane mode to use AR");
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        PARFragment.this.airplaneModeDialog = null;
                        PARFragment.this.hasAirplaneModeDialog = false;
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        PARFragment.this.airplaneModeDialog = null;
                        PARFragment.this.hasAirplaneModeDialog = false;
                        PARFragment.this.getActivity().finish();
                    }
                });
                this.airplaneModeDialog = builder.show();
            }

            this.hasAirplaneModeDialog = true;
        } else if (this.airplaneModeDialog != null) {
            this.airplaneModeDialog.hide();
            this.airplaneModeDialog = null;
            this.hasAirplaneModeDialog = false;
        }

    }

    protected void checkForGPSDisabled() {
        boolean newGPSMode = this._sensorManager.isGPSEnabled();
        if (newGPSMode != this.isGPSEnabled) {
            this.isGPSEnabled = newGPSMode;
            this.onGPSDisabled(newGPSMode);
        }

    }

    public void onGPSDisabled(boolean gpsEnabled) {
        if (!gpsEnabled) {
            if (!this.hasGPSDialog) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
                builder.setTitle("GPS disabled");
                builder.setMessage("Enable GPS to use AR");
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        PARFragment.this.gpsDialog = null;
                        PARFragment.this.hasGPSDialog = false;
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        PARFragment.this.gpsDialog = null;
                        PARFragment.this.hasGPSDialog = false;
                        PARFragment.this.getActivity().finish();
                    }
                });
                this.gpsDialog = builder.show();
            }

            this.hasGPSDialog = true;
        } else if (this.gpsDialog != null) {
            this.gpsDialog.hide();
            this.gpsDialog = null;
            this.hasGPSDialog = false;
        }

    }

    protected void drawLabels() {
        Matrix.multiplyMM(this._perspectiveCameraMatrix, 0, this._perspectiveMatrix, 0,
                this._deviceAttitude.getRotationVectorAttitudeMatrix(), 0);

        ListIterator it = PARController.getPois().listIterator();

        while (it.hasNext()) {
            PARPoi arPoi = (PARPoi) it.next();
            if (!arPoi.isClippedByDistance()) {
                arPoi.renderInView(this);
            }
        }

    }

    private boolean createWatermark() {
        this._needsWaterMark = true;

        try {
            this._watermark = new TextView(this._arView.getContext());
            this._watermark.setText("PanicAR Demo");
            this._watermark.setLayoutParams(new RelativeLayout.LayoutParams(-1, -1));
            this._watermark.setTextSize(2, 40.0F);
            this._watermark.setBackgroundColor(0);
            this._watermark.setTextColor(-1);
            this._watermark.setRotation(-45.0F);
            this._watermark.setGravity(17);
            this._watermark.setSingleLine();
            this._watermark.setIncludeFontPadding(false);
            this._watermark.setVisibility(View.VISIBLE);
            this._arView.addView(this._watermark);
            this._watermark.setVisibility(View.INVISIBLE);
        } catch (Exception var2) {
            var2.printStackTrace();
        }

        return true;
    }

    public PARCameraView getCameraView() {
        return this._cameraView;
    }

    public static PARFragment getActiveFragment() {
        return activeFragment;
    }

    public float[] getPerspectiveCameraMatrix() {
        return this._perspectiveCameraMatrix;
    }

    public int getScreenMarginX() {
        return this._screenMargin.x;
    }

    public int getScreenMarginY() {
        return this._screenMargin.y;
    }

    public Point getScreenSize() {
        return this._screenSize;
    }

    public PARRadarView getRadarView() {
        return this._arRadarView;
    }

    public PARView getARView() {
        return this._arView;
    }

    @Override
    public void onLocationChangedEvent(Location location) {
        this.progressBar.hide();
        this.hadLocationUpdate = true;
    }

    @Override
    public void onDeviceOrientationChanged(PSKDeviceOrientation newOrientation) {
        this.orientationHidesARView = newOrientation == PSKDeviceOrientation.FaceUp || newOrientation == PSKDeviceOrientation.FaceDown;
        this.updateRadarOnOrientationChange(newOrientation);
    }

    public void updateRadarOnOrientationChange(PSKDeviceOrientation newOrientation) {
        if (this._arRadarView != null) {
            if (newOrientation == PSKDeviceOrientation.FaceUp) {
                if (this._arRadarView.radarMode != 2) {
                    this.getRadarView().setRadarToFullscreen();
                    this._mainView.requestLayout();
                }
            } else if (this._arRadarView.radarMode != 1) {
                this.getRadarView().setRadarToThumbnail();
                this._mainView.requestLayout();
            }
        }

    }

    public TextView getDebugTextView() {
        return this._debugTextView;
    }

    public boolean shouldARViewBeVisible() {
        return this.arViewShouldBeVisible;
    }

    public void setARViewShouldBeVisible(boolean arViewShouldBeVisible) {
        this.arViewShouldBeVisible = arViewShouldBeVisible;
    }

    public int getCurrentARViewVisbility() {
        return !this.hadLocationUpdate ? 8 : (this.orientationHidesARView ? 8 : (!this.arViewShouldBeVisible ? 8 : 0));
    }

    public PARProgressBar getProgressBar() {
        return this.progressBar;
    }
}
