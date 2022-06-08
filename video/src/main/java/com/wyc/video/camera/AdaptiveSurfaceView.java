package com.wyc.video.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.wyc.permission.OnPermissionCallback;
import com.wyc.permission.Permission;
import com.wyc.permission.XXPermissions;

import java.util.List;

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
public class AdaptiveSurfaceView extends SurfaceView implements SurfaceHolder.Callback  {

    private float mRatio = -1.0f;
    private CameraManager mCameraManager;
    private final Paint mPaint = new Paint();

    private final Rect mFocusRect = new Rect();

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
        init();
    }

    private void init(){
        mPaint.setAntiAlias(false);
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);

        getHolder().addCallback(this);

        setWillNotDraw(false);
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.mCameraManager = cameraManager;
        mRatio = mCameraManager.calPreViewAspectRatio();
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
        mCameraManager.updateFocusRegion(getWidth(),getHeight(), mFocusRect.left,mFocusRect.top,mFocusRect.right,mFocusRect.bottom);
        invalidate();
        postDelayed(()->{
            mFocusRect.setEmpty();
            postInvalidate();},1500);
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if (!mFocusRect.isEmpty())
            canvas.drawRect(mFocusRect,mPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mRatio <= 0f) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            setMeasuredDimension((int) (MeasureSpec.getSize(heightMeasureSpec) * mRatio), MeasureSpec.getSize(heightMeasureSpec));
        }
    }

    @Override
    protected void onLayout(boolean changed,int l,int t,int r,int b){
        super.onLayout(changed, l, t, r, b);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        XXPermissions.with(getContext())
                .permission(Permission.CAMERA)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        mCameraManager.openCamera(holder.getSurface());
                    }
                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        if (never){

                        }
                    }
                });
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        mCameraManager.releaseResource();
    }

}
