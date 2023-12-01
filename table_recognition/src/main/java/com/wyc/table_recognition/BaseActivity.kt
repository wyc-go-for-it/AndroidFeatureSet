package com.wyc.table_recognition

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {
    private var mLeft: TextView? = null
    private var mMiddle: TextView? = null
    private var mRight: TextView? = null

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
        window.statusBarColor = App.themeColor()
        findViewById<View>(R.id.title).setBackgroundColor(App.themeColor())
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
        setContentView(R.layout.activity_title)
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