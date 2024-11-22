package com.wyc.table_recognition

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import com.wyc.table_recognition.bean.RecognizingSetting

internal class SettingActivity : BaseActivity() {
    var setting  = RecognizingSetting.load()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setMiddleText("识别设置")
        setRightText("保存")

        setRightListener{
            save()
            Toast.makeText(this,"保存成功",Toast.LENGTH_LONG).show()
        }

        initSetting()
    }

    private fun save(){
        setting.save()
    }

    private fun initSetting(){
        findViewById<CheckBox>(R.id.sel_barcode).let {
            it.isChecked = setting.barcodeEnable
            it.setOnCheckedChangeListener { _, isChecked ->
                setting.barcodeEnable = isChecked
            }
        }
        findViewById<CheckBox>(R.id.sel_name).let {
            it.isChecked = setting.nameEnable
            it.setOnCheckedChangeListener { _, isChecked ->
                setting.nameEnable = isChecked
            }
        }
        findViewById<CheckBox>(R.id.sel_num).let {
            it.isChecked = setting.numEnable
            it.setOnCheckedChangeListener { _, isChecked ->
                setting.numEnable = isChecked
            }
        }
        findViewById<CheckBox>(R.id.sel_price).let {
            it.isChecked = setting.priceEnable
            it.setOnCheckedChangeListener { _, isChecked ->
                setting.priceEnable = isChecked
            }
        }

        findViewById<CheckBox>(R.id.sel_spec).let {
            it.isChecked = setting.specEnable
            it.setOnCheckedChangeListener { _, isChecked ->
                setting.specEnable = isChecked
            }
        }
        findViewById<CheckBox>(R.id.sel_unit).let {
            it.isChecked = setting.unitEnable
            it.setOnCheckedChangeListener { _, isChecked ->
                setting.unitEnable = isChecked
            }
        }
        findViewById<CheckBox>(R.id.sel_buyingprice).let {
            it.isChecked = setting.buyingEnable
            it.setOnCheckedChangeListener { _, isChecked ->
                setting.buyingEnable = isChecked
            }
        }
        findViewById<CheckBox>(R.id.sel_sellprice).let {
            it.isChecked = setting.sellPriceEnable
            it.setOnCheckedChangeListener { _, isChecked ->
                setting.sellPriceEnable = isChecked
            }
        }

        findViewById<EditText>(R.id.barcode).let {
            it.setText(setting.barcodeFiled)
            it.addTextChangedListener(object :TextWatcher{
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }

                override fun afterTextChanged(s: Editable?) {
                    setting.barcodeFiled = s.toString()
                }

            })
        }
        findViewById<EditText>(R.id.name).let {
            it.setText(setting.nameFiled)
            it.addTextChangedListener(object :TextWatcher{
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }

                override fun afterTextChanged(s: Editable?) {
                    setting.nameFiled = s.toString()
                }
            })
        }
        findViewById<EditText>(R.id.num).let {
            it.setText(setting.numFiled)
            it.addTextChangedListener(object :TextWatcher{
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }

                override fun afterTextChanged(s: Editable?) {
                    setting.numFiled = s.toString()
                }
            })
        }
        findViewById<EditText>(R.id.price).let {
            it.setText(setting.priceFiled)
            it.addTextChangedListener(object :TextWatcher{
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }

                override fun afterTextChanged(s: Editable?) {
                    setting.priceFiled = s.toString()
                }
            })
        }

        findViewById<EditText>(R.id.spec).let {
            it.setText(setting.specFiled)
            it.addTextChangedListener(object :TextWatcher{
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }

                override fun afterTextChanged(s: Editable?) {
                    setting.specFiled = s.toString()
                }
            })
        }

        findViewById<EditText>(R.id.unit).let {
            it.setText(setting.unitFiled)
            it.addTextChangedListener(object :TextWatcher{
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }

                override fun afterTextChanged(s: Editable?) {
                    setting.unitFiled = s.toString()
                }
            })
        }

        findViewById<EditText>(R.id.buyingprice).let {
            it.setText(setting.buyingPriceFiled)
            it.addTextChangedListener(object :TextWatcher{
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }

                override fun afterTextChanged(s: Editable?) {
                    setting.buyingPriceFiled = s.toString()
                }
            })
        }

        findViewById<EditText>(R.id.sellprice).let {
            it.setText(setting.sellPriceFiled)
            it.addTextChangedListener(object :TextWatcher{
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }

                override fun afterTextChanged(s: Editable?) {
                    setting.sellPriceFiled = s.toString()
                }
            })
        }

        findViewById<Button>(R.id._default).setOnClickListener {
            setting = RecognizingSetting()
            setting.save()
            initSetting()
        }
    }

    override fun getContentLayoutId(): Int {
        return R.layout.activity_setting
    }

    companion object{
        @JvmStatic
        fun start(context: Context){
            context.startActivity(Intent(context,SettingActivity::class.java))
        }
    }
}