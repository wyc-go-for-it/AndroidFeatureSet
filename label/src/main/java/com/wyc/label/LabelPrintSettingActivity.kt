package com.wyc.label

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.wyc.label.databinding.ActivityLabelPrintSettingBinding
import com.wyc.label.room.BluetoothUtils
import java.util.*

class LabelPrintSettingActivity : AppCompatActivity() {
    private var mLabelTemplateSelector: ActivityResultLauncher<Intent>? = null
    private var mBluetoothDevices:MutableList<TreeListItem>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityLabelPrintSettingBinding>(this, R.layout.activity_label_print_setting)

        initParam()
        registerGoodsCallback()
    }
    private fun initView(){
        findViewById<TextView>(R.id.template_tv).setOnClickListener {
            mLabelTemplateSelector?.launch(Intent(this,BrowseLabelActivity::class.java))
        }

        findViewById<TextView>(R.id.rotate_tv).setOnClickListener{view->
            val treeListDialog = TreeListDialogForObj(mContext, mContext.getString(R.string.paper_spec))
            treeListDialog.setData(convertRotate(), null, true)
            if (treeListDialog.exec() == 1) {
                val obj = treeListDialog.singleContent
                (view as TextView).text = obj.item_name

                val setting: LabelPrintSetting? = DataBindingUtil.bind<ActivityLabelPrintSettingBinding>(rootView)?.setting
                setting?.rotate = LabelPrintSetting.Rotate.valueOf(obj.item_id)
            }
        }
        private fun convertRotate(): List<TreeListItem> {
            val data: MutableList<TreeListItem> = ArrayList()
            val  values: Array<LabelPrintSetting.Rotate> = LabelPrintSetting.Rotate.values()
            values.iterator().forEach {
                val item = TreeListItem()
                item.item_id = it.name
                item.item_name = it.description
                data.add(item)
            }
            return data
        }

        findViewById<TextView>(R.id.minus).setOnClickListener{view->
            var i = 1
            if (view.id == R.id.plus){
                i = -1
            }
            val bind = DataBindingUtil.bind<ActivityLabelPrintSettingBinding>(window.decorView)
            val setting = bind?.setting
            val num = setting?.printNum?:0
            setting?.printNum = num - i
            bind?.invalidateAll()
        }

        @OnClick(R.id.plusX, R.id.minusX)
        fun offsetX(view: View){
            var i = -1
            if (view.id == R.id.minusX){
                i = 1
            }
            val bind = DataBindingUtil.bind<LabelPrintSettingBinding>(rootView)
            val setting = bind?.setting
            val num = setting?.offsetX?:0
            setting?.offsetX = num - i
            bind?.invalidateAll()
        }

        @OnClick(R.id.plusY, R.id.minusY)
        fun offsetY(view: View){
            var i = -1
            if (view.id == R.id.minusY){
                i = 1
            }
            val bind = DataBindingUtil.bind<LabelPrintSettingBinding>(rootView)
            val setting = bind?.setting
            val num = setting?.offsetY?:0
            setting?.offsetY = num - i
            bind?.invalidateAll()
        }

        findViewById<TextView>(R.id.printer_tv).setOnClickListener{
            XXPermissions.with(this)
                    .permission(Manifest.permission.ACCESS_FINE_LOCATION)
                    .request(object : OnPermissionCallback {
                        override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                            BluetoothUtils.startBlueToothDiscovery(this@LabelPrintFragment)
                        }

                        override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                            if (never) {

                            }
                        }
                    })
        }

        @OnClick(R.id.print_template_tv)
        fun templateDesign(){
            LabelDesignActivity.start(mContext)
        }
    }

    private fun registerGoodsCallback(){
        mLabelTemplateSelector = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK){
                it.data?.getParcelableExtra<LabelTemplate>("label")?.apply {
                    findViewById<TextView>(R.id.template_tv)?.text = templateName
                    val setting: LabelPrintSetting? = DataBindingUtil.bind<ActivityLabelPrintSettingBinding>(window.decorView)?.setting
                    try {
                        setting?.labelTemplateId = templateId
                    }catch (e: NumberFormatException){
                        e.printStackTrace()
                    }
                    setting?.labelTemplateName = templateName
                }
            }
        }
    }

    private fun initParam(){
        val setting = LabelPrintSetting.getSetting()
        DataBindingUtil.bind<ActivityLabelPrintSettingBinding>(window.decorView)?.setting = setting
        BluetoothUtils.bondBlueTooth(setting.getPrinterAddress())
    }


    override fun onResume() {
        super.onResume()
        BluetoothUtils.attachReceiver(context,receiver)
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
                            val address = it.address
                            if (mBluetoothDevices == null)mBluetoothDevices = mutableListOf()
                            try {
                                mBluetoothDevices!!.first {
                                    it.item_id == address
                                }
                            }catch (e: NoSuchElementException){
                                mBluetoothDevices!!.add(TreeListItem(address,it.name))
                            }
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    showProgress(getString(R.string.searching_bluetooth))
                    mProgressDialog?.setOnCancelListener {
                        BluetoothUtils.stopBlueToothDiscovery()
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {

                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    dismissProgress()
                    setPrinter()
                }
            }
        }
    }

    private fun setPrinter(){
        mBluetoothDevices?.let {
            if (it.isNotEmpty()){
                DataBindingUtil.bind<ActivityLabelPrintSettingBinding>(window.decorView)?.run {
                    if (it.size == 1){
                        setting?.printer =
                                LabelPrintSetting.combinationPrinter(it[0].item_id,it[0].item_name)
                    }else{
                        val treeListDialog = TreeListDialogForObj(mContext, mContext.getString(R.string.printer))
                        treeListDialog.setData(mBluetoothDevices, null, true)
                        if (treeListDialog.exec() == 1) {
                            val obj = treeListDialog.singleContent
                            setting?.printer = LabelPrintSetting.combinationPrinter(obj.item_id,obj.item_name)
                        }
                    }
                    invalidateAll()
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
        if (DataBindingUtil.bind<ActivityLabelPrintSettingBinding>(window.decorView)?.setting?.hasChange() == true){
            DataBindingUtil.bind<ActivityLabelPrintSettingBinding>(window.decorView)?.setting?.saveSetting()
        }
        return super.onBackPressed()
    }
}