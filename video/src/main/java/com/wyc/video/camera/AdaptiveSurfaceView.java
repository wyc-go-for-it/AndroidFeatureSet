package com.wyc.video.camera;

import android.content.Context;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.wyc.video.Utils;

/**
 * @ProjectName: AndroidFeatureSet
 * @Package: com.wyc.video.camera
 * @ClassName: AdaptiveSurfaceView
 * @Description: 自适应摄像头预览尺寸
 * @Author: wyc
 * @CreateDate: 2022/6/2 15:55
 * @UpdateUser: 更新者：
 * @UpdateDate: 2022/6/2 15:55
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */
public class AdaptiveSurfaceView extends SurfaceView {

    private float mRatio = -1.0f;

    public AdaptiveSurfaceView(Context context) {
        this(context,null);
    }

    public AdaptiveSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public AdaptiveSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr,0);
    }

    public AdaptiveSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mRatio == -1.0f) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {

            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);

            if (Utils.hasNatureRotation(getContext())){
                width = (int) (height * mRatio);
            }else {
                height = (int) (width * mRatio);
            }
            setMeasuredDimension(width, height);
        }
    }

    public void resize(float ratio) {
        mRatio = ratio;
        requestLayout();
        invalidate();
    }

}
