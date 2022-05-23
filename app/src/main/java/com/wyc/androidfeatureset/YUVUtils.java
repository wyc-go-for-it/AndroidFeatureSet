package com.wyc.androidfeatureset;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;

import androidx.annotation.NonNull;

import com.wyc.logger.Logger;

import java.util.Arrays;
import java.util.Locale;

public class YUVUtils {
    public static void flipYUV_420ByDiagonal(byte[] src, byte[] des, int width, int height) {
        /*
        YYYY
        YYYY
        VUVU
    */

        int wh = width * height;
        int k = 0;
        for(int i=height - 1;i >= 0;i--) {
            for(int j=width - 1;j >= 0;j--)
            {
                des[k] = src[width*i + j];
                k++;
            }
        }

        for(int i=height / 2 - 1;i >= 0;i--) {
            for(int j=width - 1;j >=0 ;j-=2)
            {
                des[k] = src[wh+ width*i + j - 1];
                des[k+1]=src[wh + width*i + j];
                k+=2;
            }
        }
    }

    public static void flipYUV_420ByDiagonalPlus(byte[] src, int width, int height) {
        /*
        YYYY
        YYYY
        VUVU
    */
        int left = -1,right = width * height ;
        while (left++ < right--){
            src[left] ^= src[right];
            src[right] ^= src[left];
            src[left] ^= src[right];
        }
        left = width * height;
        right = width * (height + height /2);
        while (left++ < right--){
            src[left] ^= src[right];
            src[right] ^= src[left];
            src[left] ^= src[right];
        }

    }

    public static void flipYUV_420ByY_axis(byte[] src, int width, int height){
        /*
        YYYY
        YYYY
        VUVU
    */
        int left = 0,right = width;
        for (int i = 0;i < height;i ++){
            while (left < right){
                src[left] ^= src[right-1];
                src[right-1] ^= src[left];
                src[left] ^= src[right-1];

                left++;
                right--;
            }
            left = width * (i + 1) ;
            right =  left + width;
        }
        for (int i = height ;i < height + height / 2;i ++){
            while (left + 2 < right){
                src[left] ^= src[right-2];
                src[left + 1] ^= src[right-1];

                src[right-2] ^= src[left];
                src[right-1] ^= src[left + 1];

                src[left] ^= src[right-2];
                src[left + 1] ^= src[right-1];

                left +=2;
                right -=2;
            }
            left = width * (i + 1) ;
            right =  left + width;
        }
    }

    public static void flipYUV_420ByX_axis(byte[] src, int width, int height){
        /*
        YYYY
        YYYY
        VUVU
    */
        int top = 0,bottom = height - 1;

        byte[] line = new byte[width];
        int left,lPos,rPos;
        while (top < bottom){

            System.arraycopy(src,top * width,line,0,width);
            System.arraycopy(src,bottom * width,src,top * width,width);
            System.arraycopy(line,0,src,bottom * width,width);
/*            left = -1;
            lPos = top * width;
            rPos = bottom * width;
            while (left++ < width){
                src[lPos + left] ^= src[rPos + left];
                src[rPos + left] ^= src[lPos + left];
                src[lPos + left] ^= src[rPos + left];
            }*/

            top++;
            bottom--;
        }
        top = height;
        bottom = height + height / 2 - 1;
        while (top < bottom){

            System.arraycopy(src,top * width,line,0,width);
            System.arraycopy(src,bottom * width,src,top * width,width);
            System.arraycopy(line,0,src,bottom * width,width);

/*            left = 0;
            lPos = top * width;
            rPos = bottom * width;
            while (++left < width){
                src[lPos + left] ^= src[rPos + left];
                src[rPos + left] ^= src[lPos + left];
                src[lPos + left] ^= src[rPos + left];
            }*/

            top++;
            bottom--;
        }
    }

    public static void clipYUV_420(@NonNull byte[] src, int s_w, int s_h,@NonNull byte[] dst, @NonNull Rect region){
        /*
        YYYY
        YYYY
        VUVU
    */
        if (s_w * s_h * 1.5 > src.length){
            throw new IllegalArgumentException("src is too small.");
        }

        if (region.left + region.width() > s_w || region.top + region.height() > s_h){
            throw new IllegalArgumentException("clip region is not in src.");
        }

        int d_w = region.width(),d_h = region.height();
        //y
        int yOffset = region.top & (~1);
        int xOffset = region.left & (~1);

        int line = yOffset * s_w;
        for (int y = 0; y < d_h; y++) {
            System.arraycopy(src, line + xOffset, dst, y * d_w, d_w);
            line += s_w;
        }
        line = s_w * (s_h + (yOffset >> 1));
        for (int y =  d_h ; y < d_h + d_h / 2; y++) {
            System.arraycopy(src, line + xOffset, dst, y * d_w, d_w);
            line +=  s_w;
        }
    }

    public static byte[] rotateYUV_420_90(@NonNull byte[] src, int s_w, int s_h,byte[] dst){
        /*
        YYYY
        YYYY
        VUVU
    */
        if (dst == null)dst = new byte[src.length];

        int top = 0;

        byte[] line = new byte[s_w];

        while (top < s_h){
            System.arraycopy(src,top * s_w,line,0,s_w);
            for (int i = 0,len = line.length;i < len;i++){
                dst[(i + 1) * s_h - top - 1] = line[i];
            }
            top++;
        }

        int tOffset = s_h * s_w,offset;
        int h = 0;
        while (top < s_h + s_h / 2){
            System.arraycopy(src,top * s_w,line,0,s_w);
            for (int i = 0,k = 0,len = line.length;i < len;i+=2,k++){
                offset = tOffset + (k + 1) * s_h - h - 1;
                dst[offset ] = line[i+ 1];
                dst[offset - 1] = line[i];
            }
            h+=2;
            top++;
        }
        return dst;
    }

    public static Bitmap yuv420ToBitmap(@NonNull byte[] src, int s_w, int s_h,Bitmap dst){
        /*
        NV21

        YYYY
        YYYY
        VUVU

    */
        if (s_w * s_h * 1.5 > src.length){
            throw new IllegalArgumentException("src is too small.");
        }
        if (dst == null)dst = Bitmap.createBitmap(s_w,s_h,Bitmap.Config.ARGB_8888);

        int r,g,b;
        int y,v,u;
        int k;
        int xOffset,yOffset;
        int newPixel = 0;
        for (int j = 0;j < s_h;j ++){
            k = -2;
            xOffset = j * s_w;
            yOffset = s_h * s_w + (j >> 1) * s_w;
            for (int i = 0 ;i < s_w;i++ ){
                y = (0xff & src[xOffset + i]);

                if (i % 2 == 0)k += 2;

                v = (0xff & src[yOffset + k]) - 128;
                u = (0xff & src[yOffset +  k + 1]) - 128;

                r = y + v + ((v * 103) >> 8);
                g= y - ((u * 88) >> 8) - ((v * 183) >> 8);
                b= y + u + ((u * 198) >> 8);


                newPixel |= (0xff);
                newPixel = newPixel << 8 | r & 0xff ;
                newPixel = newPixel << 8 | g & 0xff ;
                newPixel = newPixel << 8 | b & 0xff ;

                dst.setPixel(i,j,newPixel);
            }
        }
        return dst;
    }

}
