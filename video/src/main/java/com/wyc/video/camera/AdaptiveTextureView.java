package com.wyc.video.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wyc.permission.OnPermissionCallback;
import com.wyc.permission.Permission;
import com.wyc.permission.XXPermissions;

import java.util.List;

/**
 * @ProjectName: AndroidFeatureSet
 * @Package: com.wyc.video.camera
 * @ClassName: AdaptiveTextureView
 * @Description: 作用描述
 * @Author: wyc
 * @CreateDate: 2022/6/9 17:52
 * @UpdateUser: 更新者：
 * @UpdateDate: 2022/6/9 17:52
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */
public class AdaptiveTextureView extends TextureView implements TextureView.SurfaceTextureListener {

    private final float mRatio = VideoCameraManager.getInstance().calPreViewAspectRatio();
    private final Paint mPaint = new Paint();

    private final Rect mFocusRect = new Rect();

    private Surface mSurface;

    public AdaptiveTextureView(@NonNull Context context) {
        this(context,null);
    }

    public AdaptiveTextureView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public AdaptiveTextureView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr,0);
    }

    public AdaptiveTextureView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init(){
        mPaint.setAntiAlias(false);
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);

        setSurfaceTextureListener(this);

        setWillNotDraw(false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e){
        if (e.getAction() == MotionEvent.ACTION_DOWN){
            updateFocusRect((int) e.getX(),(int) e.getY());
        }
        return super.onTouchEvent(e);
    }

    private void updateFocusRect(int x,int y){
        int region = 88;
        mFocusRect.set(x - region,y - region,x + region,y + region);
        VideoCameraManager.getInstance().updateFocusRegion(getWidth(),getHeight(), mFocusRect.left,mFocusRect.top,mFocusRect.right,mFocusRect.bottom);
        invalidate();
        postDelayed(()->{
            mFocusRect.setEmpty();
            postInvalidate();},1500);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        if (mRatio <= 0f) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            setMeasuredDimension((int) (MeasureSpec.getSize(heightMeasureSpec) *  mRatio), MeasureSpec.getSize(heightMeasureSpec));
        }
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        surface.setDefaultBufferSize(height,width);
        mSurface = new Surface(surface);
        XXPermissions.with(getContext())
                .permission(Permission.CAMERA)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        VideoCameraManager.getInstance().addSurface(mSurface);
                    }
                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        if (never){

                        }
                    }
                });
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        if (mSurface != null)mSurface.release();
        VideoCameraManager.clear();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

    }
}
