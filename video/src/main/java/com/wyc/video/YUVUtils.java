package com.wyc.video;

import android.graphics.Rect;

import androidx.annotation.NonNull;

public class YUVUtils {

    public static void flipYUV_420By_180(byte[] src, int width, int height) {
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

        if (region.left > 0 && region.left + region.width() > s_w || region.top > 0 && region.top + region.height() > s_h){
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
        /* NV21
        YYYY
        YYYY
        VUVU
    */
        if (dst == null)dst = new byte[src.length];

        for (int i = 0;i < s_h;i ++){
            for (int j = 0;j < s_w; j ++){
                dst[(j + 1) * s_h - i - 1] = src[i * s_w + j];
            }
        }

        int tOffset = s_w * s_h - 1,offset,srcOffset,vOffset;
        for (int i = s_h;i <  s_h + s_h / 2;i ++){
            srcOffset = i * s_w;
            vOffset = (i - s_h) * 2;
            for (int j = 0;j < s_w; j +=2){
                offset = tOffset + (j / 2 + 1) * s_h - vOffset;
                dst[offset] = src[srcOffset + j + 1];
                dst[offset - 1] = src[srcOffset + j];
            }
        }

        return dst;
    }

    public static int[] yuv420ToARGB(@NonNull byte[] src, int s_w, int s_h , int[] pixels){
        /*
        NV21

        YYYY
        YYYY
        VUVU

    */
        if (s_w * s_h * 1.5 > src.length){
            throw new IllegalArgumentException("src is too small.");
        }

        if (pixels == null)
            pixels = new int[s_w * s_h];
        else if (pixels.length < s_w * s_h){
            throw new IllegalArgumentException("pixels is too small.");
        }

        int r,g,b;
        int y = 0,v = 0,u = 0;
        int xOffset,yOffset;

        for (int j = 0;j < s_h;j ++){
            xOffset = j * s_w;
            yOffset = s_h * s_w + (j >> 1) * s_w;
            for (int i = 0 ;i < s_w;i++){
                y = (0xff & src[xOffset + i]);

                if ((i & 1) == 0){
                    v = (0xff & src[yOffset++]) - 128;
                    u = (0xff & src[yOffset++]) - 128;
                }

                r = y + v + ((v * 103) >> 8);
                g= y - ((u * 88) >> 8) - ((v * 183) >> 8);
                b= y + u + ((u * 198) >> 8);

                if (r < 0)
                    r = 0;
                else if (r > 255)
                    r = 255;
                if (g < 0)
                    g = 0;
                else if (g > 255)
                    g = 255;
                if (b < 0)
                    b = 0;
                else if (b > 255)
                    b = 255;

                pixels[xOffset + i] = 0xff000000 | (r << 16) | (g << 8) | b;

/*                int y1192 = 1192 * y;
                r = (y1192 + 1634 * v);
                g = (y1192 - 833 * v - 400 * u);
                b = (y1192 + 2066 * u);
                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;


                pixels[xOffset + i] = 0xff000000 | ((r << 6) & 0xff0000)
                        | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);*/
            }
        }

        return pixels;
    }

    public static byte[] rotateYUV_420_270(@NonNull byte[] src, int s_w, int s_h,byte[] dst){
        /*
        YYYY
        YYYY
        VUVU
    */
        if (dst == null)dst = new byte[src.length];

        int srcOffset;
        for (int i = 0;i < s_h;i ++){
            srcOffset = i * s_w;
            for (int j = 0;j < s_w; j++){
                dst[(s_w - 1 - j) * s_h + i ] = src[srcOffset + j];
            }
        }

        int tOffset = s_w * s_h ,offset;
        for (int i = s_h,k = 0;i <  s_h + s_h / 2;i ++,k++){
            srcOffset = i * s_w;
            for (int j = s_w;j > 0; j -=2){
                offset = tOffset + ((s_w - j) >> 1) * s_h - (s_h - i) * 2;
                dst[offset] = src[srcOffset + j - 2];
                dst[offset + 1] = src[srcOffset + j - 1];
            }
        }

        return dst;
    }

    static {
        System.loadLibrary("YUVUtils");
    }

    public static int[] fastYuv420ToARGB(byte[] src, int s_w, int s_h , int[] pixels){
        return nativeYuv420ToARGB(src,s_w,s_h,pixels);
    }
    public static byte[] fastRotateYUV_420_270(byte[] src, int s_w, int s_h,byte[] dst){
        return nativeRotateYUV_420_270(src,s_w,s_h,dst);
    }

    /**
     * @param r==1 旋转角度270，否则90
     * */
    public static byte[] fastRotateYUV_420_270_90(byte[] src, int s_w, int s_h,byte[] dst,int r){
        return nativeRotateYUV_I420_270_90(src,s_w,s_h,dst,r);
    }

    private static native int[] nativeYuv420ToARGB(byte[] src, int s_w, int s_h, int[] pixels);
    private static native byte[] nativeRotateYUV_420_270(@NonNull byte[] src, int s_w, int s_h,byte[] dst);
    private static native byte[] nativeRotateYUV_I420_270_90(@NonNull byte[] src, int s_w, int s_h,byte[] dst,int r);
}
