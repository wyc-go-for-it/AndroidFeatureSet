package com.wyc.video.camera;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

/**
 * @ProjectName: AndroidFeatureSet
 * @Package: com.wyc.video.camera
 * @ClassName: RecordBtn
 * @Description: 摄像机录制按钮
 * @Author: wyc
 * @CreateDate: 2022/6/5 15:55
 * @UpdateUser: 更新者：
 * @UpdateDate: 2022/6/5 15:55
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */
public class RecordBtn extends AppCompatButton {
    private final Paint mPaint = new Paint();
    private float mOutRadius = 0;
    private float mCenter = 0;
    private float mInnerRadius = 0;
    private final float mBorder = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,2,getResources().getDisplayMetrics());
    private MODE mMode = MODE.RECORD;
    private ActionCallback mCallback = null;
    private ValueAnimator mAnimator = null;
    private final int mShortVideoTime = 15;//unit second
    private RECORD_STATUS mRecordStatus = RECORD_STATUS.STOP;
    private RectF mRecordIco = null;
    private Path mTri = null;
    private long mStartRecordTime = 0;
    private float mSweepAngle = 0;

    public RecordBtn(@NonNull Context context) {
        this(context,null);
    }

    public RecordBtn(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public RecordBtn(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        mPaint.setAntiAlias(true);
        setBackgroundColor(Color.TRANSPARENT);
        super.setOnClickListener(v -> {
            if (mCallback != null){
                switch (mMode){
                    case RECORD:
                    {
                        switch (mRecordStatus){
                            case START:
                                mRecordStatus = RECORD_STATUS.STOP;
                                mCallback.finishRecord(System.currentTimeMillis() - mStartRecordTime);
                                mStartRecordTime = 0;
                                break;
                            case STOP:
                                mRecordStatus = RECORD_STATUS.START;
                                mStartRecordTime = System.currentTimeMillis();
                                mCallback.startRecord();
                                break;
                        }
                    }
                        break;
                    case PICTURE:
                        mCallback.takePicture();
                        startAnimation();
                }
            }
        });
    }

    private void startAnimation(){
        final float r = mInnerRadius;
        if (mAnimator == null){
            mAnimator = new ValueAnimator();
            mAnimator.setFloatValues(r,r - 8,r);
            mAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            mAnimator.setRepeatCount(1);
            mAnimator.setDuration(1000);
            mAnimator.addUpdateListener(animation -> {
                mInnerRadius = (float) (Float)animation.getAnimatedValue();
                postInvalidate();
            });
            mAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {

                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mInnerRadius = r;
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }else {
            mAnimator.cancel();
        }
        mAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAnimator!= null){
            mAnimator.cancel();
            mAnimator = null;
        }
        removeCallbacks(runnable);
    }

    public interface ActionCallback{
        void startRecord();
        void finishRecord(long recordTime);
        void takePicture();
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {

    }

    public void setCallback(ActionCallback callback){
        mCallback = callback;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mCallback != null && mMode == MODE.SHORT_RECORD){
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    if (mRecordStatus == RECORD_STATUS.STOP){
                        mOutRadius = mCenter;
                        mRecordStatus = RECORD_STATUS.START;
                        mStartRecordTime = System.currentTimeMillis();
                        mCallback.startRecord();
                        postDelayed(runnable,100);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    removeCallbacks(runnable);
                    if (mRecordStatus == RECORD_STATUS.START){
                        mOutRadius = mCenter * 0.75f;
                        mSweepAngle = 0;
                        mRecordStatus = RECORD_STATUS.STOP;
                        mCallback.finishRecord(System.currentTimeMillis() - mStartRecordTime);
                        postInvalidate();
                    }
                    break;
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            postDelayed(runnable,100);

            mSweepAngle += 360f / (float)mShortVideoTime / 10f;
            if (mSweepAngle > 360){
                mSweepAngle = 0;
                mOutRadius = mCenter * 0.75f;
                removeCallbacks(runnable);
                if (mRecordStatus == RECORD_STATUS.START){
                    mRecordStatus = RECORD_STATUS.STOP;
                    mCallback.finishRecord(System.currentTimeMillis() - mStartRecordTime);
                }
            }
            postInvalidate();
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawOutCircle(canvas);
        drawInnerCircle(canvas);
        drawRecordStatus(canvas);
        drawShortRecordStatus(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mCenter = Math.min(getMeasuredWidth(),getMeasuredHeight()) >> 1;
        mOutRadius = mCenter * 0.75f;
        mInnerRadius = mOutRadius - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,8,getResources().getDisplayMetrics());
    }

    private void drawOutCircle(Canvas canvas){
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mBorder);
        canvas.drawCircle(mCenter,mCenter,mOutRadius - mBorder,mPaint);
    }

    private void drawInnerCircle(Canvas canvas){
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(mCenter,mCenter,mInnerRadius,mPaint);
    }

    private void drawRecordStatus(Canvas canvas){
        if (mMode == MODE.RECORD){
            mPaint.setColor(Color.RED);
            mPaint.setStyle(Paint.Style.FILL);

            switch (mRecordStatus){
                case STOP:
                    if (mTri == null){
                        float triangle_side = mInnerRadius;
                        float first_x = (float) (mCenter - triangle_side / 2 * Math.tan(Math.PI / 6));
                        float first_y = (float) (mCenter - triangle_side / 2);

                        float second_y = (float) (mCenter + triangle_side / 2);

                        float third_x = (float) (mCenter + triangle_side / 2 / Math.cos(Math.PI / 6));
                        float third_y = mCenter ;

                        mTri = new Path();
                        mTri.moveTo(first_x,first_y);
                        mTri.lineTo(first_x,second_y);
                        mTri.lineTo(third_x,third_y);
                    }
                    canvas.drawPath(mTri,mPaint);
                    break;
                case START:
                    if (mRecordIco == null){
                        float w = mInnerRadius / 2f;
                        float left = mCenter - w;
                        float right = mCenter + w;
                        mRecordIco = new RectF(left, left,right,right);
                    }
                    float r = mRecordIco.width() / 8f;
                    canvas.drawRoundRect(mRecordIco,r,r,mPaint);
                    break;
            }
        }
    }

    private void drawShortRecordStatus(Canvas canvas){
        if (mMode == MODE.SHORT_RECORD && mRecordStatus == RECORD_STATUS.START){
            mPaint.setColor(Color.GREEN);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(mBorder);
            canvas.drawArc(mBorder,mBorder,mOutRadius * 2 - mBorder,mOutRadius * 2 - mBorder,-90,mSweepAngle,false,mPaint);
        }
    }

    public enum MODE{
        RECORD,PICTURE,SHORT_RECORD
    }
    private enum RECORD_STATUS{
        START,STOP
    }
}
