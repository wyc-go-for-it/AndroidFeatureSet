package com.wyc.androidfeatureset.camera;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.wyc.video.YUVUtils;
import com.wyc.androidfeatureset.R;
import com.wyc.logger.Logger;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.schedulers.Schedulers;


public class CaptureActivity extends AppCompatActivity implements SensorEventListener {
    private static final int CAMERA_PERMISSION = 1000;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private final float[] orientationVals = new float[3];
    private final float[] mRotationMatrix = new float[16];

    @BindView(R.id.preview)
    CameraPreview preview;
    @BindView(R.id.pic_view)
    CircleImage thumbnail_view;
    @BindView(R.id.small_preview)
    ImageView small_preview;

    private byte[] mCacheMatrix;
    private Bitmap mCacheBitmap;
    private int[] mCachePixel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_activy);
        ButterKnife.bind(this);

        checkCameraPermission();

        initThumb();
        initSensor();
     }

    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS){
                //format NV21
                preview.setPreviewBack((data, format, w, h) -> {

                    w = (w + 16 -1) & (-16);

                    int left = 0,top = 0;
                    final Rect rect = new Rect(left,top,w,h);

                    int width = rect.width();
                    int height = rect.height();

                    if (mCacheMatrix == null){
                        mCacheMatrix = new byte[width * (height + height / 2)];
                    }

                    YUVUtils.clipYUV_420(data,w,h,mCacheMatrix,rect);

                    //mCacheMatrix = YUVUtils.rotateYUV_420_90(mCacheMatrix,width,height,null);

                    long t = System.currentTimeMillis();
                    mCacheMatrix = YUVUtils.fastRotateYUV_420_270(mCacheMatrix,width,height,null);
                    //YUVUtils.flipYUV_420By_180(mCacheMatrix,width,height);
                    Log.e("rotateYUV_420_270","lose:" + (System.currentTimeMillis() - t));

                    if (mCacheBitmap == null){
                        mCacheBitmap = Bitmap.createBitmap(height,width,Bitmap.Config.ARGB_8888);
                    }

                    mCachePixel = YUVUtils.fastYuv420ToARGB(mCacheMatrix,height,width,mCachePixel);
                    mCacheBitmap.setPixels(mCachePixel,0,height,0,0,height,width);


  /*                  YuvImage yuvImage = new YuvImage(mCacheMatrix,format,height,width,null);
                    ByteArrayOutputStream out =
                            new ByteArrayOutputStream();
                    yuvImage.compressToJpeg(new Rect(0, 0, height, width), 30, out);

                    byte[] compressed = out.toByteArray();

                    Bitmap newBmp = BitmapFactory.decodeByteArray(compressed, 0, compressed.length);*/

                    small_preview.setImageBitmap(mCacheBitmap);
                });
            }
        }
    };

     private void initSensor(){
         mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
         mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
     }

     private void initThumb(){
         Observable.create((ObservableOnSubscribe<CameraManager.PictureInfo>) emitter -> {
             File dir = new File(preview.getPicDir());
             File[] pics = dir.listFiles();
             if (pics != null && pics.length != 0){
                 Arrays.sort(pics, (o1, o2) -> Long.compare(o1.lastModified(), o2.lastModified()));
                 File pic = pics[pics.length - 1];

                 BitmapFactory.Options options = new BitmapFactory.Options();
                 options.inSampleSize = 3;
                 Bitmap bitmap = BitmapFactory.decodeFile(pic.getAbsolutePath(),options);
                 Uri uri = Uri.fromFile(pic);

                 emitter.onNext(new CameraManager.PictureInfo(bitmap,uri));
             }
         }).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe(info -> {
             thumbnail_view.setTag(info.getUri());
             thumbnail_view.setImageBitmap(info.getBitmap());
         }, throwable -> Toast.makeText(this,throwable.getMessage(),Toast.LENGTH_LONG).show());
     }

    @OnClick(R.id.pic_btn)
    void takePicture(){
        preview.takePicture(new CameraPreview.Subscribe(preview) {
            @Override
            public void pictureTaken(CameraManager.PictureInfo info) {
                thumbnail_view.setTag(info.getUri());
                thumbnail_view.setImageBitmap(info.getBitmap());
            }
        });
    }

    @OnClick(R.id.pic_view)
    void clickPic(){
        Toast.makeText(this,"hello",Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.swt_btn)
    void switchCamera(){
        preview.switchCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this,mSensor,SensorManager.SENSOR_DELAY_NORMAL);
        if (!OpenCVLoader.initDebug()) {
            Logger.d("Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Logger.d("OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                preview.switchCamera();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void checkCameraPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if ((ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA))) {
                Toast.makeText(this,"APP不能使用相机,请设置允许APP访问权限",Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA}, CAMERA_PERMISSION);
            }
        }
    }

    public static void start(Context context){
        final Intent intent = new Intent();
        intent.setClass(context,CaptureActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
            SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Y, mRotationMatrix);
            SensorManager.getOrientation(mRotationMatrix, orientationVals);
            orientationVals[0] = (float) Math.toDegrees(orientationVals[0]);
            orientationVals[1] = (float) Math.toDegrees(orientationVals[1]);
            orientationVals[2] = (float) Math.toDegrees(orientationVals[2]);
            thumbnail_view.setMatrix(orientationVals[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}