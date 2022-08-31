//
// Created by Administrator on 2022/8/26.
//

#ifndef ANDROIDFEATURESET_IMAGEDEF_H
#define ANDROIDFEATURESET_IMAGEDEF_H

#include <malloc.h>
#include <string.h>
#include <unistd.h>
#include "stdio.h"
#include "sys/stat.h"
#include "stdint.h"
#include "LogUtil.h"

enum IMAGE_FORMAT{
    IMAGE_FORMAT_RGBA,IMAGE_FORMAT_NV21,IMAGE_FORMAT_NV12,IMAGE_FORMAT_I420,IMAGE_FORMAT_NULL
};

#define IMAGE_FORMAT_RGBA_EXT       "RGB32"
#define IMAGE_FORMAT_NV21_EXT       "NV21"
#define IMAGE_FORMAT_NV12_EXT       "NV12"
#define IMAGE_FORMAT_I420_EXT       "I420"
#define IMAGE_FORMAT_NULL_EXT       "NULL"

class NativeImage
{
private:
    int width = 0;
    int height = 0;
    IMAGE_FORMAT format = IMAGE_FORMAT_NULL;
    uint8_t *ppPlane[3] = {nullptr};
    int pLineSize[3] = {0};

private:
    static void AllocNativeImage(NativeImage * const pImage)
    {
        if (pImage->height == 0 || pImage->width == 0) return;

        switch (pImage->format)
        {
            case IMAGE_FORMAT_RGBA:
            {
                pImage->ppPlane[0] = static_cast<uint8_t *>(malloc(pImage->width * pImage->height * 4));
                pImage->pLineSize[0] = pImage->width * 4;
                pImage->pLineSize[1] = 0;
                pImage->pLineSize[2] = 0;
            }
                break;
            case IMAGE_FORMAT_NV12:
            case IMAGE_FORMAT_NV21:
            {
                pImage->ppPlane[0] = static_cast<uint8_t *>(malloc(pImage->width * pImage->height * 1.5));
                pImage->ppPlane[1] = pImage->ppPlane[0] + pImage->width * pImage->height;
                pImage->pLineSize[0] = pImage->width;
                pImage->pLineSize[1] = pImage->width;
                pImage->pLineSize[2] = 0;
            }
                break;
            case IMAGE_FORMAT_I420:
            {
                pImage->ppPlane[0] = static_cast<uint8_t *>(malloc(pImage->width * pImage->height * 1.5));
                pImage->ppPlane[1] = pImage->ppPlane[0] + pImage->width * pImage->height;
                pImage->ppPlane[2] = pImage->ppPlane[1] + (pImage->width >> 1) * (pImage->height >> 1);
                pImage->pLineSize[0] = pImage->width;
                pImage->pLineSize[1] = pImage->width / 2;
                pImage->pLineSize[2] = pImage->width / 2;
            }
                break;
            default:
                LOGE("NativeImageUtil::AllocNativeImage do not support the format. Format = %d", pImage->format);
                break;
        }
    }

    static void FreeNativeImage(NativeImage * const pImage)
    {
        if (pImage == nullptr || pImage->ppPlane[0] == nullptr) return;
        free(pImage->ppPlane[0]);
        pImage->ppPlane[0] = nullptr;
        pImage->ppPlane[1] = nullptr;
        pImage->ppPlane[2] = nullptr;
    }

    static void CopyNativeImage(const NativeImage * const pSrcImg,NativeImage * const pDstImg)
    {
/*        LOGI("NativeImage::CopyNativeImage src[w,h,format,line0,line1,line2]=[%d, %d, %d,%d, %d, %d],"
             " dst[w,h,format,line0,line1,line2]=[%d, %d, %d,%d, %d, %d]",
             pSrcImg->width, pSrcImg->height, pSrcImg->format, pSrcImg->pLineSize[0], pSrcImg->pLineSize[1], pSrcImg->pLineSize[2],
             pDstImg->width, pDstImg->height, pDstImg->format, pDstImg->pLineSize[0], pDstImg->pLineSize[1], pDstImg->pLineSize[2]);*/

        if(pSrcImg->ppPlane[0] == nullptr) return;

        if(pSrcImg->format != pDstImg->format ||
           pSrcImg->width != pDstImg->width ||
           pSrcImg->height != pDstImg->height)
        {
            LOGE("NativeImage::CopyNativeImage invalid params.");
            return;
        }

        if(pDstImg->ppPlane[0] == nullptr) AllocNativeImage(pDstImg);

        switch (pSrcImg->format)
        {
            case IMAGE_FORMAT_I420:
            {
                // y plane
                if(pSrcImg->pLineSize[0] != pDstImg->pLineSize[0]) {
                    for (int i = 0; i < pSrcImg->height; ++i) {
                        memcpy(pDstImg->ppPlane[0] + i * pDstImg->pLineSize[0], pSrcImg->ppPlane[0] + i * pSrcImg->pLineSize[0], pDstImg->width);
                    }
                }
                else
                {
                    memcpy(pDstImg->ppPlane[0], pSrcImg->ppPlane[0], pDstImg->pLineSize[0] * pSrcImg->height);
                }

                // u plane
                if(pSrcImg->pLineSize[1] != pDstImg->pLineSize[1]) {
                    for (int i = 0; i < pSrcImg->height / 2; ++i) {
                        memcpy(pDstImg->ppPlane[1] + i * pDstImg->pLineSize[1], pSrcImg->ppPlane[1] + i * pSrcImg->pLineSize[1], pDstImg->width / 2);
                    }
                }
                else
                {
                    memcpy(pDstImg->ppPlane[1], pSrcImg->ppPlane[1], pDstImg->pLineSize[1] * pSrcImg->height / 2);
                }

                // v plane
                if(pSrcImg->pLineSize[2] != pDstImg->pLineSize[2]) {
                    for (int i = 0; i < pSrcImg->height / 2; ++i) {
                        memcpy(pDstImg->ppPlane[2] + i * pDstImg->pLineSize[2], pSrcImg->ppPlane[2] + i * pSrcImg->pLineSize[2], pDstImg->width / 2);
                    }
                }
                else
                {
                    memcpy(pDstImg->ppPlane[2], pSrcImg->ppPlane[2], pDstImg->pLineSize[2] * pSrcImg->height / 2);
                }
            }
                break;
            case IMAGE_FORMAT_NV21:
            case IMAGE_FORMAT_NV12:
            {
                // y plane
                if(pSrcImg->pLineSize[0] != pDstImg->pLineSize[0]) {
                    for (int i = 0; i < pSrcImg->height; ++i) {
                        memcpy(pDstImg->ppPlane[0] + i * pDstImg->pLineSize[0], pSrcImg->ppPlane[0] + i * pSrcImg->pLineSize[0], pDstImg->width);
                    }
                }
                else
                {
                    memcpy(pDstImg->ppPlane[0], pSrcImg->ppPlane[0], pDstImg->pLineSize[0] * pSrcImg->height);
                }

                // uv plane
                if(pSrcImg->pLineSize[1] != pDstImg->pLineSize[1]) {
                    for (int i = 0; i < pSrcImg->height / 2; ++i) {
                        memcpy(pDstImg->ppPlane[1] + i * pDstImg->pLineSize[1], pSrcImg->ppPlane[1] + i * pSrcImg->pLineSize[1], pDstImg->width);
                    }
                }
                else
                {
                    memcpy(pDstImg->ppPlane[1], pSrcImg->ppPlane[1], pDstImg->pLineSize[1] * pSrcImg->height / 2);
                }
            }
                break;
            case IMAGE_FORMAT_RGBA:
            {
                if(pSrcImg->pLineSize[0] != pDstImg->pLineSize[0])
                {
                    for (int i = 0; i < pSrcImg->height; ++i) {
                        memcpy(pDstImg->ppPlane[0] + i * pDstImg->pLineSize[0], pSrcImg->ppPlane[0] + i * pSrcImg->pLineSize[0], pDstImg->width * 4);
                    }
                } else {
                    memcpy(pDstImg->ppPlane[0], pSrcImg->ppPlane[0], pSrcImg->pLineSize[0] * pSrcImg->height);
                }
            }
                break;
            default:
            {
                LOGE("NativeImage::CopyNativeImage do not support the format. Format = %d", pSrcImg->format);
            }
                break;
        }

    }

    static void DumpNativeImage(NativeImage *pSrcImg, const char *pPath, const char *pFileName)
    {
        if (pSrcImg == nullptr || pPath == nullptr || pFileName == nullptr) return;

        if(access(pPath, 0) == -1)
        {
            mkdir(pPath, 0666);
        }

        char imgPath[256] = {0};
        const char *pExt = nullptr;
        switch (pSrcImg->format)
        {
            case IMAGE_FORMAT_I420:
                pExt = IMAGE_FORMAT_I420_EXT;
                break;
            case IMAGE_FORMAT_NV12:
                pExt = IMAGE_FORMAT_NV12_EXT;
                break;
            case IMAGE_FORMAT_NV21:
                pExt = IMAGE_FORMAT_NV21_EXT;
                break;
            case IMAGE_FORMAT_RGBA:
                pExt = IMAGE_FORMAT_RGBA_EXT;
                break;
            default:
                pExt = "Default";
                break;
        }

        static int index = 0;
        sprintf(imgPath, "%s/IMG_%dx%d_%s_%d.%s", pPath, pSrcImg->width, pSrcImg->height, pFileName, index, pExt);

        FILE *fp = fopen(imgPath, "wb");

        LOGE("DumpNativeImage fp=%p, file=%s", fp, imgPath);

        if(fp)
        {
            switch (pSrcImg->format)
            {
                case IMAGE_FORMAT_I420:
                {
                    fwrite(pSrcImg->ppPlane[0],
                           static_cast<size_t>(pSrcImg->width * pSrcImg->height), 1, fp);
                    fwrite(pSrcImg->ppPlane[1],
                           static_cast<size_t>((pSrcImg->width >> 1) * (pSrcImg->height >> 1)), 1, fp);
                    fwrite(pSrcImg->ppPlane[2],
                           static_cast<size_t>((pSrcImg->width >> 1) * (pSrcImg->height >> 1)),1,fp);
                    break;
                }
                case IMAGE_FORMAT_NV21:
                case IMAGE_FORMAT_NV12:
                {
                    fwrite(pSrcImg->ppPlane[0],
                           static_cast<size_t>(pSrcImg->width * pSrcImg->height), 1, fp);
                    fwrite(pSrcImg->ppPlane[1],
                           static_cast<size_t>(pSrcImg->width * (pSrcImg->height >> 1)), 1, fp);
                    break;
                }
                case IMAGE_FORMAT_RGBA:
                {
                    fwrite(pSrcImg->ppPlane[0],
                           static_cast<size_t>(pSrcImg->width * pSrcImg->height * 4), 1, fp);
                    break;
                }
                default:
                {
                    LOGE("DumpNativeImage default");
                    break;
                }
            }

            fclose(fp);
            fp = NULL;
        }


    }

    void reset(){
        width = 0;
        height = 0;
        format = IMAGE_FORMAT_NULL;
        ppPlane[0] = nullptr;
        ppPlane[1] = nullptr;
        ppPlane[2] = nullptr;
        pLineSize[0] = 0;
        pLineSize[1] = 0;
        pLineSize[2] = 0;
    }
public:
    NativeImage()noexcept = default;
    NativeImage(IMAGE_FORMAT f,int w,int h){
        width = w;
        height = h;
        format = f;
        AllocNativeImage(this);
    }

    ~NativeImage(){
        FreeNativeImage(this);
    }
    NativeImage(const NativeImage &o){
        if (*this != o){
            FreeNativeImage(this);
            format = o.format;
            width = o.width;
            height = o.height;
            CopyNativeImage(&o,this);
        }
    }
    NativeImage & operator=(const NativeImage &o){
        if (*this != o){
            FreeNativeImage(this);
            format = o.format;
            width = o.width;
            height = o.height;
            CopyNativeImage(&o,this);
        }
        return *this;
    }

    NativeImage(NativeImage &&o) noexcept{
        if (*this != o){
            format = o.format;
            width = o.width;
            height = o.height;
            ppPlane[0] = o.ppPlane[0];
            ppPlane[1] = o.ppPlane[1];
            ppPlane[2] = o.ppPlane[2];
            pLineSize[0] = o.pLineSize[0];
            pLineSize[1] = o.pLineSize[1];
            pLineSize[2] = o.pLineSize[2];

            o.reset();
        }
    }

    NativeImage & operator=(NativeImage &&o) noexcept{
        if (*this != o){
            format = o.format;
            width = o.width;
            height = o.height;
            ppPlane[0] = o.ppPlane[0];
            ppPlane[1] = o.ppPlane[1];
            ppPlane[2] = o.ppPlane[2];
            pLineSize[0] = o.pLineSize[0];
            pLineSize[1] = o.pLineSize[1];
            pLineSize[2] = o.pLineSize[2];

            o.reset();
        }
        return *this;
    }

    bool operator==(const NativeImage &o){
        return format == o.format && width == o.width && height == o.height && ppPlane[0] == o.ppPlane[0]
        && ppPlane[1] == o.ppPlane[1] && ppPlane[2] == o.ppPlane[2] && pLineSize[0] == o.pLineSize[0]
        && pLineSize[1] == o.pLineSize[1] && pLineSize[2] == o.pLineSize[2];
    }

    bool operator!=(const NativeImage &o){
        return format != o.format || width == o.width || height == o.height || ppPlane[0] == o.ppPlane[0]
               || ppPlane[1] == o.ppPlane[1] || ppPlane[2] == o.ppPlane[2] || pLineSize[0] == o.pLineSize[0]
               || pLineSize[1] == o.pLineSize[1] || pLineSize[2] == o.pLineSize[2];
    }

    void dumpInfo(const char *extra = ""){
        const char *pExt = nullptr;
        switch (format)
        {
            case IMAGE_FORMAT_I420:
                pExt = IMAGE_FORMAT_I420_EXT;
                break;
            case IMAGE_FORMAT_NV12:
                pExt = IMAGE_FORMAT_NV12_EXT;
                break;
            case IMAGE_FORMAT_NV21:
                pExt = IMAGE_FORMAT_NV21_EXT;
                break;
            case IMAGE_FORMAT_RGBA:
                pExt = IMAGE_FORMAT_RGBA_EXT;
                break;
            default:
                pExt = IMAGE_FORMAT_NULL_EXT;
                break;
        }
        LOGI("%s NativeImage=%p information -> format:%s,width:%d,height:%d,ppPlane[0]:%p,ppPlane[1]:%p,ppPlane[2]:%p,pLineSize[0]:%d,pLineSize[1]:%d,pLineSize[2]:%d",
             extra, this,pExt,width,height,ppPlane[0],ppPlane[1],ppPlane[2],pLineSize[0],pLineSize[1],pLineSize[2]);
    }

    void setFormat(IMAGE_FORMAT f){
        format = f;
    }
    IMAGE_FORMAT getFormat() const{
        return format;
    }
    void setWidth(int w){
        width = w;
    }
    void setHeight(int h){
        height = h;
    }
    uint8_t *getPlanePtr0() const{
        return ppPlane[0];
    }
    uint8_t *getPlanePtr1() const{
        return ppPlane[1];
    }
    uint8_t *getPlanePtr2() const{
        return ppPlane[2];
    }

    int getPLineSize0() const {
        return pLineSize[0];
    }

    int getPLineSize1() const {
        return pLineSize[1];
    }

    int getPLineSize2() const {
        return pLineSize[2];
    }
};

#endif //ANDROIDFEATURESET_IMAGEDEF_H
