package com.wyc.label

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import com.google.zxing.BarcodeFormat
import java.io.ObjectStreamException


/**
 *
 * @ProjectName:    AndroidClient
 * @Package:        com.wyc.cloudapp.design
 * @ClassName:      QRCodeItem
 * @Description:    二维码
 * @Author:         wyc
 * @CreateDate:     2022/4/11 18:55
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/4/11 18:55
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

internal class QRCodeItem: CodeItemBase()  {
    init {
        width = 231
        height = width
        barcodeFormat = BarcodeFormat.QR_CODE

        generateBitmap()

        BarcodeFormat.values().forEach {
            if (it.name == BarcodeFormat.QR_CODE.name){
                supportFormatList.add(it)
            }
        }
    }
    @Throws(ObjectStreamException::class)
    private fun readResolve(): Any {
        serializableInit()
        return this
    }
    companion object {
        const val serialVersionUID = 1L
    }

    override fun scale(scaleX: Float, scaleY: Float) {
        width += scaleX.coerceAtLeast(scaleY).toInt()
        height = width
    }

    override fun popMenu(labelView: LabelView) {
        val view = View.inflate(labelView.context, R.layout.com_wyc_label_qrcode_item_attr,null)
        showEditDialog(labelView.context,view)

        val et: EditText = view.findViewById(R.id.content)
        et.setText(content)
        if (field.isNotEmpty()){
            et.isEnabled = false
        }
        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }
            override fun afterTextChanged(s: Editable) {
                content = s.toString()
                generateBitmap()
                labelView.postInvalidate()
            }
        })
        et.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus){
                v.tag = content
            }else{
                val old = v.tag as? String ?: content
                if (old != content){
                    addAttrChange(labelView,"content",old,content)
                }
            }
        }

        view.findViewById<Spinner>(R.id.format)?.apply {
            val adapter = ArrayAdapter<String>(labelView.context, R.layout.com_wyc_label_drop_down_style)
            adapter.setDropDownViewResource(R.layout.com_wyc_label_drop_down_style)
            adapter.add(barcodeFormat.name)

            supportFormatList.forEach {
                if (it.name == barcodeFormat.name)return@forEach
                adapter.add(it.name)
            }
            setAdapter(adapter)

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    supportFormatList.forEach {
                        if (it.name == adapter.getItem(position)){
                            barcodeFormat = it
                            generateBitmap()
                            labelView.postInvalidate()
                            return
                        }
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }
        }
    }
}