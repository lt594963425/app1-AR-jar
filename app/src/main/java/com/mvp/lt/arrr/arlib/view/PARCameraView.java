package com.mvp.lt.arrr.arlib.view;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * $activityName
 *
 * @author LiuTao
 * @date 2018/7/27/027
 */


public class PARCameraView  extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "PARCameraView";
    private static final int PICTURE_SIZE_MAX_WIDTH = 1280;
    private static final int PREVIEW_SIZE_MAX_WIDTH = 640;
    private int cameraId = 0;
    private Camera camera;
    private SurfaceHolder surfaceHolder;
    private Camera.Size bestPreviewSize;
    private float displaySizeW = 16.0F;
    private float displaySizeH = 9.0F;
    private float resolvedAspectRatioW;
    private float resolvedAspectRatioH;
    private float parentWidth;
    private float parentHeight;
    private DisplayMetrics displaymetrics;

    public PARCameraView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.resolvedAspectRatioW = this.displaySizeW;
        this.resolvedAspectRatioH = this.displaySizeH;
        this.displaymetrics = new DisplayMetrics();
    }

    public PARCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.resolvedAspectRatioW = this.displaySizeW;
        this.resolvedAspectRatioH = this.displaySizeH;
        this.displaymetrics = new DisplayMetrics();
    }

    public PARCameraView(Context context) {
        super(context);
        this.resolvedAspectRatioW = this.displaySizeW;
        this.resolvedAspectRatioH = this.displaySizeH;
        this.displaymetrics = new DisplayMetrics();
    }

    public void onCreateView() {
        this.getHolder().addCallback(this);
        Display display = ((WindowManager)this.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        try {
            if(Build.VERSION.SDK_INT >= 17) {
                display.getRealMetrics(this.displaymetrics);
            } else {
                display.getMetrics(this.displaymetrics);
            }

            this.parentWidth = (float)this.displaymetrics.widthPixels;
            this.parentHeight = (float)this.displaymetrics.heightPixels;
        } catch (Exception var3) {
            var3.printStackTrace();
            this.parentWidth = 1280.0F;
            this.parentHeight = 1280.0F;
        }

    }

    public Point getViewSize() {
        return new Point((int)this.parentWidth, (int)this.parentHeight);
    }

    public void onResume() {
        try {
            this.camera = Camera.open(this.cameraId);
            this.startCameraPreview();
        } catch (Exception var2) {
            Log.e("PARCameraView", "Can't open camera with id " + this.cameraId, var2);
        }
    }

    public void onPause() {
        try {
            if(this.camera != null) {
                this.stopCameraPreview();
                this.camera.release();
            }
        } catch (Exception var2) {
            var2.printStackTrace();
        }

    }

    private synchronized void startCameraPreview() {
        this.determineDisplayOrientation();
        this.setupCamera();

        try {
            this.camera.setPreviewDisplay(this.surfaceHolder);
            this.camera.startPreview();
        } catch (IOException var2) {
            Log.e("PARCameraView", "Can't start camera preview due to IOException", var2);
        } catch (NullPointerException var3) {
            var3.printStackTrace();
        }

    }

    private synchronized void stopCameraPreview() {
        try {
            this.camera.stopPreview();
        } catch (Exception var2) {
            Log.i("PARCameraView", "Exception during stopping camera preview");
        }

    }

    public void determineDisplayOrientation() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(this.cameraId, cameraInfo);
       int rotation = ((WindowManager)this.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        int degrees = 0;
        switch(rotation) {
            case 0:
                this.resolvedAspectRatioW = this.displaySizeW;
                this.resolvedAspectRatioH = this.displaySizeH;
                this.parentWidth = (float)this.displaymetrics.widthPixels;
                this.parentHeight = (float)this.displaymetrics.heightPixels;
                degrees = 0;
                break;
            case 1:
                this.resolvedAspectRatioW = this.displaySizeH;
                this.resolvedAspectRatioH = this.displaySizeW;
                this.parentWidth = (float)this.displaymetrics.heightPixels;
                this.parentHeight = (float)this.displaymetrics.widthPixels;
                degrees = 90;
                break;
            case 2:
                this.resolvedAspectRatioW = this.displaySizeW;
                this.resolvedAspectRatioH = this.displaySizeH;
                this.parentWidth = (float)this.displaymetrics.widthPixels;
                this.parentHeight = (float)this.displaymetrics.heightPixels;
                degrees = 180;
                break;
            case 3:
                this.resolvedAspectRatioW = this.displaySizeH;
                this.resolvedAspectRatioH = this.displaySizeW;
                this.parentWidth = (float)this.displaymetrics.heightPixels;
                this.parentHeight = (float)this.displaymetrics.widthPixels;
                degrees = 270;
        }

        int displayOrientation;
        if(cameraInfo.facing == 1) {
            displayOrientation = (cameraInfo.orientation + degrees) % 360;
            displayOrientation = (360 - displayOrientation) % 360;
        } else {
            displayOrientation = (cameraInfo.orientation - degrees + 360) % 360;
        }

        if(this.camera != null) {
            this.camera.setDisplayOrientation(displayOrientation);
        }

    }

    public void setupCamera() {
        if(this.camera != null) {
            Camera.Parameters parameters = this.camera.getParameters();
            this.bestPreviewSize = this.determineBestPreviewSize(parameters);
            Camera.Size bestPictureSize = this.determineBestPictureSize(parameters);
            parameters.setPreviewSize(this.bestPreviewSize.width, this.bestPreviewSize.height);
            parameters.setPictureSize(bestPictureSize.width, bestPictureSize.height);
            this.camera.setParameters(parameters);
        }

    }

    private Camera.Size determineBestPreviewSize(Camera.Parameters parameters) {
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        return this.determineBestSize(sizes, 640);
    }

    private Camera.Size determineBestPictureSize(Camera.Parameters parameters) {
        List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
        return this.determineBestSize(sizes, 1280);
    }

    protected Camera.Size determineBestSize(List<Camera.Size> sizes, int widthThreshold) {
        Camera.Size bestSize = null;
        Iterator i$ = sizes.iterator();

        while(i$.hasNext()) {
            Camera.Size currentSize = (Camera.Size)i$.next();
            boolean isDesiredRatio = (float)currentSize.width / this.resolvedAspectRatioW == (float)currentSize.height / this.resolvedAspectRatioH;
            boolean isBetterSize = bestSize == null || currentSize.width > bestSize.width;
            boolean isInBounds = currentSize.width <= 1280;
            if(isDesiredRatio && isInBounds && isBetterSize) {
                bestSize = currentSize;
            }
        }

        if(bestSize == null) {
            return (Camera.Size)sizes.get(0);
        } else {
            return bestSize;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        float originalWidth = (float)MeasureSpec.getSize((int)this.parentWidth);
        float originalHeight = (float)MeasureSpec.getSize((int)this.parentHeight);
        float width = originalWidth;
        float height = originalHeight;
        float parentWidth = (float)((ViewGroup)this.getParent()).getMeasuredWidth();
        float parentHeight = (float)((ViewGroup)this.getParent()).getMeasuredHeight();
        if(originalWidth > originalHeight * this.getResolvedAspectRatio()) {
            width = originalHeight / this.getResolvedAspectRatio() + 0.5F;
        } else {
            height = originalWidth * this.getResolvedAspectRatio() + 0.5F;
        }

        this.setX((parentWidth - width) * 0.5F);
        this.setY((parentHeight - height) * 0.5F);
        this.setMeasuredDimension((int)width, (int)height);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        this.surfaceHolder = holder;

        try {
            this.startCameraPreview();
        } catch (Exception var3) {
            var3.printStackTrace();
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    private float getResolvedAspectRatio() {
        return this.resolvedAspectRatioW / this.resolvedAspectRatioH;
    }
}
