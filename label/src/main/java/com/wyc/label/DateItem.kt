package com.wyc.label

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*


/**
 *
 * @ProjectName:    AndroidClient
 * @Package:        com.wyc.cloudapp.design
 * @ClassName:      DateItem
 * @Description:    日期
 * @Author:         wyc
 * @CreateDate:     2022/3/28 13:38
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/3/28 13:38
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class DateItem: TextItem() {
    var dateFormat = FORMAT.Y_M_D_H_M_S

    var dateContent = Date().time
    init {
        content = formatDate()
    }

    var autoUpdate = false

    override fun drawItem(offsetX: Float, offsetY: Float, canvas: Canvas, paint: Paint) {
        if (autoUpdate){
            dateContent = Date().time
            val v = formatDate()
            val index = content.indexOf("\n")
            content = if (index != -1 && v.length > index){
                val stringBuilder = StringBuilder(v)
                stringBuilder.insert(index,"\n").toString()
            }else v
        }
        super.drawItem(offsetX, offsetY, canvas, paint)
    }

    override fun popMenu(labelView: LabelView) {
        val view = View.inflate(labelView.context, R.layout.com_wyc_label_date_item_attr,null)
        super.showTextEditDialog(labelView,view)

        val dateView = view.findViewById<EditText>(R.id.content)
        dateView.setOnClickListener {
            if (!autoUpdate){
                val calendar = Calendar.getInstance()
                DatePickerDialog(labelView.context,
                    { _, year, month, dayOfMonth ->

                        TimePickerDialog(
                            labelView.context,
                            { _, hourOfDay, minute ->
                                calendar.set(Calendar.YEAR,year)
                                calendar.set(Calendar.MONTH,month)
                                calendar.set(Calendar.DAY_OF_MONTH,dayOfMonth)
                                calendar.set(Calendar.HOUR_OF_DAY,hourOfDay)
                                calendar.set(Calendar.MINUTE,minute)

                                dateContent = calendar.time.time
                                dateView.setText(formatDate())
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                    }
                    // 设置初始日期
                    , calendar.get(Calendar.YEAR)
                    ,calendar.get(Calendar.MONTH)
                    ,calendar.get(Calendar.DAY_OF_MONTH)).show()
            }
        }

        view.findViewById<Spinner>(R.id.format)?.apply {
            val adapter = ArrayAdapter<String>(labelView.context, R.layout.com_wyc_label_drop_down_style)
            adapter.setDropDownViewResource(R.layout.com_wyc_label_drop_down_style)
            adapter.add(dateFormat.description)

            FORMAT.values().forEach {
                if (it.description == dateFormat.description)return@forEach
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
                    FORMAT.values().forEach {
                        if (it.description == adapter.getItem(position)){
                            dateFormat = it
                            dateView.setText(formatDate())
                            return
                        }
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }
        }
        val bCheckBox: CheckBox = view.findViewById(R.id.auto_update)
        bCheckBox.isChecked = autoUpdate
        bCheckBox.setOnCheckedChangeListener{ _, check ->
            autoUpdate = check
            labelView.postInvalidate()
        }
    }
    private fun formatDate():String{
        val fm = SimpleDateFormat(dateFormat.format, Locale.CHINA)
        return fm.format(dateContent)
    }

    enum class FORMAT(f:String,d:String){
        Y_M("YYYY-MM","年-月"),Y_M_D("YYYY-MM-dd","年-月-日"),Y_M_D_H_M("YYYY-MM-dd HH:mm","年-月-日 时:分"),
        Y_M_D_H_M_S("YYYY-MM-dd HH:mm:ss","年-月-日 时:分:秒"),D_M_Y("dd-MM-YYYY","日-月-年");
        val format = f
        val description = d
    }
}