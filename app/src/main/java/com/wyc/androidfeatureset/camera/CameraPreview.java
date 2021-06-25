package com.wyc.androidfeatureset.camera;

import android.Manifest;
import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.wyc.logger.Logger;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * @ProjectName: AndroidFeatureSet
 * @Package: com.wyc.androidfeatureset.camera
 * @ClassName: CameraPreview
 * @Description: 相机预览类
 * @Author: wyc
 * @CreateDate: 2021-06-09 15:05
 * @UpdateUser: 更新者
 * @UpdateDate: 2021-06-09 15:05
 * @UpdateRemark: 更新说明
 * @Version: 1.0
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback,CameraManager.OnFocusSuccessListener{
    private CameraManager mCameraManager;
    private final RectF mFocusArea;
    private final Paint mFocusPaint;
    private int mWidth = -1,mHeight = -1;
    private Point mPreSize;

    private ValueAnimator animator;
    private float mCurValue;
    private PathMeasure mPathMeasure;
    private Path mDstPath,mCirclePath;
    private boolean isStartAnimator;

    public CameraPreview(Context context) {
        this(context,null);
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr,0);
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mFocusPaint = new Paint();
        mFocusPaint.setAntiAlias(true);
        mFocusPaint.setStyle(Paint.Style.STROKE);

        initCameraManager();
        mFocusArea = new RectF();
        getHolder().addCallback(this);

        initAnimator();
    }
    private void initCameraManager(){
        mCameraManager = new CameraManager(getContext());
        mCameraManager.setFocusSuccessListener(this);
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putInt("cameraId",mCameraManager.getCameraId());
        bundle.putParcelable("super",super.onSaveInstanceState());
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle)state;
        Parcelable s = bundle.getParcelable("super");
        int id = bundle.getInt("cameraId");
        mCameraManager.setCameraId(id);
        super.onRestoreInstanceState(s);
    }

    private void initAnimator(){
        animator = ValueAnimator.ofFloat(0,2);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                isStartAnimator = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isStartAnimator = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isStartAnimator = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animator.addUpdateListener(animation -> {
            mCurValue = animation.getAnimatedFraction();
            invalidate();
        });
        animator.setDuration(1000);

        mCirclePath = new Path();
        mPathMeasure = new PathMeasure();
        mDstPath = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mFocusArea.isEmpty()){
            mFocusPaint.setColor(Color.GREEN);
            mFocusPaint.setStrokeWidth(2f);
            canvas.drawRect(mFocusArea,mFocusPaint);
        }
        if (isStartAnimator){
            mFocusPaint.setColor(Color.RED);
            mFocusPaint.setStrokeWidth(5f);
            drawWait(canvas);
        }
    }
    private void drawWait(Canvas canvas){
        float len = mPathMeasure.getLength();
        float stop = mCurValue * len;
        float start = stop - ((1 - mCurValue) *len);

        mDstPath.reset();
        mDstPath.moveTo(getX()/ 2,getY() / 2);
        mPathMeasure.getSegment(start,stop ,mDstPath,true);
        canvas.drawPath(mDstPath,mFocusPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (-1 == mWidth || -1 == mHeight) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            setMeasuredDimension(mWidth, mHeight);
        }
        mCirclePath.addCircle(getMeasuredWidth() >> 1,getMeasuredHeight() >> 1,48,Path.Direction.CW);
        mPathMeasure.setPath(mCirclePath,false);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    public void resize(int width, int height) {
        final ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.width = layoutParams.width - width -  getWidth();

        mWidth = width;
        mHeight = height;
        getHolder().setFixedSize(width, height);
        requestLayout();
        invalidate();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN){
            if (!isStartAnimator){
                float x = event.getX(),y = event.getY();
                float x_ratio =x / getWidth(),y_ratio = y / getHeight();
                Logger.d("x_ratio:%f,y_ratio:%f",x_ratio,y_ratio);

                int size = 300;
                mCameraManager.focus(x_ratio,y_ratio,size);

                float half_s = size / 2f;
                mFocusArea.set(Math.max(x - half_s,getLeft()),Math.max(y - half_s,getTop()),Math.min(x + half_s,getRight()),Math.min(y + half_s,getBottom()));
                invalidate();
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        setWillNotDraw(false);
        mCameraManager.initCamera();
        int width = getWidth(),height = getHeight();
        mPreSize = mCameraManager.findBestPreviewSizeValue(width,height);
        if (height >= width)
            resize(Math.max(height * mPreSize.y/mPreSize.x,width),height);
        else
            resize(width,width * mPreSize.x/mPreSize.y);
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        if (holder.getSurface() == null)return;
        Logger.d("surfaceChanged width:%d,height:%d",width,height);

        mCameraManager.stopPreview();
        mCameraManager.setPreviewDisplay(holder);

        if (mPreSize != null)
            mCameraManager.setPreviewSize(mPreSize.x,mPreSize.y);

        mCameraManager.startPreview();
        mCameraManager.autoFocus();
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        mCameraManager.releaseCamera();
    }

    @Override
    public void onFocusSuccess(Camera camera) {
        if (!mFocusArea.isEmpty()){
            mFocusArea.setEmpty();
            postInvalidate();
        }
    }

    public void takePicture(Subscribe s){
        mCameraManager.takePicture(s);
    }

    public static abstract class Subscribe implements CameraManager.OnPictureTakenListener{
        private final CameraPreview view;
        public Subscribe(CameraPreview v){
            view = v;
        }
        @Override
        public final void start() {
            view.animator.start();
        }

        @Override
        public final void finish() {
            view.animator.end();
        }
    }

    public void switchCamera(){
        mCameraManager.switchCamera(getHolder());
    }
    public String getPicDir(){
        return mCameraManager.getPicDir();
    }
    public void setPreviewBack(CameraManager.OnPreviewListener cb){
        mCameraManager.setPreviewCb(cb);
    }
}
