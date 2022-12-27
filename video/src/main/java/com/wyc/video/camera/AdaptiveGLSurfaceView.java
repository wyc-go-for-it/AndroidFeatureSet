package com.wyc.video.camera;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class AdaptiveGLSurfaceView extends GLSurfaceView {
    private final float mRatio = VideoCameraManager.getInstance().calPreViewAspectRatio();

    public AdaptiveGLSurfaceView(Context context) {
        super(context);
    }

    public AdaptiveGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mRatio <= 0f) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            setMeasuredDimension((int) (MeasureSpec.getSize(heightMeasureSpec) * mRatio), MeasureSpec.getSize(heightMeasureSpec));
        }
    }
}
