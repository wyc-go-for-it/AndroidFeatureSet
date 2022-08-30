package com.wyc.video.recorder;

import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.wyc.video.YUVUtils;
import com.wyc.video.camera.VideoCameraManager;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * @ProjectName: AndroidFeatureSet
 * @Package: com.wyc.video.FFmpeg.ffmpegApi
 * @ClassName: FFMediaCoder
 * @Description: 使用FFmpeg编码
 * @Author: wyc
 * @CreateDate: 2022/8/30 13:48
 * @UpdateUser: 更新者：
 * @UpdateDate: 2022/8/30 13:48
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */
public class FFMediaCoder extends AbstractRecorder {
    private final static int WIDTH = VideoCameraManager.getInstance().getVWidth();
    private final static int HEIGHT = VideoCameraManager.getInstance().getVHeight();
    private final static int FRAME_RATE = VideoCameraManager.getInstance().getMBestFPSRange().getUpper();

    private long mNativeObj = 0;

    private ImageReader mImageReaderYUV = null;
    private HandlerThread mImageReaderThread  = null;
    private Handler mImageReaderHandler = null;

    private final ByteBuffer mYuvBuffer = ByteBuffer.allocate(WIDTH * HEIGHT * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8);

    public FFMediaCoder(){
        mNativeObj = nativeInitCoder(getFile().getAbsolutePath(),FRAME_RATE,WIDTH,HEIGHT);
        Log.e("FFMediaCoder mNativeObj:",String.valueOf(mNativeObj));
    }

    @Override
    public void configure() {
        mImageReaderYUV = ImageReader.newInstance(WIDTH,HEIGHT, ImageFormat.YUV_420_888,2);
    }

    private void stopYUV(){
        if (mImageReaderYUV != null){
            mImageReaderYUV.close();
            mImageReaderYUV = null;
        }
    }

    private void startImageReaderThread(){
        stopImageReaderThread();
        mImageReaderThread = new HandlerThread("ImageReaderThread");
        mImageReaderThread.start();
        mImageReaderHandler = new Handler(mImageReaderThread.getLooper());
    }
    private void stopImageReaderThread() {
        if (mImageReaderThread != null) {
            mImageReaderThread.quitSafely();
            try {
                mImageReaderThread.join();
                mImageReaderThread = null;
                mImageReaderHandler = null;
            } catch (InterruptedException ignore) {
            }
        }
    }

    private final ImageReader.OnImageAvailableListener mImageReaderYUVCallback = reader -> {
        final Image image = reader.acquireLatestImage();
        if (image == null)return;

        if (image.getFormat() == ImageFormat.YUV_420_888){
            final int w = image.getWidth();
            final int h = image.getHeight();

            final Image.Plane[] planes = image.getPlanes();
            final Image.Plane yPlane = planes[0];
            final Image.Plane uPlane = planes[1];
            final Image.Plane vPlane = planes[2];

            final ByteBuffer yBuffer = yPlane.getBuffer();
            final ByteBuffer uBuffer = uPlane.getBuffer();
            final ByteBuffer vBuffer = vPlane.getBuffer();

            mYuvBuffer.rewind();
            mYuvBuffer.put(yBuffer);
            int index = 0;
            int pos = vBuffer.limit();
            int pixelStride = vPlane.getPixelStride();
            while (index < pos - 1){
                mYuvBuffer.put(uBuffer.get(index));
                mYuvBuffer.put(vBuffer.get(index));
                index += pixelStride;
            }

            final byte[] bytes = new byte[w * h * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
            if (VideoCameraManager.getInstance().isBack())
                YUVUtils.rotateYUV_420_90(mYuvBuffer.array(),w,h,bytes);
            else {
                YUVUtils.rotateYUV_420_270(mYuvBuffer.array(),w,h,bytes);
            }

            addData(bytes,1);

            Log.e("",String.format(
                    Locale.CHINA,"yuvWidth:%d,yuvHeight:%d,YpixelStride:%d,YrowStride:%d,VpixelStride:%d,VrowStride:%d,UpixelStride:%d,UrowStride:%d",
                    w,h,yPlane.getPixelStride(),yPlane.getRowStride(),vPlane.getPixelStride(),
                    vPlane.getRowStride(),uPlane.getPixelStride(),uPlane.getRowStride()));

        }
        image.close();
    };

    @NonNull
    @Override
    public Surface getSurface() {
        if (mImageReaderYUV == null)throw new IllegalArgumentException("must invoke configure method.");
        return mImageReaderYUV.getSurface();
    }

    @Override
    public void start() {
        if (mNativeObj != 0){
            nativeStartCoder(mNativeObj);
        }else {
            mNativeObj = nativeInitCoder(getFile().getAbsolutePath(),FRAME_RATE,WIDTH,HEIGHT);
            if (mNativeObj != 0){
                nativeStartCoder(mNativeObj);
            }
        }

        startImageReaderThread();
        mImageReaderYUV.setOnImageAvailableListener(mImageReaderYUVCallback,mImageReaderHandler);
    }

    @Override
    public void stop() {
        if (mNativeObj != 0){
            nativeStopCoder(mNativeObj);
            mNativeObj = 0;
        }
        mImageReaderYUV.setOnImageAvailableListener(null,null);
        stopImageReaderThread();
    }

    @Override
    public void release() {
        stop();
        stopYUV();
        super.release();
    }

    @Override
    protected void finalize() {
        super.finalize();
        if (mNativeObj != 0){
            nativeReleaseCoder(mNativeObj);
            mNativeObj = 0;
        }
    }

    /**
     * @param format 0 I420 1 NV21 2 RGBA
     * */
    private void addData(byte[] data,int format){
        if (mNativeObj != 0){
            nativeAddData(mNativeObj,data,format);
        }
    }

    private native long nativeInitCoder(String file, int frameRadio, int width, int height);
    private native void nativeStartCoder(long nativeObj);
    private native void nativeStopCoder(long nativeObj);
    private native void nativeReleaseCoder(long nativeObj);
    private native void nativeAddData(long nativeObj,byte[] data,int format);

    static {
        System.loadLibrary("mediaCoder");
    }
}
