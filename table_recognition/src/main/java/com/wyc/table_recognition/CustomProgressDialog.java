package com.wyc.table_recognition;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import java.util.Locale;
import java.util.concurrent.locks.LockSupport;

class CustomProgressDialog extends Dialog implements SurfaceHolder.Callback {
    private String szMessage;
    private TextView mMessage;
    private boolean mRestShowTime = true;

    private Paint mPaint;
    private volatile boolean isStart;
    private long mShowTime = 0;
    private Thread mThread;

    public CustomProgressDialog(Context context)
    {
        super(context,R.style.com_wyc_table_CustomProgressDialog);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        hideNavigationBar();
        init();
        initSurfaceView();
    }
    private void hideNavigationBar() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        decorView.setOnSystemUiVisibilityChangeListener(visibility -> decorView.setSystemUiVisibility(uiOptions));
    }

    private void initSurfaceView(){
        final SurfaceView surfaceView = findViewById(R.id.timer_view);
        surfaceView.setZOrderOnTop(true);
        final SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
        surfaceHolder.addCallback(this);

        final Paint paint =  new Paint();
        paint.setTextSize(16);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(getContext().getColor(R.color.r_white));
        mPaint = paint;
    }

    private void init()
    {
        setContentView(R.layout.com_wyc_table_progress_dialog_layout);
        mMessage = findViewById(R.id.title);
        mMessage.setText(szMessage);
        Window window = getWindow();
        if (null != window){
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }
        final View main = findViewById(R.id.progress_linearLayout);
        main.setMinimumWidth(100);

        final GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadii(new float[]{5,5,5,5,5,5,5,5});
        drawable.setStroke(0,0);
        drawable.setColor(App.themeColor());

        main.setBackground(drawable);
    }

    @Override
    public void dismiss(){
        szMessage = "";
        try {
            super.dismiss();
        }catch (Exception ignore){
        }
    }

    @Override
    public void onAttachedToWindow (){
        super.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow(){
        super.onDetachedFromWindow();
        if (mRestShowTime){
            mShowTime = 0;
        }
    }

    public CustomProgressDialog setMessage(final String m){
        szMessage = m;
        return this;
    }

    public CustomProgressDialog refreshMessage(){
        mMessage.post(()-> mMessage.setText(szMessage));
        return this;
    }

    public CustomProgressDialog setRestShowTime(boolean b){
        mRestShowTime = b;
        return this;
    }

    public CustomProgressDialog setCancel(boolean b){
        setCancelable(b);
        return this;
    }

    public static CustomProgressDialog showProgress(final Context context,final String message){
        final CustomProgressDialog progressDialog = new CustomProgressDialog(context);
        progressDialog.setMessage(message).show();
        progressDialog.setCancelable(false);
        return progressDialog;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        isStart = true;
        final Surface surface = holder.getSurface();
        drawTimer(holder,surface);
        mThread = new Thread(() -> {
            while (isStart){
                LockSupport.parkNanos(this,1000L * 1000L * 1000L);
                if (!isStart)break;
                drawTimer(holder,surface);
            }
        });
        mThread.start();
    }
    private void drawTimer(final SurfaceHolder holder, final Surface surface){
        final Canvas canvas = holder.lockCanvas();
        if (canvas != null){
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            final String sz = String.format(Locale.CHINA,"%ds",++mShowTime);
            final Rect textBounds = new Rect();
            mPaint.getTextBounds(sz,0,sz.length(),textBounds);
            final int margin = 8;
            canvas.drawText(sz,holder.getSurfaceFrame().width() - textBounds.width() - margin,textBounds.height() + margin,mPaint);
            if (surface.isValid())holder.unlockCanvasAndPost(canvas);//has already been released,may throw IllegalStateException.
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        isStart = false;
        LockSupport.unpark(mThread);
    }
}