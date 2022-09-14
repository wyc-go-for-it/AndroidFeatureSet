package com.wyc.label

import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import androidx.annotation.Nullable


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.label
 * @ClassName:      IPInputDialog
 * @Description:    网卡打印机输入
 * @Author:         wyc
 * @CreateDate:     2022/9/8 15:18
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/9/8 15:18
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class IPInputDialog(context: Context): Dialog(context,R.style.com_wyc_label_MyDialog) {
    private var  mListener:OnContent? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.com_wyc_label_ip_input)
        initView()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        initWindowSize()
    }
    private fun initWindowSize(){
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val d: Display = wm.defaultDisplay // 获取屏幕宽、高用
        val point = Point()
        d.getSize(point)

        window?.apply {
            setWindowAnimations(R.style.com_wyc_label_bottom_pop_anim)
            val wlp: WindowManager.LayoutParams = attributes
            wlp.gravity = Gravity.BOTTOM
            wlp.y = 68
            wlp.width = (point.x * 0.95).toInt()
            attributes = wlp
        }
    }

    private fun initView(){
        findViewById<Button>(R.id.ok).setOnClickListener {
            if (mListener != null){
                val ip = findViewById<EditText>(R.id.ip)?.text.toString()
                val port = findViewById<EditText>(R.id.port).text.toString()
                if (ip.isEmpty()){
                    Utils.showToast(R.string.com_wyc_label_ip_not_empty_hint)
                    findViewById<EditText>(R.id.ip)?.requestFocus()
                    return@setOnClickListener
                }
                mListener!!.content(ip,port)
            }
            dismiss()
        }
    }
    interface OnContent{
        fun content(@Nullable ip:String , port:String)
    }
    fun setListener(l:OnContent){
        mListener = l
    }
}