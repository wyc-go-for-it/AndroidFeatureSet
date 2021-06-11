package com.wyc.androidfeatureset.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.wyc.logger.Logger;

import java.io.IOException;

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
        mFocusPaint.setColor(Color.GREEN);
        mFocusPaint.setStyle(Paint.Style.STROKE);
        mFocusPaint.setStrokeWidth(2f);

        mFocusArea = new RectF();
        getHolder().addCallback(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN){
            float x = event.getX(),y = event.getY();
            float x_ratio =x / getWidth(),y_ratio = y / getHeight();
            Logger.d("x_ratio:%f,y_ratio:%f",x_ratio,y_ratio);

            int size = 300;
            mCameraManager.focus(x_ratio,y_ratio,size);

            float half_s = size / 2f;
            mFocusArea.set(Math.max(x - half_s,getLeft()),Math.max(y - half_s,getTop()),Math.min(x + half_s,getRight()),Math.min(y + half_s,getBottom()));
            invalidate();
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mFocusArea.isEmpty()){
            canvas.drawRect(mFocusArea,mFocusPaint);
        }
     }

    public void setCamera(CameraManager c){
        mCameraManager = c;
        mCameraManager.setFocusSuccessListener(this);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        setWillNotDraw(false);

        mCameraManager.setPreviewDisplay(holder);
        mCameraManager.startPreview();
        mCameraManager.autoFocus();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        if (holder.getSurface() == null){
            return;
        }
        try {
            mCameraManager.stopPreview();
        } catch (Exception e){
            e.printStackTrace();
        }
        mCameraManager.setPreviewSize(width,height);
        mCameraManager.setPreviewDisplay(holder);
        mCameraManager.startPreview();
        mCameraManager.autoFocus();
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        mCameraManager.releaseCamera();
    }

    @Override
    public void success(Camera camera) {
        if (!mFocusArea.isEmpty()){
            mFocusArea.setEmpty();
            postInvalidate();
        }
    }

    public void takePicture(CameraManager.OnPictureTakenListener l){
        mCameraManager.takePicture(l);
    }
    public void switchCamera(){
        mCameraManager.switchCamera(getHolder());
    }
}
