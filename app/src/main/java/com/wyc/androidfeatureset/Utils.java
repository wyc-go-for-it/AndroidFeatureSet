package com.wyc.androidfeatureset;

import com.wyc.logger.Logger;

public class Utils {
    public static void flipYUV_420ByDiagonal(byte[] src, byte[] des, int width, int height)
    {/*
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

    public static void flipYUV_420ByDiagonalPlus(byte[] src, int width, int height)
    {/*
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

    public static void flipYUV_420ByDiagonalY_axis(byte[] src, int width, int height){
        /*
        YYYY
        YYYY
        VUVU
    */
        int left = 0,right = width - 1;
        for (int i = 0;i < height + height / 2;i ++){
            while (left < right){
                src[left] ^= src[right];
                src[right] ^= src[left];
                src[left] ^= src[right];

                left++;
                right--;
            }
            left = width * (i + 1) ;
            right =  left + width;
        }
    }
}
