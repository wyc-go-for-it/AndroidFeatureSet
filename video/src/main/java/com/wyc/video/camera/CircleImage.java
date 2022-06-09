package com.wyc.video.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * @ProjectName: AndroidFeatureSet
 * @Package: com.wyc.androidfeatureset.video
 * @ClassName: CircleImage
 * @Description: CircleImage
 * @Author: wyc
 * @CreateDate: 2021-06-11 11:39
 * @UpdateUser: 更新者
 * @UpdateDate: 2021-06-11 11:39
 * @UpdateRemark: 更新说明
 * @Version: 1.0
 */
public class CircleImage extends AppCompatImageView {
    private Paint mPaint;
    private Matrix mMatrix;
    private float mRotationVal;
    public CircleImage(@NonNull Context context) {
        this(context,null);
    }

    public CircleImage(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CircleImage(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    private void init() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.WHITE);

        mMatrix = new Matrix();
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        final Drawable drawable = getDrawable();
        if (drawable instanceof BitmapDrawable) {
            final Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) drawRoundByXfermode(canvas, bitmap);
        } else {
            super.onDraw(canvas);
        }
    }

    private void drawRoundByXfermode(Canvas canvas, Bitmap bitmap) {

        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        int viewWidth = getWidth();
        int viewHeight = getHeight();


        canvas.save();

        int circle_x = viewWidth >> 1,circle_y = viewHeight >> 1,circle_radius  = Math.min(viewWidth / 2, viewHeight / 2) - 2;

        //draw mask
        canvas.drawCircle(circle_x, circle_y,circle_radius, mPaint);

        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        mMatrix.reset();
        mMatrix.postRotate(mRotationVal,viewWidth >> 1,viewHeight >> 1);
        if (bitmapWidth > viewWidth && bitmapHeight > viewHeight){
            final Bitmap b = Bitmap.createBitmap(bitmap, (bitmapWidth - viewWidth) >> 1, (bitmapHeight - viewHeight) >> 1,viewWidth,viewHeight);
            canvas.drawBitmap(b,mMatrix,mPaint);
            b.recycle();
        }else {
            mMatrix.postTranslate((viewWidth - bitmapWidth) >> 1,(viewHeight - bitmapHeight) >> 1);
            canvas.drawBitmap(bitmap,mMatrix,mPaint);
        }
        mPaint.setXfermode(null);

        //draw border
        mPaint.setStrokeWidth(2);
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(circle_x, circle_y,circle_radius, mPaint);
        mPaint.setStyle(Paint.Style.FILL);

        canvas.restore();
    }
    public void setMatrix(float a){
        if ((int)mRotationVal != (int)a){
            mRotationVal = a;
            postInvalidate();
        }
    }
}
