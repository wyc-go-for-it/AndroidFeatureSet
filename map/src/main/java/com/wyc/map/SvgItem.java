package com.wyc.map;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;

class SvgItem {
    Path path;
    private final Region region;
    private boolean isSelected = false;
    private final RectF rectF;
    private final int index;

    public boolean onTouch(float x, float y) {
        if (region.contains((int) x, (int) y)) {
            isSelected = true;
            return true;
        }
        isSelected = false;
        return false;
    }

    public SvgItem(Path path, int index) {
        this.path = path;
        rectF = new RectF();
        path.computeBounds(rectF, true);
        region = new Region();
        region.setPath(path, new Region(new Rect((int) rectF.left
                , (int) rectF.top, (int) rectF.right, (int) rectF.bottom)));
        this.index = index;
    }


    protected void onDraw(Canvas canvas, Paint paint) {
        paint.reset();
        paint.setColor(isSelected ? Color.YELLOW : Color.GRAY);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        canvas.drawPath(path, paint);
    }
}
