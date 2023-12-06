package com.wyc.map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Scroller;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WycMapView extends View {
    private final List<SvgItem> list = new ArrayList<>();
    private Paint paint;
    private int vectorWidth = -1;
    private final Matrix matrix = new Matrix();
    private final Matrix invertMatrix = new Matrix();
    private float viewScale = -1f;
    private float userScale = 1.0f;
    private boolean initFinish = false;
    private int bgColor;
    private GestureDetector gestureDetector;
    private int offsetX, offsetY;
    private Scroller scroller;
    private float[] points;
    private float[] pointsFocusBefore;
    private float focusX, focusY;
    private ScaleGestureDetector scaleGestureDetector;
    private boolean showDebugInfo = false;
    private static final int MAX_SCROLL = 10000;
    private static final int MIN_SCROLL = -10000;
    private int mapId = R.raw.chongqingsvg;

    public WycMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        bgColor = Color.parseColor("#504F4F");
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.GRAY);
        scroller = new Scroller(getContext());
        gestureDetector = new GestureDetector(getContext(), onGestureListener);
        scaleGestureDetector = new ScaleGestureDetector(getContext(), scaleGestureListener);
    }

    private final ScaleGestureDetector.OnScaleGestureListener scaleGestureListener = new ScaleGestureDetector.OnScaleGestureListener() {

        float lastScaleFactor;
        boolean mapPoint = false;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float[] points = new float[]{detector.getFocusX(), detector.getFocusY()};
            pointsFocusBefore = new float[]{detector.getFocusX(), detector.getFocusY()};
            if (mapPoint) {
                mapPoint = false;
                invertMatrix.mapPoints(points);
                focusX = points[0];
                focusY = points[1];
            }
            float change = scaleFactor - lastScaleFactor;
            lastScaleFactor = scaleFactor;
            userScale += change;
            postInvalidate();
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            lastScaleFactor = 1.0f;
            mapPoint = true;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }
    };

    private final GestureDetector.OnGestureListener onGestureListener = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            boolean result = false;
            float x = event.getX();
            float y = event.getY();
            points = new float[]{x, y};
            invertMatrix.mapPoints(points);
            for (SvgItem item : list) {
                if (item.onTouch(points[0], points[1])) {
                    result = true;
                }
            }
            postInvalidate();
            return result;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            offsetX += -distanceX / userScale;
            offsetY += -distanceY / userScale;
            postInvalidate();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            scroller.fling(offsetX, offsetY, (int) ((int) velocityX / userScale), (int) ((int) velocityY / userScale), MIN_SCROLL,
                    MAX_SCROLL, MIN_SCROLL, MAX_SCROLL);
            postInvalidate();
            return true;
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        scaleGestureDetector.onTouchEvent(event);
        return true;
    }

    public void setMapId(int mapId) {
        this.mapId = mapId;
        userScale=1.0f;
        offsetY=0;
        offsetX=0;
        focusX=0;
        focusY=0;
        new Thread(new DecodeRunnable()).start();
    }

    private class  DecodeRunnable implements Runnable {
        @Override
        public void run() {
            final SvgInfo svgInfo = new SvgInfo(mapId);
            if (Utils.parseMap(getContext(),svgInfo)){
                vectorWidth = svgInfo.getWidth();
                list.clear();
                list.addAll(svgInfo.getSvgItems());
            }
            initFinish = true;
            postInvalidate();
        }
    };


    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            offsetX = scroller.getCurrX();
            offsetY = scroller.getCurrY();
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        if (vectorWidth != -1 && viewScale == -1) {
            int width = getWidth();
            viewScale = width * 1.0f / vectorWidth;
        }
        if (viewScale != -1) {
            float scale = viewScale * userScale;
            matrix.reset();
            matrix.postTranslate(offsetX, offsetY);
            matrix.postScale(scale, scale, focusX, focusY);

            invertMatrix.reset();
            matrix.invert(invertMatrix);
        }
        canvas.setMatrix(matrix);
        canvas.drawColor(bgColor);
        if (initFinish) {
            for (SvgItem item : list) {
                item.onDraw(canvas, paint);
            }
        }
        showDebugInfo(canvas);
    }

    private void showDebugInfo(Canvas canvas) {
        if (!showDebugInfo) {
            return;
        }
        if (points != null) {
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(points[0], points[1], 20, paint);
        }
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(focusX, focusY, 20, paint);


        if (pointsFocusBefore != null) {
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(pointsFocusBefore[0], pointsFocusBefore[1], 20, paint);
        }
    }
}

