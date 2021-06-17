package com.wyc.androidfeatureset.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.io.ByteArrayOutputStream;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * @ProjectName: AndroidFeatureSet
 * @Package: com.wyc.androidfeatureset.camera
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
            drawRoundByXferMode(canvas, bitmap);
        } else {
            super.onDraw(canvas);
        }
    }

    private void drawRoundByXferMode(Canvas canvas, Bitmap bitmap) {

        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        int viewWidth = getWidth();
        int viewHeight = getHeight();


        canvas.save();

        int circle_x = viewWidth >> 1,circle_y = viewHeight >> 1,circle_radius  = Math.min(viewWidth / 2, viewHeight / 2);

        //draw mask
        canvas.drawCircle(circle_x, circle_y,circle_radius, mPaint);

        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        float minScale = Math.min(viewWidth / (float) bitmapWidth, viewHeight / (float) bitmapHeight);
        mMatrix.reset();
        mMatrix.setScale(minScale, minScale);
        mMatrix.postTranslate((viewWidth - (int) (bitmapWidth * minScale)) >> 1,(int)(viewHeight - bitmapHeight * minScale) >> 1);
        canvas.drawBitmap(bitmap, mMatrix, mPaint);
        mPaint.setXfermode(null);

        //draw border
        mPaint.setStrokeWidth(2);
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(circle_x, circle_y,circle_radius, mPaint);
        mPaint.setStyle(Paint.Style.FILL);

        canvas.restore();
    }
}
