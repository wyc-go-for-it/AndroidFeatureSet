package com.wyc.label

import android.app.Activity
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.gprinter.bean.PrinterDevices
import com.gprinter.utils.CallbackListener
import com.wyc.label.Utils.Companion.showToast
import com.wyc.label.printer.*
import kotlinx.coroutines.*
import tspl.HPRTPrinterHelper
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread

class LabelDesignActivity : BaseActivity(), View.OnClickListener{
    private var mLabelView: LabelView? = null
    private var mCurBtn:TopDrawableTextView? = null
    private var newFlag = false

    private val REQ_CROP = 108
    private val CHOOSE_PHOTO = 110
    private val REQUEST_CAPTURE_IMG = 100
    private var mImageUri: Uri? = null

    private var mOpenDocumentLauncher: ActivityResultLauncher<Array<String>>? = null
    private var mSaveDocumentLauncher: ActivityResultLauncher<String>? = null

    private val mCoroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setMiddleText(getString(R.string.com_wyc_label_label_setting))
        setBottomColor()

        initImExport()

        initLabelView()
        initAddLabel()

        printerError()

        connPrinter()
    }
    private fun setBottomColor(){
        findViewById<View>(R.id.bottom).setBackgroundColor(LabelApp.themeColor())
    }

    override fun getContentLayoutId(): Int {
        return R.layout.com_wyc_label_activity_format
    }

    private fun initImExport(){
        mOpenDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null){
                mCoroutineScope.launch {
                    try {
                        contentResolver.openInputStream(uri)?.use { stream ->
                            val t = LabelTemplate.read(stream)
                            withContext(Dispatchers.Main) {
                                importLabel(t)
                            }
                        }
                    }catch (e:IOException){
                        showToast(R.string.com_wyc_label_import_error)
                    }
                }
            }
        }
        mSaveDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()){
            it?.apply {
                mCoroutineScope.launch() {
                    try {
                        contentResolver.openOutputStream(it)?.use {stream->
                            mLabelView?.getLabelTemplate()
                                ?.let { it1 ->
                                    LabelTemplate.write(stream, it1)
                                }
                        }
                    }catch (e:IOException){
                        showToast(R.string.com_wyc_label_export_error)
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
        LabelPrintUtils.openPrinter(callback)
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
            addLabelFormat.show()
        }
    }

    private fun initLabelView(){
        mLabelView = findViewById(R.id.labelView)
        val intent = intent
        var labelTemplateId:Int = -1
        if (intent != null){
            labelTemplateId = intent.getIntExtra(BrowseLabelActivity.LABEL_KEY,-1)
        }
        mCoroutineScope.launch {
            LabelPrintSetting.getSetting().let { setting->
                if (labelTemplateId == -1){
                    labelTemplateId = setting.labelTemplateId
                }
                mLabelView?.setRotate(setting.rotate.value)
            }
            val template = LabelTemplate.getLabelById(labelTemplateId)
            withContext(Dispatchers.Main){
                updateLabel(template)
            }
        }
    }
    private fun updateLabel(label:LabelTemplate){
        mLabelView?.updateLabelTemplate(label)
        initLabelSize()
        initLabelName()
    }
    private fun importLabel(label:LabelTemplate){
        mLabelView?.getLabelTemplate()?.apply {
            label.templateId = templateId
            label.templateName = templateName
            updateLabel(label)
        }
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

    override fun onDestroy() {
        mCoroutineScope.cancel()
        LabelPrintUtils.closePrinter()
        super.onDestroy()
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
                        showToast(e.message)
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
        val storageDir: String = getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath ?:"wyc"
        return if (Build.VERSION.SDK_INT >= 30) {

            val paramFile = File(storageDir + imageFileName)
            val localContentValues = ContentValues()
            localContentValues.put(MediaStore.Images.ImageColumns.TITLE, paramFile.name)
            localContentValues.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, paramFile.name)
            localContentValues.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/*")
            localContentValues.put(MediaStore.Images.ImageColumns.DATE_MODIFIED,
                System.currentTimeMillis())
            localContentValues.put(MediaStore.Images.ImageColumns.DATE_ADDED,
                System.currentTimeMillis())
            localContentValues.put(MediaStore.Images.ImageColumns.ORIENTATION, 0)
            localContentValues.put(MediaStore.Images.ImageColumns.DATE_TAKEN,
                System.currentTimeMillis())
            localContentValues.put(MediaStore.Images.ImageColumns.DATA, paramFile.absolutePath)
            localContentValues.put(MediaStore.Images.ImageColumns.SIZE, paramFile.length())

            LabelApp.getInstance().contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                localContentValues
            )

        } else Uri.parse("file://" + File.separator + storageDir + File.separator + imageFileName)
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
        fun start(context: Activity,templateId:Int = -1){
            val intent = Intent(context, LabelDesignActivity::class.java)
            if (templateId != -1)intent.putExtra("label",templateId)
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
                    val goods = DataItem.testGoods()
                    this@LabelDesignActivity.findViewById<ImageView>(R.id.imageView3).setImageBitmap(mLabelView?.printSingleGoodsBitmap("",goods))
                }
                R.id.document->{
                    showImExportDialog()
                }
                R.id.printLabel->{
                    if (mLabelView != null){
                        if((v as TopDrawableTextView).hasNormal()){
                            CoroutineScope(Dispatchers.IO).launch {
                                var n = LabelPrintSetting.getSetting().printNum
                                val goods = DataItem.testGoods()

                                while (n-- > 0){
                                    LabelPrintUtils.print(goods)
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
    private val callback = object : PrinterStateCallback{
        override fun onConnecting() {
            showToast(R.string.com_wyc_label_printer_connecting)
        }

        override fun onSuccess(printer:IPrinter) {
            showToast(R.string.com_wyc_label_conn_success)
            printerNormal()
        }

        override fun onReceive() {

        }

        override fun onFailure() {
            showToast(R.string.com_wyc_label_conn_fail)
            printerError()
        }

        override fun onDisconnect() {
            showToast(R.string.com_wyc_label_printer_disconnect)
            printerError()
        }
    }

}