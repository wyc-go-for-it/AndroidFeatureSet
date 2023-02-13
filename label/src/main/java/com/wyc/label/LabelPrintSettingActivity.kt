package com.wyc.label

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.wyc.label.databinding.ComWycLabelActivityLabelPrintSettingBinding
import com.wyc.label.room.BluetoothUtils
import java.util.logging.Level

class LabelPrintSettingActivity : AppCompatActivity(),View.OnClickListener {
    private var mLabelTemplateSelector: ActivityResultLauncher<Intent>? = null
    private var mPermission: ActivityResultLauncher<Array<String>>? = null
    private var mSelectDialog:SelectDialog? = null

    private var root: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ComWycLabelActivityLabelPrintSettingBinding>(this,R.layout.com_wyc_label_activity_label_print_setting)
        setTitleColor()

        root = findViewById(R.id.root)

        initSearchDialog()
        initParam()
        initTitle()
        initView()
        registerLabelCallback()
        registerPermissionCallback()
    }
    private fun setTitleColor(){
        window.statusBarColor = LabelApp.themeColor()
        findViewById<View>(R.id.title).setBackgroundColor(LabelApp.themeColor())
    }
    private fun initSearchDialog(){
        mSelectDialog = SelectDialog(this,true)
        mSelectDialog!!.setSelectListener(object : SelectDialog.OnSelect{
            override fun select(content: SelectDialog.Item) {
                DataBindingUtil.bind<ComWycLabelActivityLabelPrintSettingBinding>(root!!)?.run {
                    setting?.printer = LabelPrintSetting.combinationPrinter(content.id,content.name)
                    mSelectDialog!!.dismiss()
                    invalidateAll()
                }
            }
        })
        mSelectDialog!!.setOnDismissListener {
            BluetoothUtils.stopBlueToothDiscovery()
            mSelectDialog!!.clearContent()
        }
    }

    private fun initTitle(){
        findViewById<TextView>(R.id.middle_title_tv).setText(R.string.com_wyc_label_rotate_label_print_setting)
        findViewById<TextView>(R.id.left_title_tv).setOnClickListener {
            finish()
        }
        val save = findViewById<TextView>(R.id.right_title_tv)
        save.setText(R.string.com_wyc_label_save)
        save.setOnClickListener {
            DataBindingUtil.bind<ComWycLabelActivityLabelPrintSettingBinding>(root!!)?.setting?.saveSetting()
        }
    }

    private fun initView(){
        findViewById<TextView>(R.id.way_tv).setOnClickListener(this)
        findViewById<TextView>(R.id.printer_tv).setOnClickListener(this)
        findViewById<TextView>(R.id.rotate_tv).setOnClickListener(this)
        findViewById<TextView>(R.id.plus).setOnClickListener(this)
        findViewById<TextView>(R.id.minus).setOnClickListener(this)

        findViewById<TextView>(R.id.plusX).setOnClickListener(this)
        findViewById<TextView>(R.id.minusX).setOnClickListener(this)

        findViewById<TextView>(R.id.plusY).setOnClickListener(this)
        findViewById<TextView>(R.id.minusY).setOnClickListener(this)

        findViewById<TextView>(R.id.d_minus).setOnClickListener(this)
        findViewById<TextView>(R.id.d_plus).setOnClickListener(this)


        findViewById<TextView>(R.id.print_template_tv).setOnClickListener(this)
        findViewById<TextView>(R.id.cur_template_tv).setOnClickListener(this)
    }


    override fun onClick(v: View) {
        when(v.id){
            R.id.way_tv->{
                val selectDialog = SelectDialog(this)
                LabelPrintSetting.Way.values().forEach {
                    val item = SelectDialog.Item(it.name,it.description)
                    selectDialog.addContent(item)
                }
                selectDialog.setSelectListener(object : SelectDialog.OnSelect{
                    override fun select(content: SelectDialog.Item) {
                        DataBindingUtil.bind<ComWycLabelActivityLabelPrintSettingBinding>(root!!)?.apply {
                            setting?.let {
                                if (it.way != LabelPrintSetting.Way.valueOf(content.id)){
                                    it.way = LabelPrintSetting.Way.valueOf(content.id)
                                    it.type = LabelPrintSetting.Type.NULL
                                    it.printer = ""
                                    invalidateAll()
                                }
                            }
                            selectDialog.dismiss()
                        }

                    }
                })
                selectDialog.show()
            }
            R.id.type_tv->{
                val selectDialog = SelectDialog(this)
                DataBindingUtil.bind<ComWycLabelActivityLabelPrintSettingBinding>(root!!)?.setting?.getValidPrinterType()?.forEach {
                    val item = SelectDialog.Item(it.name,it.description)
                    selectDialog.addContent(item)
                }
                selectDialog.setSelectListener(object : SelectDialog.OnSelect{
                    override fun select(content: SelectDialog.Item) {
                        DataBindingUtil.bind<ComWycLabelActivityLabelPrintSettingBinding>(root!!)?.apply {
                            setting?.type = LabelPrintSetting.Type.valueOf(content.id)
                            invalidateAll()
                            selectDialog.dismiss()
                        }

                    }
                })
                selectDialog.show()
            }
            R.id.printer_tv->{
                val setting = DataBindingUtil.bind<ComWycLabelActivityLabelPrintSettingBinding>(root!!)?.setting
                when(setting?.way){
                    LabelPrintSetting.Way.BLUETOOTH_PRINT->{
                        var permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                        if (Build.VERSION.SDK_INT > 30){
                            permissions = arrayOf("android.permission.BLUETOOTH_SCAN","android.permission.BLUETOOTH_ADVERTISE","android.permission.BLUETOOTH_CONNECT")
                        }
                        mPermission?.launch(permissions)
                    }
                    LabelPrintSetting.Way.WIFI_PRINT ->{
                        val ipInputDialog = IPInputDialog(this)
                        ipInputDialog.setListener(object :IPInputDialog.OnContent{
                            override fun content(ip: String, port: String) {
                                setting.printer = LabelPrintSetting.combinationPrinter(port,ip)
                                DataBindingUtil.bind<ComWycLabelActivityLabelPrintSettingBinding>(root!!)?.invalidateAll()
                            }
                        })
                        ipInputDialog.show()
                    }
                    else -> {
                        Utils.showToast(R.string.com_wyc_label_not_support_way)
                    }
                }
            }
            R.id.rotate_tv->{
                val selectDialog = SelectDialog(this)
                LabelPrintSetting.Rotate.values().forEach {
                    val item = SelectDialog.Item(it.name,it.description)
                    selectDialog.addContent(item)
                }
                selectDialog.setSelectListener(object : SelectDialog.OnSelect{
                    override fun select(content: SelectDialog.Item) {
                        DataBindingUtil.bind<ComWycLabelActivityLabelPrintSettingBinding>(root!!)?.apply {
                            setting?.rotate = LabelPrintSetting.Rotate.valueOf(content.id)
                            invalidateAll()
                            selectDialog.dismiss()
                        }

                    }
                })
                selectDialog.show()
            }
            R.id.plus,R.id.minus->{
                var i = 1
                if (v.id == R.id.plus){
                    i = -1
                }
                val bind = DataBindingUtil.bind<ComWycLabelActivityLabelPrintSettingBinding>(root!!)
                val setting = bind?.setting
                val num = setting?.printNum?:0
                setting?.printNum = num - i
                bind?.invalidateAll()
            }
            R.id.plusX, R.id.minusX->{
                var i = -1
                if (v.id == R.id.minusX){
                    i = 1
                }
                val bind = DataBindingUtil.bind<ComWycLabelActivityLabelPrintSettingBinding>(root!!)
                val setting = bind?.setting
                val num = setting?.offsetX?:0
                setting?.offsetX = num - i
                bind?.invalidateAll()
            }
            R.id.plusY, R.id.minusY->{
                var i = -1
                if (v.id == R.id.minusY){
                    i = 1
                }
                val bind = DataBindingUtil.bind<ComWycLabelActivityLabelPrintSettingBinding>(root!!)
                val setting = bind?.setting
                val num = setting?.offsetY?:0
                setting?.offsetY = num - i
                bind?.invalidateAll()
            }
            R.id.d_minus, R.id.d_plus->{
                var i = -1
                if (v.id == R.id.d_minus){
                    i = 1
                }
                val bind = DataBindingUtil.bind<ComWycLabelActivityLabelPrintSettingBinding>(root!!)
                val setting = bind?.setting
                val num = setting?.density?:1
                setting?.density  = num - i
                bind?.invalidateAll()
            }
            R.id.print_template_tv->{
                LabelDesignActivity.start(this)
            }
            R.id.cur_template_tv->{
                mLabelTemplateSelector?.launch(Intent(this,BrowseLabelActivity::class.java))
            }
        }
    }

    private fun registerPermissionCallback(){
        mPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            if(it.all {m->m.value == true }){
                BluetoothUtils.startBlueToothDiscovery(this)
            }
        }
    }

    private fun registerLabelCallback(){
        mLabelTemplateSelector = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK){
                it.data?.getIntExtra(BrowseLabelActivity.LABEL_KEY,0)?.apply {
                    val label = LabelTemplate.getLabelById(this)
                    findViewById<TextView>(R.id.cur_template_tv)?.text = label.templateName
                    val setting: LabelPrintSetting? = DataBindingUtil.bind<ComWycLabelActivityLabelPrintSettingBinding>(root!!)?.setting
                    setting?.labelTemplateId = label.templateId
                    setting?.labelTemplateName = label.templateName
                }
            }
        }
    }

    private fun initParam(){
        val setting = LabelPrintSetting.getSetting()
        DataBindingUtil.bind<ComWycLabelActivityLabelPrintSettingBinding>(root!!)?.setting = setting

        if (setting.way == LabelPrintSetting.Way.BLUETOOTH_PRINT)
            BluetoothUtils.bondBlueTooth(setting.getPrinterAddress())
    }


    override fun onResume() {
        super.onResume()
        BluetoothUtils.attachReceiver(this,receiver)
    }

    override fun onPause() {
        super.onPause()
        BluetoothUtils.stopBlueToothDiscovery()
    }

    override fun onDestroy() {
        super.onDestroy()
        BluetoothUtils.detachReceiver(this,receiver)
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action){
                BluetoothDevice.ACTION_FOUND -> {
                    val bluetoothDevice_found: BluetoothDevice? = intent.getParcelableExtra(
                            BluetoothDevice.EXTRA_DEVICE)
                    bluetoothDevice_found?.let {
                        val device_style = it.bluetoothClass.majorDeviceClass
                        if (device_style == BluetoothClass.Device.Major.IMAGING || device_style == BluetoothClass.Device.Major.MISC) {
                            mSelectDialog?.addContent(it.address,it.name)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    mSelectDialog?.showMsg(getString(R.string.com_wyc_label_searching_bluetooth))
                    mSelectDialog?.show()
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {

                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    mSelectDialog?.stopSearch()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == Activity.RESULT_OK && requestCode == BluetoothUtils.REQUEST_BLUETOOTH_ENABLE) {
            BluetoothUtils.startBlueToothDiscovery(this)
        }
    }

    override fun onBackPressed() {
        if (DataBindingUtil.bind<ComWycLabelActivityLabelPrintSettingBinding>(root!!)?.setting?.hasChange() == true){
            DataBindingUtil.bind<ComWycLabelActivityLabelPrintSettingBinding>(root!!)?.setting?.saveSetting()
        }
        return super.onBackPressed()
    }
    companion object{
        @JvmStatic
        fun start(context: Activity ){
            context.startActivity(Intent(context, LabelPrintSettingActivity::class.java))
        }
    }

}