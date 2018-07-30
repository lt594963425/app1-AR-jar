package com.mvp.lt.arrr.arlib.poi;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PointF;
import android.location.Location;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DecimalFormat;

/**
 * $activityName
 *
 * @author LiuTao
 * @date 2018/7/27/027
 */


public class PARPoiLabel  extends PARPoi {
    protected static final DecimalFormat FORMATTER_DISTANCE_LARGEST = new DecimalFormat("#### km");
    protected static final DecimalFormat FORMATTER_DISTANCE_LARGE = new DecimalFormat("###,## km");
    protected static final DecimalFormat FORMATTER_DISTANCE_SMALL = new DecimalFormat("### m");
    protected final float SMALL_DISTANCE_INTERVAL = 5.0F;
    protected final float LARGE_DISTANCE_INTERVAL = 1000.0F;
    protected static Point defaultSize = new Point(256, 128);
    protected Point size = null;
    protected boolean hasCreatedView;
    protected int layoutId;
    private String TAG = "PARPoiLabel";
    protected String _title;
    protected String _description;
    protected String _distance;
    protected int _iconImageViewResource = -1;
    protected TextView distanceTextView;
    protected TextView altitudeTextView;
    protected TextView titleTextView;
    protected TextView descriptionTextView;
    protected ImageView iconImageView;
    private View.OnClickListener onClickListener;
    protected float _lastUpdateAtDistance;
    protected boolean isAltitudeEnabled = false;
    protected Point offset = new Point();

    public PARPoiLabel() {
    }

    public PARPoiLabel(Location location, String title, int layoutId, int radarResourceId) {
        super(location);
        this._title = title;
        this.offset.set(0, 0);
        this.layoutId = layoutId;
        this.radarResourceId = radarResourceId;
    }

    public PARPoiLabel(Location location, String title, String description, int layoutId, int radarResourceId) {
        super(location);
        this._title = title;
        this._description = description;
        this.offset.set(0, 0);
        this.layoutId = layoutId;
        this.radarResourceId = radarResourceId;
    }

    public PARPoiLabel(Location atLocation) {
        super(atLocation);
    }

    public static Point getDefaultSize() {
        return defaultSize;
    }

    public static void setDefaultSize(Point defaultSize) {
        defaultSize = defaultSize;
    }

    public String getTitle() {
        return this._title;
    }

    public void setTitle(String title) {
        if(this._title != title) {
            this._title = title;
            if(this.titleTextView != null) {
                this.titleTextView.setText(this._title);
            }
        }

    }

    public String getDescription() {
        return this._description;
    }

    public void setDescription(String description) {
        if(this._description != description) {
            this._description = description;
            if(this.descriptionTextView != null) {
                this.descriptionTextView.setText(this._description);
            }
        }

    }

    public int getIconImageViewResource() {
        return this._iconImageViewResource;
    }

    public void setIconImageViewResource(int iconImageViewResource) {
        this._iconImageViewResource = iconImageViewResource;
        if(this.iconImageView != null) {
            if(this._iconImageViewResource > -1) {
                this.iconImageView.setImageResource(this._iconImageViewResource);
                this.iconImageView.setVisibility(View.VISIBLE);
            } else {
                this.iconImageView.setVisibility(View.INVISIBLE);
            }
        }

    }

    public boolean getIsAltitudeEnabled() {
        return this.isAltitudeEnabled;
    }

    public void setIsAltitudeEnabled(boolean isAltitudeEnabled) {
        this.isAltitudeEnabled = isAltitudeEnabled;
    }

    @Override
    public Point getOffset() {
        return this.offset;
    }

    public void setOffset(Point leftTop) {
        this.offset = leftTop;
    }

    @Override
    public void createView() {
        if(this.ctx == null) {
            Log.e(this.TAG, "context is NULL");
        } else {
            LayoutInflater inflater = (LayoutInflater)this.ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if(inflater == null) {
                Log.e(this.TAG, "Layout inflater is null");
            } else {
                this._labelView = (RelativeLayout)inflater.inflate(this.layoutId, (ViewGroup)null);
                if(this.onClickListener != null) {
                    this._labelView.setOnClickListener(this.onClickListener);
                }

                if(this.size == null) {
                    this.size = new Point(defaultSize.x, defaultSize.y);
                }

                Resources r = this._labelView.getResources();
                int width = (int) TypedValue.applyDimension(1, (float)this.size.x, r.getDisplayMetrics());
                int height = (int) TypedValue.applyDimension(1, (float)this.size.y, r.getDisplayMetrics());
                this._labelView.setLayoutParams(new ViewGroup.LayoutParams(width, height));
                this.halfSizeOfView = new PointF((float)(width / 2), (float)(height / 2));
                if(this._backgroundImageResource > -1) {
                    this._labelView.setBackgroundResource(this._backgroundImageResource);
                }

                this.titleTextView = (TextView)this._labelView.findViewWithTag("title");
                this.titleTextView.setText(this._title);
                this.descriptionTextView = (TextView)this._labelView.findViewWithTag("description");
                this.descriptionTextView.setText(this._description);
                this.distanceTextView = (TextView)this._labelView.findViewWithTag("distance");
                this.iconImageView = (ImageView)this._labelView.findViewWithTag("icon");
                this.setIconImageViewResource(this._iconImageViewResource);
                this.altitudeTextView = (TextView)this._labelView.findViewWithTag("altitude");
                if(this.altitudeTextView != null) {
                    if(this.isAltitudeEnabled) {
                        this.altitudeTextView.setVisibility(View.VISIBLE);
                    } else {
                        this.altitudeTextView.setVisibility(View.INVISIBLE);
                    }
                }

                this.hasCreatedView = true;
                this.updateContent();
            }
        }
    }

    @Override
    public void updateContent() {
        if(this.hasCreatedView) {
            double distance = this.distanceToUser;
            if(distance >= 10000.0D) {
                if(Math.abs(distance - (double)this._lastUpdateAtDistance) < 1000.0D) {
                    return;
                }

                distance = Math.floor(distance / 1000.0D);
                this._distance = FORMATTER_DISTANCE_LARGEST.format(distance);
            } else if(distance > 1000.0D) {
                if(Math.abs(distance - (double)this._lastUpdateAtDistance) < 100.0D) {
                    return;
                }

                distance = Math.floor(distance / 1000.0D);
                this._distance = FORMATTER_DISTANCE_LARGE.format(distance);
            } else {
                if(Math.abs(distance - (double)this._lastUpdateAtDistance) < 10.0D) {
                    return;
                }

                distance = Math.floor(distance / 5.0D) * 5.0D;
                this._distance = FORMATTER_DISTANCE_SMALL.format(distance);
            }

            if(this.distanceTextView != null) {
                this.distanceTextView.setText(this._distance + " away");
            }

            this._lastUpdateAtDistance = (float)this.distanceToUser;
        }
    }

    public Point getSize() {
        return this.size;
    }

    public void setSize(Point size) {
        this.size = size;
    }

    public void setSize(int w, int h) {
        this.size = new Point(w, h);
    }

    public View.OnClickListener getOnClickListener() {
        return this.onClickListener;
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
        if(this._labelView != null) {
            this._labelView.setOnClickListener(onClickListener);
        }

    }
}

