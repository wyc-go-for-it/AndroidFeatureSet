package com.wyc.table_recognition

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.wyc.table_recognition.VideoCameraManager.Companion.clear
import com.wyc.table_recognition.VideoCameraManager.Companion.getInstance
import com.wyc.table_recognition.bean.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URLEncoder

class RecognitionActivity : BaseActivity(),VideoCameraManager.OnPicture {
    private val setting  = RecognizingSetting.load()
    private var isContinue = false
    private var mProgressDialog:CustomProgressDialog? = null
    private val mCamera = getInstance()

    private val mColIndex = HashMap<String,Int>()
    private val mRecognizingInfo = ArrayList<RecognizingData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setMiddleText("识别")
        setRightText("设置")
        setRightListener{
            SettingActivity.start(this)
        }

        initRecordBtn()

        getInstance().setPicCallback(this)

        findViewById<SurfaceView>(R.id.preview_surface).holder.addCallback(object :SurfaceHolder.Callback{
            override fun surfaceCreated(holder: SurfaceHolder) {
                getInstance().addSurface(holder.surface)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                getInstance().setDisplaySize(width, height)
                getInstance().openCamera()
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                clear()
            }
        })

        findViewById<CheckBox>(R.id.c_recognition).setOnCheckedChangeListener { _, isChecked -> isContinue = isChecked }
    }


    private fun initRecordBtn() {
        val mRecord = findViewById<Button>(R.id.recordBtn)
        mRecord.setOnClickListener { v: View? ->
            showProgress()
            mCamera.tackPic()
        }
    }

    override fun onBackPressed() {
        setData()
        super.onBackPressed()
    }

    private fun setData(){
        val intent = Intent()
        intent.putParcelableArrayListExtra(DATA_KEY, mRecognizingInfo)
        setResult(RESULT_OK, intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode == VideoCameraManager.REQUEST_CAMERA_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getInstance().openCamera()
                mCamera.recodeVideo()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun showProgress() {
        if (mProgressDialog == null) mProgressDialog =
            CustomProgressDialog.showProgress(this, "正在识别...")
            .refreshMessage()
        if (!mProgressDialog!!.isShowing()) mProgressDialog!!.show()
    }

    private fun dismissProgress() {
        if (mProgressDialog != null && mProgressDialog!!.isShowing) mProgressDialog!!.dismiss()
    }

    override fun onResume() {
        super.onResume()
        if(ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                VideoCameraManager.REQUEST_CAMERA_PERMISSIONS
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        getInstance().setPicCallback(null)
    }
    override fun getContentLayoutId(): Int {
       return R.layout.camera_surface_view
    }

    override fun onTaken(bmp: Bitmap?) {
        if (bmp != null){
            recognition(bmp)
        }else{
            Toast.makeText(this,"未生成识别信息",Toast.LENGTH_LONG).show()
            dismissProgress()
        }
    }

    private fun recognition(bmp:Bitmap){
        val byteArrayOutputStream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG,50,byteArrayOutputStream)
        val content = Base64.encodeToString(byteArrayOutputStream.toByteArray(),Base64.DEFAULT)
        val url = " https://aip.baidubce.com/rest/2.0/ocr/v1/table?access_token=${AccessToken.load().access_token}"
        val img  = "image=" + URLEncoder.encode(content, "UTF-8");
        HttpUtils.sendAsyncPost(url,img).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread{
                    Toast.makeText(this@RecognitionActivity, "识别错误：${e.message}",Toast.LENGTH_LONG).show()
                }
                dismissProgress()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful){
                    val c = response.body!!.string()
                    val result = JsonUtils.string2Object<TableResult>(c)
                    if (!result.error_msg.isNullOrEmpty()){
                        runOnUiThread{
                            Toast.makeText(this@RecognitionActivity, "识别错误：" + result.error_msg,Toast.LENGTH_LONG).show()
                        }
                    }else{
                        parseTable(result)
                    }
                }else{
                    runOnUiThread{
                        Toast.makeText(this@RecognitionActivity, "识别错误：" + response.message,Toast.LENGTH_LONG).show()
                    }
                }
                dismissProgress()
            }
        })
    }

    private fun parseTable(data: TableResult){
        if (data.tableNum > 0){
            val result = data.tablesResult[0]
            val bodyList = result.body
            val cols = mutableListOf<MutableList<String>>()
            var col = mutableListOf<String>()
            var rowStart = 1
            for (i in bodyList.indices){
                val body = bodyList[i]
                if (rowStart != body.rowStart){
                    rowStart = body.rowStart
                    cols.add(col)
                    col = mutableListOf()
                    col.add(body.words)
                }else{
                    col.add(body.words)
                }
            }
            cols.add(col)
            check(cols)
        }else{
            runOnUiThread {
                Toast.makeText(this@RecognitionActivity, "未识别到内容",Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun check(cols:MutableList<MutableList<String>>){
        if (cols.isNotEmpty()){
            val max = cols.maxOf { it.size }
            val validCols = cols.filter { it.size >= max }

            val vailIndex = validCols.indexOfFirst { it.any { it.isNotEmpty() } }
            if (vailIndex == -1){
                Toast.makeText(this,"未找到有效标题",Toast.LENGTH_LONG).show()
                return
            }

            val col1 = validCols[vailIndex]

            if (!isContinue || mColIndex.isEmpty()){
                mColIndex.clear()
                val barcodeEnable = setting.barcodeEnable
                val nameEnable = setting.nameEnable
                val numEnable = setting.numEnable
                val priceEnable = setting.priceEnable
                if (barcodeEnable){
                    mColIndex["barcode"] = col1.indexOfFirst { setting.barcodeFiled == it }
                }
                if (nameEnable){
                    mColIndex["name"] = col1.indexOfFirst { setting.nameFiled == it }
                }
                if (numEnable){
                    mColIndex["num"] = col1.indexOfFirst { setting.numFiled == it }
                }
                if (priceEnable){
                    mColIndex["price"] = col1.indexOfFirst { setting.priceFiled == it }
                }
            }

            val maxCol = mColIndex.values.maxOf { it }
            for (i in vailIndex + 1 until validCols.size){
                val c = validCols[i]
                if (c.size > maxCol){
                    val data = RecognizingData()
                    var index = mColIndex["barcode"]
                    data.barcodeEnable = index !=null && index > -1
                    if (index !=null && index > -1){
                        data.barcode = c[index]
                    }
                    index = mColIndex["name"]
                    data.nameEnable = index !=null && index > -1
                    if (data.nameEnable){
                        data.name = c[index!!]
                    }
                    index = mColIndex["num"]
                    data.numEnable = index !=null && index > -1
                    if (data.numEnable){
                        data.num = c[index!!].toDoubleOrNull()?:0.0
                    }
                    index = mColIndex["price"]
                    data.priceEnable = index !=null && index > -1
                    if (data.priceEnable){
                        data.price = c[index!!].toDoubleOrNull()?:0.0
                    }
                    mRecognizingInfo.add(data)
                }
            }

            if (mRecognizingInfo.isEmpty()){
                runOnUiThread{
                    Toast.makeText(this,"未识别到有效字段",Toast.LENGTH_LONG).show()
                }
            }else{
                if (!isContinue){
                    setData()
                    finish()
                }
            }
            Log.d("mRecognizingInfo", mRecognizingInfo.toTypedArray().contentToString())
        }
    }

    companion object{
        const val REQUEST_CODE = 88
        const val DATA_KEY = "recognizingInfo"
        @JvmStatic
        fun start(context: Context){
            context.startActivity(Intent(context,RecognitionActivity::class.java))
        }
        @JvmStatic
        fun startForResult(context: Activity){
            context.startActivityForResult(Intent(context,RecognitionActivity::class.java),REQUEST_CODE)
        }
        @JvmStatic
        fun startForResult(fragment:Fragment){
            fragment.startActivityForResult(Intent(fragment.context,RecognitionActivity::class.java),REQUEST_CODE)
        }
    }
}