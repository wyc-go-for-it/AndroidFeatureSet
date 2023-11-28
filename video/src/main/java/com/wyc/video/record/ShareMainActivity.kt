package com.wyc.video.record

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import com.wyc.video.R
import com.wyc.video.activity.BaseActivity


class ShareMainActivity : BaseActivity(),SurfaceHolder.Callback {
    private val REQUEST_CODE = 898
    private var mSurface:Surface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSurface()
        initBtn()
    }

    override fun onDestroy() {
        super.onDestroy()
        //stopService(Intent(this,ShareService::class.java))
    }

    override fun getContentLayoutId(): Int {
        return R.layout.activity_record_main
    }

    private fun initSurface(){
        findViewById<SurfaceView>(R.id.show).holder.addCallback(this)
    }

    private fun initBtn(){
        findViewById<Button>(R.id.start).setOnClickListener{
            startScreenRecording()
        }
    }

    private fun startScreenRecording() {
        val permissionIntent: Intent = (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).createScreenCaptureIntent()
        startActivityForResult(permissionIntent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK){
            val intent = Intent(this,ShareService::class.java)
            intent.putExtra("resultCode",resultCode)
            intent.putExtra("data",data)
            intent.putExtra("surface",mSurface)

            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            intent.putExtra("width",metrics.widthPixels)
            intent.putExtra("height",metrics.heightPixels)
            intent.putExtra("density",metrics.densityDpi)

            startForegroundService(intent)
        }
    }

    companion object{
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, ShareMainActivity::class.java))
        }
    }


    override fun surfaceCreated(holder: SurfaceHolder) {
        mSurface = holder.surface
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }
}