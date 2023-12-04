package com.wyc.map;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

class SvgItem {
    private final Path path;
    private final Region pathRegion;
    private final RectF regionRectF;

    private boolean isSelected = false;
    private String name = "";

    private final Rect nameRect = new Rect();
    private final List<Point> maxTop = new ArrayList<>();
    private final List<Point> maxBottom = new ArrayList<>();
    private final Path textPath = new Path();

     public boolean onTouch(float x, float y) {
         findMaxTopPoint();
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
        this.name = Id;
    }


    protected void onDraw(Canvas canvas, Paint paint) {
        paint.reset();
        paint.setColor(isSelected ? Color.YELLOW : Color.GRAY);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        canvas.drawPath(path, paint);

        drawName(canvas,paint);

        drawPoint(canvas,paint);
    }

    private void drawName(Canvas canvas, Paint paint){
        if (nameRect.width() > regionRectF.width()){
            canvas.drawTextOnPath(name,textPath,0,0,paint);
        }else {
            paint.getTextBounds(name,0,name.length(),nameRect);
            nameRect.offset((int) (regionRectF.centerX() - ((float) nameRect.width()) / 2f),(int)(regionRectF.centerY() + ((float)nameRect.height()) / 2f));
            canvas.drawText(name, regionRectF.centerX() - ((float) nameRect.width()) / 2f, regionRectF.centerY() + ((float)nameRect.height()) / 2f,paint);
        }
    }

    private void findMaxTopPoint(){
        if (maxTop.isEmpty() && maxBottom.isEmpty()){
            int top = (int) regionRectF.top + 1;
            int bottom = (int) regionRectF.bottom - 1;
            int left = (int) regionRectF.left;

            int width = (int) regionRectF.width();
            for (int i = left; i < width + left; i ++){
                if (maxTop.isEmpty() && pathRegion.contains(i,top)){
                    Log.e(name,"x:" + i + ",top:" + top);
                    maxTop.add(new Point(i,top));
                }

                if (maxBottom.isEmpty() && pathRegion.contains(i,bottom)){
                    Log.e(name,"x:" + i + ",bottom:" + bottom);
                    maxBottom.add(new Point(i,bottom));
                }
            }
        }
    }

    private void drawPoint(Canvas canvas,Paint paint){
         if (!maxTop.isEmpty() && !maxBottom.isEmpty()){
             for (int i = 0,size = maxTop.size();i < size;i ++){
                 Point p = maxTop.get(i);

                 canvas.drawCircle(p.x,p.y,5,paint);
             }
             for (int i = 0,size = maxBottom.size();i < size;i ++){
                 Point p = maxBottom.get(i);

                 canvas.drawCircle(p.x,p.y,5,paint);
             }
             Point top = maxTop.get(maxTop.size() - 1);
             Point bottom = maxBottom.get(maxBottom.size() - 1);
             canvas.drawLine(top.x,top.y,bottom.x,bottom.y,paint);

             textPath.moveTo(top.x,top.y);
             textPath.lineTo(bottom.x,bottom.y);
         }
    }
}
