package ru.samlib.client.util;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class ResizeAnimation extends Animation {
    private View mView;
    private float mToHeight;
    private float mFromHeight;

    private float mToWidth;
    private float mFromWidth;

    public ResizeAnimation(View v, float deltaWidth, float deltaHeight) {
        mFromHeight = v.getHeight();
        mFromWidth = v.getWidth();
        mToHeight = mFromHeight + deltaHeight;
        mToWidth = mFromWidth + deltaWidth;
        mView = v;
        v.setVisibility(View.VISIBLE);
        setDuration(300);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        float height = (mToHeight - mFromHeight) * interpolatedTime + mFromHeight;
        float width = (mToWidth - mFromWidth) * interpolatedTime + mFromWidth;
        ViewGroup.LayoutParams p = mView.getLayoutParams();
        p.height = (int) height;
        p.width = (int) width;
        if (interpolatedTime == 1 && (mToHeight <= 0 || mFromWidth <= 0)) {
            mView.setVisibility(View.GONE);
        }
        mView.requestLayout();
    }
}