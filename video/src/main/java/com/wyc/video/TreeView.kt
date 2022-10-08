package com.wyc.video

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
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
    private val mSelectedList = mutableListOf<Item>()
    private var mSingleSelection = true

    private val mPreGap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18f, resources.displayMetrics)
    private val mVerGap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics)
    private val mLogoGap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics)

    private var mMaxWidth = 0
    private var mHeight = 0

    private val mTextColor = Color.BLACK
    private val mFoldLogoColor = Color.RED

    private var mDashPathEffect:DashPathEffect? = null

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
            p.name = "菜单880"
            p.fold = true
            p.children = mutableListOf()
            for (i in 0..3){
                val item = Item().also {item->
                    item.id = i
                    item.code = (id * 10).toString()
                    item.name = "列表$i"

                    if (i != 1)
                    item.fold = true

                    item.parent = p
                    item.children = mutableListOf()
                    for (j in 10..12){
                        val k = Item().also {k->
                            k.id = j
                            k.code = (id * 10).toString()
                            k.name = "列表$i$j"
                            k.parent = item
                            k.fold = true
                            k.parent = item
                            k.children = mutableListOf()

                            if (j == 11 && i == 0)
                            for(pp in 20..25){
                                val kk = Item().also {kk ->
                                    kk.id = pp
                                    kk.code = (id * 10).toString()
                                    kk.name = "列表$pp$i$j"
                                    kk.parent = k

                                    if (pp == 23){
                                        for (kkk in 30..35){
                                            val bbb = Item().apply {
                                                id = pp
                                                code = (id * 10).toString()
                                                name = "列表$kkk$pp$i$j"
                                                parent = kk
                                            }
                                            kk.children.add(bbb)
                                        }

                                    }

                                }
                                k.children.add(kk)
                            }

                        }
                        item.children.add(k)
                    }
                }
                p.children.add(item)
            }
        }

    }

    private fun initPaint(){
        mPaint.color = mTextColor
        mPaint.isAntiAlias = true
        mPaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics)
    }
    private fun measureChild(){
        mHeight = 0
        mMaxWidth = 0

        mHeadItem?.apply {
            recursiveMeasure(this,0)
        }
    }
    private fun recursiveMeasure(item:Item,index: Int){
        val bound = Rect()
        mPaint.getTextBounds(item.name,0,item.name.length,bound)

        val w = bound.width()
        val h = bound.height()

        item.sWidth = w

        item.sHeight = h

        val p = item.parent

        if(p == null){
            mMaxWidth = w
            item.sX = mLogoGap * 2f
        }else if (p.fold){
            item.sX = p.sX + mPreGap + mLogoGap
            if ((w + item.sX) > mMaxWidth){
                mMaxWidth = (w + item.sX).toInt()
            }
        }

        if (p?.fold == true){
            if (index == 0) {
                item.sY = p.sY + p.sHeight + mVerGap
            }else{
                val sibling = p.children[index - 1]
                item.sY = sibling.sY + calItemHeight(sibling,bound)
            }
        }

        val ch = item.children;
        if (!ch.isNullOrEmpty()){
            for (i in 0 until ch.size)
                recursiveMeasure(ch[i],i)
        }

        if (item.sY > mHeight)
            mHeight = item.sY.toInt() + h
    }

    private fun calItemHeight(item:Item,bound:Rect):Int{
        mPaint.getTextBounds(item.name,0,item.name.length,bound)
        var h = bound.height() + mVerGap.toInt()
        if (item.fold){
            val ch = item.children
            if (!ch.isNullOrEmpty()){
                for (i in 0 until ch.size){
                    h += calItemHeight(ch[i],bound)
                }
            }
        }
        return h
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
                    realWidthMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxWidth + paddingLeft + paddingRight,MeasureSpec.EXACTLY)
                }
                MeasureSpec.EXACTLY ->{
                    realWidthMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec) + paddingLeft + paddingRight,MeasureSpec.EXACTLY)
                }
            }

            when(heightSpec){
                MeasureSpec.AT_MOST,MeasureSpec.UNSPECIFIED->{
                    realHeightMeasureSpec = MeasureSpec.makeMeasureSpec(mHeight + paddingTop + paddingBottom,MeasureSpec.EXACTLY)
                }
                MeasureSpec.EXACTLY->{
                    realHeightMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec) + paddingTop + paddingBottom,MeasureSpec.EXACTLY)
                }
            }
        }
        super.onMeasure(realWidthMeasureSpec, realHeightMeasureSpec)
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {

        mHeadItem?.apply {
            sX = mLogoGap + mPreGap
            sY = 0f
            layoutChild(this,left + paddingLeft,top + paddingTop)
        }
        super.onLayout(changed, left, top, right, bottom)
    }
    private fun layoutChild(item:Item,l: Int, t: Int){
        item.sX += l.toFloat()
        item.sY += t.toFloat()

        val ch = item.children;
        if (!ch.isNullOrEmpty()){
            for (i in 0 until ch.size){
                layoutChild(ch[i],l,t)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        drawChild(canvas)
        super.onDraw(canvas)
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
            if (item.sel){
                val offset = mVerGap * 0.25f
                mPaint.style = Paint.Style.STROKE
                if (mDashPathEffect == null)mDashPathEffect = DashPathEffect(floatArrayOf(4f,4f),0f)
                mPaint.pathEffect = mDashPathEffect
                canvas.drawRoundRect(item.sX - offset,item.sY - offset,item.sX + item.sWidth + offset,item.sY + item.sHeight + offset,2f,2f,mPaint)
                mPaint.style = Paint.Style.FILL
                mPaint.pathEffect = null
            }

            drawLogo(canvas,item)

            val ch = item.children;
            if (!ch.isNullOrEmpty()){
                ch.forEach {
                    recursiveDraw(it,canvas)
                }
            }
        }
    }
    private fun drawLogo(canvas: Canvas,item: Item){
        val c = !item.children.isNullOrEmpty()
        if (c){
            val offsetX = mLogoGap * 0.5f
            val logoSize = mLogoGap * 0.95f

            mPaint.color = mFoldLogoColor
            mPaint.style = Paint.Style.STROKE

            val y = item.sY + item.sHeight / 2
            val startX = (item.sX - logoSize) - offsetX
            val stopX = item.sX - offsetX

            val cX = (item.sX - logoSize /2f) - offsetX

            canvas.drawCircle(cX, y, mLogoGap / 1.5f , mPaint)

            if (item.fold){
                canvas.drawLine(startX,y,stopX,y,mPaint)
            }else{
                canvas.drawLine(startX,y,stopX,y,mPaint)
                canvas.save()
                canvas.rotate(90f, cX, y)
                canvas.drawLine(startX,y,stopX,y,mPaint)
                canvas.restore()
            }

            mPaint.color = mTextColor
            mPaint.style = Paint.Style.FILL
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        mHeadItem?.apply {
            clickItem(this,event.x,event.y)
        }

        return super.onTouchEvent(event)
    }
    private fun clickItem(item: Item,x:Float,y:Float):Boolean{
        var hasDraw = false
        val bH = y>= item.sY && y<= item.sY + item.sHeight
        if (x>= item.sX - mLogoGap * 2 && x<= item.sX && bH){
            item.fold = !item.fold
            hasDraw = true
        }else if (x >= item.sX && x <= item.sX + item.sWidth && bH){
            item.sel = !item.sel
            hasDraw = true
            if (mSingleSelection){
                if (mSelectedList.isNotEmpty()){
                    mSelectedList.removeAt(0).sel = false
                }
                if (item.sel){
                    mSelectedList.add(item)
                }
            }else{
                if (item.sel){
                    mSelectedList.add(item)
                }else mSelectedList.remove(item)
            }
        }else if (item.fold){
            val ch = item.children
            if (!ch.isNullOrEmpty()){
                run out@{
                    ch.forEach {
                        if (clickItem(it, x, y)) {
                            return@out
                        }
                    }
                }
            }
        }

        if (hasDraw){
            requestLayout()
            invalidate()
        }

        return hasDraw
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
        override fun toString(): String {
            return "Item(sX=$sX, sY=$sY, sWidth=$sWidth, sHeight=$sHeight, fold=$fold, name='$name')"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Item

            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int {
            return id
        }

    }
}