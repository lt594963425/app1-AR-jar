package com.mvp.lt.arrr.arlib.poi;

import android.location.Location;

/**
 * $activityName
 *
 * @author LiuTao
 * @date 2018/7/27/027
 */


public class PARPoiLabelAdvanced extends PARPoiLabel {
    public PARPoiLabelAdvanced(Location location, String title, String description, int layoutId, int radarResourceId) {
        super(location, title, description, layoutId, radarResourceId);
    }

    public float getAltitude() {
        return (float)this.getLocation().getAltitude();
    }

    public void setAltitude(float altitude) {
        this.getLocation().setAltitude((double)altitude);
    }

    @Override
    public void updateContent() {
        if(this.hasCreatedView) {
            super.updateContent();
            if(this.isAltitudeEnabled && this.altitudeTextView != null) {
                this.altitudeTextView.setText("" + FORMATTER_DISTANCE_SMALL.format((double)this.getAltitude()) + " altitude");
            }

        }
    }
}
