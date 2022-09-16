package com.wyc.video.activity

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.wyc.video.R
import com.wyc.video.VideoApp


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
        setTitleColor()
    }
    private fun initTitle() {
        mLeft = findViewById(R.id.left_title_tv)
        mMiddle = findViewById(R.id.middle_title_tv)
        mRight = findViewById(R.id.right_title_tv)
        mLeft?.setOnClickListener { onBackPressed() }
    }

    private fun setTitleColor(){
        window.statusBarColor = VideoApp.themeColor()
        findViewById<View>(R.id.title).setBackgroundColor(VideoApp.themeColor())
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
        setContentView(R.layout.wyc_video_layout_activity_title)
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