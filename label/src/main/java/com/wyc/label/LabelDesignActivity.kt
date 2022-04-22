package com.wyc.label

import android.app.Activity
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.fastjson.JSON
import com.gprinter.bean.PrinterDevices
import com.gprinter.utils.CallbackListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.nio.charset.StandardCharsets

class LabelDesignActivity : AppCompatActivity(), View.OnClickListener,CallbackListener,CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private var mLabelView: LabelView? = null
    private var mCurBtn:TopDrawableTextView? = null
    private var newFlag = false

    private val REQ_CROP = 108
    private val CHOOSE_PHOTO = 110
    private val REQUEST_CAPTURE_IMG = 100
    private var mImageUri: Uri? = null

    private var mOpenDocumentLauncher: ActivityResultLauncher<Array<String>>? = null
    private var mSaveDocumentLauncher: ActivityResultLauncher<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initTitle()

        initImExport()

        initLabelView()
        initAddLabel()

        printerError()

        connPrinter()
    }

    private fun initTitle(){
        findViewById<TextView>(R.id.middle_title_tv).setText(R.string.com_wyc_label_label_setting)
        findViewById<TextView>(R.id.left_title_tv).setOnClickListener {
            finish()
        }
    }

    private fun initImExport(){
        mOpenDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null){
                launch {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        val reader = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8))
                        val stringBuilder = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            stringBuilder.append(line)
                        }
                        withContext(Dispatchers.Main) {
                            updateLabel(LabelTemplate.parse(stringBuilder.toString()))
                        }
                    }
                }
            }
        }
        mSaveDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()){
            it?.apply {
                launch {
                    contentResolver.openOutputStream(it)?.use {stream->
                        val writer = BufferedWriter(OutputStreamWriter(stream,StandardCharsets.UTF_8))
                        val json = JSON.toJSONString(mLabelView?.getLabelTemplate())
                        writer.write(json)
                        writer.flush()
                    }
                }
            }
        }
    }
    private fun showImExportDialog(){
        val pop = Dialog(this, R.style.com_wyc_label_MyDialog)
        pop.setContentView(R.layout.com_wyc_label_im_export_file)

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val d: Display = wm.defaultDisplay
        val point = Point()
        d.getSize(point)

        pop.window?.apply {
            setWindowAnimations(R.style.com_wyc_label_bottom_pop_anim)
            val wlp: WindowManager.LayoutParams = attributes
            wlp.gravity = Gravity.BOTTOM
            wlp.y = 68
            wlp.width = (point.x * 0.95).toInt()
            attributes = wlp
        }

        pop.findViewById<Button>(R.id.import_file)?.setOnClickListener {
            pop.dismiss()
            mOpenDocumentLauncher?.launch(arrayOf("text/*"))
        }
        pop.findViewById<Button>(R.id.export)?.setOnClickListener {
            pop.dismiss()
            mLabelView?.apply {
                mSaveDocumentLauncher?.launch(getLabelName() + ".txt")
            }
        }
        pop.show()
    }

    private fun connPrinter(){
        GPPrinter.openBlueTooth(LabelPrintSetting.getSetting().getPrinterAddress(),this)
    }

    private fun initAddLabel(){
        val tv = findViewById<TextView>(R.id.right_title_tv)
        tv.setText(R.string.com_wyc_label_add)

        tv.setOnClickListener {
            val addLabelFormat = AddLabelFormat(this)
            newFlag = true
            addLabelFormat.setListener(object :AddLabelFormat.OnContent{
                override fun content(labelTemplate: LabelTemplate) {
                    mLabelView?.updateLabelTemplate(labelTemplate)
                    this@LabelDesignActivity.findViewById<ImageView>(R.id.imageView3).setImageBitmap(null)
                    findViewById<EditText>(R.id.label_name)?.setText(labelTemplate.templateName)
                    (findViewById<Spinner>(R.id.label_size).adapter as? ArrayAdapter<String>)?.apply {
                        clear()
                        mLabelView?.getLabelSize()?.forEach {
                            add(it.description)
                        }
                    }
                    newFlag = false
                }
            })
        }
    }

    private fun initLabelView(){
        mLabelView = findViewById(R.id.labelView)
        val intent = intent
        if (intent != null){
            intent.getParcelableExtra<LabelTemplate>("label")?.apply {
                updateLabel(this)
            }
        }
    }
    private fun updateLabel(label:LabelTemplate){
        mLabelView?.updateLabelTemplate(label)
        initLabelSize()
        initLabelName()
    }

    private fun initLabelName(){
        findViewById<EditText>(R.id.label_name)?.apply {
               setText(mLabelView?.getLabelName())
               addTextChangedListener(object :TextWatcher{
                   override fun beforeTextChanged(
                       s: CharSequence?,
                       start: Int,
                       count: Int,
                       after: Int
                   ) {

                   }

                   override fun onTextChanged(
                       s: CharSequence?,
                       start: Int,
                       before: Int,
                       count: Int
                   ) {

                   }

                   override fun afterTextChanged(s: Editable?) {
                       if (!newFlag)
                        mLabelView?.updateLabelName(s.toString())
                   }
               })
        }
    }
    private fun initLabelSize(){
        findViewById<Spinner>(R.id.label_size)?.apply {
            val adapter = ArrayAdapter<String>(this@LabelDesignActivity, R.layout.com_wyc_label_drop_down_style)
            adapter.setDropDownViewResource(R.layout.com_wyc_label_drop_down_style)
            mLabelView?.getLabelSize()?.forEach {
                adapter.add(it.description)
            }
            setAdapter(adapter)

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    mLabelView?.getLabelSize()?.forEach {
                        if (it.description == adapter.getItem(position)){
                            if (!newFlag)
                                mLabelView?.updateLabelSize(it.getrW(),it.getrH())
                            return
                        }
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        //关闭打印机
        GPPrinter.close()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_CAPTURE_IMG -> {
                    crop()
                }
                REQ_CROP -> {
                    try {
                        mImageUri?.let {
                            contentResolver.openInputStream(it).use { inputStream ->
                                mLabelView?.setLabelBackground(BitmapFactory.decodeStream(inputStream))
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(this,e.localizedMessage,Toast.LENGTH_LONG).show()
                    }
                }
                CHOOSE_PHOTO -> {
                    mImageUri = intent?.data
                    crop()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, intent)
    }

    private fun crop() {
        val intent = Intent("com.android.camera.action.CROP")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setDataAndType(mImageUri, "image/*")

        intent.putExtra("outputX", mLabelView?.getRealWidth())
        intent.putExtra("outputY", mLabelView?.getRealHeight())

        intent.putExtra("scale", true)
        intent.putExtra("return-data", false)
        val imgCropUri = createCropImageFile()
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imgCropUri)
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString())
        intent.putExtra("noFaceDetection", false)
        mImageUri = imgCropUri
        startActivityForResult(intent, REQ_CROP)
    }
    private fun createCropImageFile(): Uri? {
        val imageFileName = "clip_wyc_." + Bitmap.CompressFormat.JPEG.toString()
        val storageDir: String = getDir("wyc", Context.MODE_PRIVATE).absolutePath
        return Uri.parse("file://" + File.separator + storageDir + imageFileName)
    }

    private fun openAlbum() {
        val openAlbumIntent = Intent(Intent.ACTION_GET_CONTENT)
        openAlbumIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        openAlbumIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivityForResult(openAlbumIntent, CHOOSE_PHOTO) //打开相册
    }

    private fun showEditBackgroundDialog(){
        val pop = Dialog(this, R.style.com_wyc_label_MyDialog)
        pop.setContentView(R.layout.com_wyc_label_background_edit)

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val d: Display = wm.defaultDisplay // 获取屏幕宽、高用
        val point = Point()
        d.getSize(point)

        pop.window?.apply {
            setWindowAnimations(R.style.com_wyc_label_bottom_pop_anim)
            val wlp: WindowManager.LayoutParams = attributes
            wlp.gravity = Gravity.BOTTOM
            wlp.y = 68
            wlp.width = (point.x * 0.95).toInt()
            attributes = wlp
        }

        pop.findViewById<Button>(R.id.add)?.setOnClickListener {
            openAlbum()
            pop.dismiss()
        }
        pop.findViewById<Button>(R.id.del)?.setOnClickListener {
            pop.dismiss()
            mLabelView?.setLabelBackground(null)
        }
        pop.show()
    }

    companion object{
        @JvmStatic
        fun start(context: Activity,labelTemplate: LabelTemplate? = null){
            val intent = Intent(context, LabelDesignActivity::class.java)
            if (labelTemplate != null)intent.putExtra("label",labelTemplate)
            context.startActivity(intent)
        }
    }
    private fun swapCurBtn(v: View){
        if (mCurBtn != null){
            mCurBtn!!.triggerAnimation(false)
        }
        mCurBtn = v as? TopDrawableTextView
        mCurBtn?.triggerAnimation(true)
    }

    override fun onClick(v: View) {
        swapCurBtn(v)

        mLabelView?.apply {
            when(v.id){
                R.id.delete->{
                    deleteItem()
                }
                R.id.shrink->{
                    shrinkItem()
                }
                R.id.zoom->{
                    zoomItem()
                }
                R.id.rotate->{
                    rotateItem()
                }
                R.id.undo->{
                    restAction()
                }
                R.id.text->{
                    addTextItem()
                }
                R.id.barcode->{
                    addBarcodeItem()
                }
                R.id.qrcode->{
                    addQRCodeItem()
                }
                R.id.line->{
                    addLineItem()
                }
                R.id.rect->{
                    addRectItem()
                }
                R.id.circle->{
                    addCircleItem()
                }
                R.id.date->{
                    addDateItem()
                }
                R.id.data->{
                    addDataItem()
                }
                R.id.image->{
                    showEditBackgroundDialog()
                }
                R.id.save->{
                    save()
                }
                R.id.preview->{
                    this@LabelDesignActivity.findViewById<ImageView>(R.id.imageView3).setImageBitmap(mLabelView?.printSingleGoodsBitmap("",DataItem.LabelGoods()))
                }
                R.id.document->{
                    showImExportDialog()
                }
                R.id.printLabel->{
                    if (hasSupportBluetooth()){
                        if (mLabelView != null){
                            val labelTemplate = mLabelView!!.getLabelTemplate()
                            if((v as TopDrawableTextView).hasNormal()){
                                launch {
                                    var n = LabelPrintSetting.getSetting().printNum
                                    while (n-- > 0){
                                        GPPrinter.sendDataToPrinter(GPPrinter.getGPTscCommand(labelTemplate,DataItem.LabelGoods()).command)
                                    }
                                }
                            }else{
                                connPrinter()
                            }
                        }
                    }
                }
            }
        }
    }
    private fun hasSupportBluetooth(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val code = bluetoothAdapter != null && bluetoothAdapter.isEnabled
        if (!code) {
            Toast.makeText(this,"未开启蓝牙功能...",Toast.LENGTH_LONG).show()
        }
        return code
    }

    private fun printerError(){
        findViewById<TopDrawableTextView>(R.id.printLabel).apply {
            warn()
        }
    }
    private fun printerNormal(){
        findViewById<TopDrawableTextView>(R.id.printLabel).apply {
            normal()
        }
    }

    override fun onConnecting() {
        Toast.makeText(this,R.string.com_wyc_label_printer_connecting,Toast.LENGTH_LONG).show()
    }

    override fun onCheckCommand() {

    }

    override fun onSuccess(p0: PrinterDevices?) {
        Toast.makeText(this,R.string.com_wyc_label_conn_success,Toast.LENGTH_LONG).show()
        printerNormal()
    }

    override fun onReceive(p0: ByteArray?) {

    }

    override fun onFailure() {
        Toast.makeText(this,R.string.com_wyc_label_conn_fail,Toast.LENGTH_LONG).show()
        printerError()
    }

    override fun onDisconnect() {
        Toast.makeText(this,R.string.com_wyc_label_printer_disconnect,Toast.LENGTH_LONG).show()
        printerError()
    }

}