package com.wyc.label

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.label
 * @ClassName:      BaseActivity
 * @Description:    作用描述
 * @Author:         wyc
 * @CreateDate:     2022/4/25 14:03
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/4/25 14:03
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

abstract class BaseActivity: AppCompatActivity() {
    private var mLeft: TextView? = null
    private var mMiddle:TextView? = null
    private var mRight:TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout()
        initTitle()
    }
    private fun initTitle() {
        mLeft = findViewById(R.id.left_title_tv)
        mMiddle = findViewById(R.id.middle_title_tv)
        mRight = findViewById(R.id.right_title_tv)
        mLeft?.setOnClickListener { onBackPressed() }
    }

    protected open fun setMiddleText(text: String?) {
        if (text != null && mMiddle != null) {
            mMiddle!!.text = text
        }
    }
    protected open fun setRightText(text: String?) {
        if (text != null && mRight != null) {
            mRight!!.text = text
        }
    }
    protected open fun setRightListener(listener: View.OnClickListener?) {
        if (mRight != null) mRight!!.setOnClickListener(listener)
    }

    private fun setContentLayout() {
        setContentView(R.layout.com_wyc_layout_activity_main)
        findViewById<LinearLayout>(R.id._main)?.apply {
            val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            this.layoutParams = layoutParams
            View.inflate(this@BaseActivity, getContentLayoutId(), this)
        }
    }
    abstract fun getContentLayoutId():Int
}