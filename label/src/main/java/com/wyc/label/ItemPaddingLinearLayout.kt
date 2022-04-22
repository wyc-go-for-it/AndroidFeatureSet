package com.wyc.label

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.LinearLayout


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.label
 * @ClassName:      ItemPaddingLinearLayout
 * @Description:    作用描述
 * @Author:         wyc
 * @CreateDate:     2022/4/22 17:09
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/4/22 17:09
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class ItemPaddingLinearLayout: LinearLayout {
    private var disable = false
    private var mCentreLabel: String? = null
    private var mPaint: Paint? = null
    private var mTextBounds: Rect? = null


    constructor(context: Context):this(context, null)
    constructor(context: Context, attrs: AttributeSet?):this(context, attrs, 0)
    @SuppressLint("ResourceType")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int):super(context, attrs, defStyleAttr){
        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.ItemPaddingLinearLayout, 0, 0)
        init(typedArray.getDimension(0, 1f), typedArray.getColor(1, Color.TRANSPARENT))
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDrawForeground(canvas: Canvas) {
        super.onDrawForeground(canvas)
        if (isDrawLabel()) {
            drawLabel(canvas)
        }
    }

    private fun isDrawLabel(): Boolean {
        return mCentreLabel != null && !mCentreLabel!!.isEmpty() && mTextBounds != null && mPaint != null
    }

    private fun drawLabel(canvas: Canvas) {
        val width = measuredWidth
        val height = measuredHeight
        val t_w = mTextBounds!!.width()
        val t_h = mTextBounds!!.height()
        val w = t_w shl 1
        val h = t_h * 3
        val left = width - w shr 1
        val top = (height - h) / 3
        val rect = RectF(left.toFloat(), top.toFloat(), (w + left).toFloat(), (h + top).toFloat())
        canvas.save()
        matrixToRect(canvas, width, height, rect)
        canvas.drawText(mCentreLabel!!, (left + (w - t_w shr 1)).toFloat(), (top + h - t_h).toFloat(), mPaint!!)
        mPaint!!.style = Paint.Style.STROKE
        canvas.drawRect(rect, mPaint!!)
        canvas.restore()
    }

    private fun matrixToRect(canvas: Canvas, width: Int, height: Int, rect: RectF) {
        val degrees = 15f
        val r_w = rect.width().toDouble()
        val r_h = rect.height().toDouble()
        val radian = Math.PI / 180 * degrees
        val new_w = Math.cos(radian) * r_w + Math.sin(radian) * r_h
        val new_h = Math.sin(radian) * r_w + Math.cos(radian) * r_h
        val scale = Math.min(width / new_w, height / new_h) - 0.05
        if (lessDouble(scale, 1.0)) {
            canvas.scale(scale.toFloat(), scale.toFloat(), rect.centerX(), rect.centerY())
        }
        canvas.rotate(degrees, rect.centerX(), rect.centerY())
    }

    private fun lessDouble(a: Double, b: Double): Boolean {
        return a - b < -0.00001
    }

    fun setCentreLabel(label: String?) {
        if (null == label) return
        if (mPaint == null || mTextBounds == null) {
            val paint = Paint()
            paint.isAntiAlias = true
            paint.color = Color.RED
            val bounds = Rect()
            paint.textSize = resources.getDimension(R.dimen.com_wyc_label_font_size_18)
            paint.getTextBounds(label, 0, label.length, bounds)
            mPaint = paint
            mTextBounds = bounds
        } else {
            mPaint!!.getTextBounds(label, 0, label.length, mTextBounds)
        }
        mCentreLabel = label
        invalidate()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return if (disable && !(ev.action == MotionEvent.ACTION_MOVE || ev.action == MotionEvent.ACTION_DOWN)) { //过滤滑动
            true
        } else super.onInterceptTouchEvent(ev)
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return disable || super.dispatchKeyEvent(event)
    }

    fun setDisableEvent(b: Boolean) {
        disable = b
    }

    private fun init(padding: Float, c: Int) {
        val drawable = GradientDrawable()
        if (orientation == HORIZONTAL) {
            drawable.setSize(padding.toInt(), 0)
        } else drawable.setSize(0, padding.toInt())
        drawable.setColor(c)
        dividerDrawable = drawable
        showDividers = SHOW_DIVIDER_MIDDLE
    }
}