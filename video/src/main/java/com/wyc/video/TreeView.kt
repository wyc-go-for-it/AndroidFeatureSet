package com.wyc.video

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.wyc.logger.Logger
import kotlin.math.abs


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.video
 * @ClassName:      TreeView
 * @Description:    自定义树形View
 * @Author:         wyc
 * @CreateDate:     2022/9/30 9:52
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/9/30 9:52
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class TreeView: View{
    private val mPaint = Paint()
    private var mHeadItem:Item? = null
    private val mPreGap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics)
    private var mMaxWidth = 0
    private var mHeight = 0

    constructor(context: Context):this(context, null)
    constructor(context: Context, attrs: AttributeSet?):this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int):super(context, attrs, defStyleAttr){
        initPaint()
        initDefaultData()
    }

    private fun initDefaultData(){
        mHeadItem = Item().also {p->
            p.id = 88
            p.code = "880"
            p.name = "列表880"
            p.fold = true
            p.children = mutableListOf()
            for (i in 0..5){
                val item = Item().also {item->
                    item.id = i
                    item.code = (id * 10).toString()
                    item.name = "列表$i"
                    item.fold = true
                    item.parent = p
                    item.children = mutableListOf()
                    for (j in 10..15){
                        val k = Item().apply {
                            id = j
                            code = (id * 10).toString()
                            name = "列表$j"
                            parent = item
                        }
                        item.children.add(k)
                    }
                }
                p.children.add(item)
            }
        }

    }

    private fun initPaint(){
        mPaint.color = Color.BLACK
        mPaint.isAntiAlias = true
        mPaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics)
    }
    private fun measureChild(){
        mHeight = 0
        mMaxWidth = 0

        mHeadItem?.apply {
            recursiveMeasure(this)
        }
    }
    private fun recursiveMeasure(item:Item){
        val bound = Rect()
        mPaint.getTextBounds(item.name,0,item.name.length,bound)

        val w = bound.width()
        val h = bound.height()

        item.sWidth = w
        item.sHeight = h

        val p = item.parent

        if(p == null){
            mMaxWidth = w
        }else if (p.fold){
            if ((w + mPreGap) > mMaxWidth){
                mMaxWidth = (w + mPreGap).toInt()
            }
        }

        if (p == null){
            mHeight += h
        }else if (p.fold){
            mHeight += (h + p.sHeight)
        }

        val ch = item.children;
        if (!ch.isNullOrEmpty()){
            ch.forEach {
              recursiveMeasure(it)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSpec = MeasureSpec.getMode(widthMeasureSpec)
        val heightSpec = MeasureSpec.getMode(heightMeasureSpec)

        var realWidthMeasureSpec = widthMeasureSpec
        var realHeightMeasureSpec = heightMeasureSpec

        if (null != mHeadItem){
            measureChild()

            when(widthSpec){
                MeasureSpec.AT_MOST,MeasureSpec.UNSPECIFIED ->{
                    Logger.d("maxWidth:%d",mMaxWidth)
                    realWidthMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxWidth + paddingLeft + paddingRight,MeasureSpec.EXACTLY)
                }
                MeasureSpec.EXACTLY ->{
                    realWidthMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec) + paddingLeft + paddingRight,MeasureSpec.EXACTLY)
                }
            }

            when(heightSpec){
                MeasureSpec.AT_MOST,MeasureSpec.UNSPECIFIED->{
                    Logger.d("mHeight:%d",mHeight)
                    realHeightMeasureSpec = MeasureSpec.makeMeasureSpec(mHeight + paddingTop + paddingBottom,MeasureSpec.EXACTLY)
                }
                MeasureSpec.EXACTLY->{
                    realHeightMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec) + paddingTop + paddingBottom,MeasureSpec.EXACTLY)
                }
            }
        }
        super.onMeasure(realWidthMeasureSpec, realHeightMeasureSpec)
    }


    override fun layout(l: Int, t: Int, r: Int, b: Int) {
        super.layout(l, t, r, b)
        layoutChild(l,t)
    }
    private fun layoutChild(l: Int, t: Int){
        mHeadItem?.apply {
            sX = l.toFloat()
            sY = t.toFloat()
            val ch = children;
            if (!ch.isNullOrEmpty()){
                for (i in 0 until ch.size){
                    recursiveLayout(ch[i],i)
                }
            }
        }
    }

    private fun recursiveLayout(child:Item,index:Int){
        child.parent?.apply {
            if (fold){
                child.sX = sX + mPreGap
                if (index == 0) {
                    child.sY = sY + calItemHeight(this)
                }else{
                    val sibling = this.children[index - 1]
                    child.sY = sibling.sY + sibling.sHeight
                }
            }
        }
        val ch = child.children;
        if (!ch.isNullOrEmpty()){
            for (i in 0 until ch.size){
                recursiveLayout(ch[i],i)
            }
        }
    }
    private fun calItemHeight(item:Item):Int{
        var h = item.sHeight
        if (item.fold){
            val ch = item.children
            if (!ch.isNullOrEmpty()){
                for (i in 0 until ch.size){
                    h += calItemHeight(ch[i])
                }
            }
        }
        return h
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        drawChild(canvas)
    }
    private fun drawChild(canvas: Canvas){
        mHeadItem?.apply {
            recursiveDraw(this,canvas)
        }
    }

    private fun recursiveDraw(item:Item,canvas: Canvas){
        val p = item.parent
        if (p == null || p.fold){
            val baseLineY = item.sHeight / 2 + (abs(mPaint.fontMetrics.ascent) - mPaint.fontMetrics.descent) / 2
            canvas.drawText(item.name,item.sX,item.sY + baseLineY,mPaint)
        }

        val ch = item.children;
        if (!ch.isNullOrEmpty()){
            ch.forEach {
                recursiveDraw(it,canvas)
            }
        }
    }

    class Item{
        var sX = 0f
        var sY = 0f
        var sWidth = 0
        var sHeight = 0
        var fold:Boolean = false
        var sel:Boolean = false

        var id:Int = 0
        var code:String = ""
        var name:String = ""
        var parent:Item? = null
        var children:MutableList<Item> = mutableListOf()
        var data:Any? = null
    }
}