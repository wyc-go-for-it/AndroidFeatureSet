package com.wyc.label;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public final class TopDrawableTextView extends androidx.appcompat.widget.AppCompatTextView{
    private float mVerSpacing,mBottomSpacing;
    private final Drawable[] mDrawables;
    private Drawable mTopDrawable;
    private final Paint mPaint = new Paint();;
    private float mDrawableStartScale = 1.0f,mScaleStep = 0.02f;
    float mDrawRotate = 0;
    private boolean mAnimationFlag = false;
    private int mAnimType = 0;//默认缩放动画
    private int mSelectTextColor;
    private int mTopDrawablePosition,mTextPosition;
    private CharSequence mText;

    private int status = 0;

    public TopDrawableTextView(Context context) {
        this(context, null);
    }

    public TopDrawableTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TopDrawableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final int default_color = getResources().getColor(R.color.blue,null);

        final TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TopDrawableTextView, 0, 0);
        final int indexCount = typedArray.getIndexCount();
        for (int i = 0; i < indexCount; i++) {
            int index = typedArray.getIndex(i);
            if (index == R.styleable.TopDrawableTextView_verSpacing) {
                mVerSpacing = typedArray.getDimension(index, 28);
            }else if (index == R.styleable.TopDrawableTextView_animType){
                mAnimType = typedArray.getInt(index, 0);
            }else if (index == R.styleable.TopDrawableTextView_selectTextColor){
                mSelectTextColor = typedArray.getColor(index,default_color);
            }else if (index == R.styleable.TopDrawableTextView_bottomSpacing){
                mBottomSpacing = typedArray.getDimension(index, 0);
            }
        }
        if (mSelectTextColor == 0)mSelectTextColor = default_color;

        mDrawables = getCompoundDrawables();

        mPaint.setAntiAlias(true);

        typedArray.recycle();
    }

    private BitmapDrawable getTopBitmapDrawable(){
        final Drawable[] drawables = mDrawables;
        if (drawables != null){
            final Drawable drawable = drawables[1];
            if (drawable instanceof BitmapDrawable)return (BitmapDrawable)drawable;
        }
        return null;
    }

    @Override
    public void onMeasure(int widthMeaSpec, int heightMeaSpec) {
        super.onMeasure(widthMeaSpec, heightMeaSpec);
    }

    @Override
    public void onLayout(boolean change, int left, int top, int right, int bottom) {
        super.onLayout(change, left, top, right, bottom);
        if (change)adjust();
    }

    private void adjust(){
        final Drawable topDrawable = getTopBitmapDrawable();
        if (topDrawable != null){
            final Rect rect = topDrawable.getBounds();
            final int bound_height = rect.height(),bound_width = rect.width(),view_h = getMeasuredHeight(),view_w = getMeasuredWidth();
            final String text = getText().toString();

            final Rect font_bounds = new Rect();
            mPaint.setTextSize(getTextSize());
            mPaint.getTextBounds(text,0,text.length(),font_bounds);

            int font_h = font_bounds.height(),font_w = font_bounds.width();
            int h = (int) (bound_height + mVerSpacing + font_h);

            int top = (view_h - h) >> 1,left = (view_w - bound_width) >> 1;
            rect.set(rect.left + left,rect.top + top,rect.right + left,rect.bottom + top);

            final int dx = view_w >> 1,dy = (bound_height + (top << 1)) >> 1,t_dx = (view_w - font_w) >> 1,t_dy = (int) (h + (font_h >> 1) - mBottomSpacing);


            mTopDrawablePosition |= dx << 16 ;
            mTopDrawablePosition |= dy & 0x0000FFFF;
            mTextPosition |= t_dx << 16 ;
            mTextPosition |= t_dy & 0x0000FFFF;

            mTopDrawable = topDrawable;
            mText = text;
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        final Drawable topDrawable = mTopDrawable;
        if (topDrawable != null){
            if (mAnimationFlag){
                mPaint.setColor(mSelectTextColor);
                final int dx = mTopDrawablePosition >> 16,dy = mTopDrawablePosition & 0x0000FFFF;
                canvas.save();
                canvas.translate(dx,dy);
                if (mAnimType == 1) {
                    canvas.rotate(mDrawRotate);
                }else{
                    canvas.scale(mDrawableStartScale, mDrawableStartScale);
                }
                canvas.translate(-dx,-dy);
                topDrawable.draw(canvas);
                canvas.restore();
            }else {
                mPaint.setColor(getCurrentTextColor());
                topDrawable.draw(canvas);
            }
            canvas.drawText(mText.toString(),mTextPosition >> 16,mTextPosition & 0x0000FFFF,mPaint);
        }else
            super.onDraw(canvas);
    }

    public void triggerAnimation(boolean b){
        final BitmapDrawable drawable = getTopBitmapDrawable();
        if (drawable != null){
            mAnimationFlag = b;
            if (b) {
                final Bitmap bitmap = drawable.getBitmap();
                final int w = bitmap.getWidth(),h = bitmap.getHeight(),font_color = mSelectTextColor;
                final int[] pixels = new int[w * h];
                bitmap.getPixels(pixels,0,w,0,0,w,h);
                for (int i = 0;i < h;i ++){
                    for (int j = 0;j < w;j ++){
                        final int color = pixels[i * w + j];
                        if (color != 0){
                            final int a = (color >> 24) & 0xff,r = (font_color >> 16) & 0xff,g = (font_color >> 8) & 0xff,bc = font_color & 0xff;
                            pixels[i * w + j] = Utils.getPixel(a,r,g,bc);
                        }
                    }
                }
                Bitmap t = Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888);
                if (status == 1){
                    mTopDrawable = new BitmapDrawable(getResources(),Utils.drawWarnToBitmap(t));
                }else mTopDrawable = new BitmapDrawable(getResources(),t);

                mTopDrawable.setBounds(drawable.getBounds());
                updateDrawableStartScale();
            }else{
                if (status == 1){
                    mTopDrawable = new BitmapDrawable(getResources(),Utils.drawWarnToBitmap(drawable.getBitmap()));
                    mTopDrawable.setBounds(drawable.getBounds());
                }else
                    mTopDrawable = drawable;

                mDrawableStartScale = 1.0f;
                mDrawRotate = 0;
                removeCallbacks(updateRunnable);
                postInvalidate();
            }
        }
    }
    private void updateDrawableStartScale(){
        if (mAnimType == 1){
            mDrawRotate += 9;
            if (mDrawRotate < 360)postDelayed(updateRunnable,5);
        }else {
            if (mDrawableStartScale <=  0.5f)mScaleStep = 0.05f;
            if (mDrawableStartScale >=  1.0f)mScaleStep = -0.05f;
            mDrawableStartScale += mScaleStep;
            if (mDrawableStartScale < 1.0f)postDelayed(updateRunnable,5);
        }
        invalidate();
    }
    private final Runnable updateRunnable = this::updateDrawableStartScale;

    public void normal(){
        if (status != 0){
            status = 0;
            triggerAnimation(false);
        }
    }
    public void warn(){
        if (status == 0){
            status = 1;
            triggerAnimation(false);
        }
    }
    public boolean hasNormal(){
        return status == 0;
    }
}