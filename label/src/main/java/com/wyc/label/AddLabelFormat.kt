package com.wyc.label

import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*


/**
 *
 * @ProjectName:    AndroidClient
 * @Package:        com.wyc.cloudapp.design
 * @ClassName:      AddLabelFormat
 * @Description:    新增标签设计格式对话框
 * @Author:         wyc
 * @CreateDate:     2022/4/6 13:40
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/4/6 13:40
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class AddLabelFormat(context: Context): Dialog(context) {
    private val mLabelTemplate = LabelTemplate()
    private var mContentListener: OnContent? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.com_wyc_label_add_label_format)
        setCanceledOnTouchOutside(false)

        initLabelSize()
        initView()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        initWindowSize()
    }

    private fun initView(){
        findViewById<TextView>(R.id.title).setText(R.string.com_wyc_label_add_label)
        findViewById<Button>(R.id._close).setOnClickListener {
            dismiss()
        }
        findViewById<Button>(R.id.cancel).setOnClickListener {
            dismiss()
        }
        findViewById<Button>(R.id.ok).setOnClickListener {
            dismiss()
            mContentListener?.content(getContent())
        }
    }

    private fun initWindowSize() {
        val d: Display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay // 获取屏幕宽、高用
        val point = Point()
        d.getSize(point)
        window?.apply {
            setGravity(Gravity.CENTER)
            val lp = attributes
            lp.width = (0.98 * point.x).toInt()
            attributes = lp
        }
    }

    private fun getContent():LabelTemplate{
        var name = findViewById<EditText>(R.id.label_name).text.toString()
        try {
            val width = findViewById<EditText>(R.id.label_width).text.toString().toInt()
            val height = findViewById<EditText>(R.id.label_height).text.toString().toInt()

            mLabelTemplate.width = width
            mLabelTemplate.height = height
            if (name.isEmpty()){
                name = String.format("未命名_%d_%d", width, height)
            }
            mLabelTemplate.templateName = name
        }catch (e:NumberFormatException){
            Utils.showToast(e.message)
        }
        return mLabelTemplate
    }
    fun setListener(l: OnContent){
        mContentListener = l
    }
    private fun initLabelSize(){
        findViewById<Spinner>(R.id.default_size)?.apply {
            val adapter = ArrayAdapter<String>(context, R.layout.com_wyc_label_drop_down_style)
            adapter.setDropDownViewResource(R.layout.com_wyc_label_drop_down_style)
            LabelTemplate.getDefaultSize().forEach {
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
                    LabelTemplate.getDefaultSize().forEach {
                        if (it.description == adapter.getItem(position)){
                            this@AddLabelFormat.findViewById<EditText>(R.id.label_width)?.setText(it.getrW().toString())
                            this@AddLabelFormat.findViewById<EditText>(R.id.label_height)?.setText(it.getrH().toString())
                            return
                        }
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }
        }
    }
    interface OnContent{
        fun content(labelTemplate: LabelTemplate)
    }
}