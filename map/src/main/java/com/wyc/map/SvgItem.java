package com.wyc.map;

import static com.wyc.map.Utils.dealColor;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.Log;

class SvgItem {
    private final Path path;
    private final Region pathRegion;
    private final RectF regionRectF;
    private int strokeColor;
    private int fillColor;

    private boolean isSelected = false;
    private String id = "";

    private static final Rect nameRect = new Rect();
    private final Path textPath = new Path();

    private final PointF mTopPoint = new PointF();
    private final PointF mBottomPoint = new PointF();

    private final PointF mLeftPoint = new PointF();
    private final PointF mRightPoint = new PointF();

    public boolean onTouch(float x, float y) {
        if (pathRegion.contains((int) x, (int) y)) {
            isSelected = true;
            return true;
        }
        isSelected = false;
        return false;
    }

    public SvgItem(Path path, String Id) {
        this.path = path;
        regionRectF = new RectF();
        path.computeBounds(regionRectF, true);
        pathRegion = new Region();
        pathRegion.setPath(path, new Region(new Rect((int) regionRectF.left
                , (int) regionRectF.top, (int) regionRectF.right, (int) regionRectF.bottom)));
        this.id = Id;

        calMaxPoint();
    }
    public SvgItem(Path path, String Id,String strokeColor) {
        this(path, Id);
        this.strokeColor = Color.parseColor(dealColor(strokeColor));
    }
    public SvgItem(Path path, String Id,String strokeColor,String fillColor) {
        this(path, Id,strokeColor);
        this.fillColor = Color.parseColor(dealColor(fillColor));
    }

    protected void onDraw(Canvas canvas, Paint paint) {
        paint.reset();
        paint.setColor(isSelected ? Color.YELLOW : fillColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(strokeColor);
        paint.setStrokeWidth(1);
        canvas.drawPath(path, paint);
        paint.setStrokeWidth(0);

        drawName(canvas,paint);
        drawPoint(canvas,paint);
    }

    private void drawName(Canvas canvas, Paint paint){
        if (nameRect.width() > regionRectF.width() && !textPath.isEmpty()){
            canvas.drawTextOnPath(id,textPath,0,0,paint);
        }else {
            paint.getTextBounds(id,0, id.length(),nameRect);
            nameRect.offset((int) (regionRectF.centerX() - ((float) nameRect.width()) / 2f),(int)(regionRectF.centerY() + ((float)nameRect.height()) / 2f));
            canvas.drawText(id, regionRectF.centerX() - ((float) nameRect.width()) / 2f, regionRectF.centerY() + ((float)nameRect.height()) / 2f,paint);
        }

    }
    private void calMaxPoint(){
        float[] pos = new float[2];
        final PathMeasure pathMeasure = new PathMeasure(path,false);
        float pathLen = pathMeasure.getLength();

        Log.e(id + " pathLen", pathLen + "  " + regionRectF);

        final float left = regionRectF.left;
        final float top = regionRectF.top;

        double leftDistance = regionRectF.width();
        double rightDistance = 0.0;
        double topDistance = regionRectF.height();
        double bottomDistance = 0.0;

        for (int i = 0;i <= pathLen;i ++){
            pathMeasure.getPosTan(i,pos,null);
            double d = Math.abs(pos[0] - left);
            if (d <= leftDistance){
                leftDistance = d;
                mLeftPoint.set(pos[0],pos[1]);
            }
            if (d >= rightDistance){
                rightDistance = d;
                mRightPoint.set(pos[0],pos[1]);
            }

            d = Math.abs(pos[1] - top);
            if (d <= topDistance){
                topDistance = d;
                mTopPoint.set(pos[0],pos[1]);
            }
            if (d >= bottomDistance){
                bottomDistance = d;
                mBottomPoint.set(pos[0],pos[1]);
            }
        }
    }

    private void drawPoint(Canvas canvas,Paint paint){
        paint.setColor(Color.RED);
        if (mTopPoint.x != 0 && mTopPoint.y != 0 && mBottomPoint.x != 0 && mBottomPoint.y != 0){
            canvas.drawCircle(mTopPoint.x,mTopPoint.y,2,paint);
            canvas.drawCircle(mBottomPoint.x,mBottomPoint.y,2,paint);
            canvas.drawLine(mTopPoint.x,mTopPoint.y,mBottomPoint.x,mBottomPoint.y,paint);
        }

        if (mLeftPoint.x != 0 && mLeftPoint.y != 0 || mRightPoint.x != 0 && mRightPoint.y != 0){
            canvas.drawCircle(mLeftPoint.x,mLeftPoint.y,2,paint);
            canvas.drawCircle(mRightPoint.x,mRightPoint.y,2,paint);
            canvas.drawLine(mLeftPoint.x,mLeftPoint.y,mRightPoint.x,mRightPoint.y,paint);
        }
    }
}
