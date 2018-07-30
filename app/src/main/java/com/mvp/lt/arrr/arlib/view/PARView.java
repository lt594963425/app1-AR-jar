package com.mvp.lt.arrr.arlib.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

/**
 * $activityName
 *
 * @author LiuTao
 * @date 2018/7/27/027
 */


public class PARView  extends RelativeLayout {
    private int screenSize;

    public PARView(Context context) {
        super(context);
    }

    public PARView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PARView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.setMeasuredDimension(this.screenSize, this.screenSize);
    }

    public void setScreenSize(int screenSize) {
        this.screenSize = screenSize;
    }
}
