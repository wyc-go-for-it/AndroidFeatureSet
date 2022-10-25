package com.wyc.video

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.EdgeEffect
import android.widget.OverScroller
import androidx.core.graphics.ColorUtils
import com.wyc.logger.Logger
import kotlin.math.*


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
    private var mHeadItem: Item =  Item()
    private var mCurItem:Item? = null
    private val mSelectedList = mutableListOf<Item>()
    private var mSingleSelection = true

    private var mBoxSize = 0f
    private val mPreGap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18f, resources.displayMetrics)
    private val mVerGap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics)

    private var mLogoSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics)
    set(value) {
        mBoxSize = value * if (mSingleSelection) 0.5f else 0.4f
        field = value
    }

    private val mLogoGap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics)

    private var mMaxWidth = 0
    private var mHeight = 0

    private val mTextColor = Color.BLACK
    private val mFoldLogoColor = Color.RED

    private var mDashPathEffect:DashPathEffect? = null

    private var downX = 0f
    private var downY = 0f
    private var hasMove = false

    private var mScroller: OverScroller = OverScroller(context)
    private var mSlideDirection = SLIDE.DOWN

    private val mEdgeEffect: EdgeEffect

    private var hasSelect = true
    private val mSelectBoxColor = Color.RED


    constructor(context: Context):this(context, null)
    constructor(context: Context, attrs: AttributeSet?):this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int):super(context, attrs, defStyleAttr){
        initPaint()
        initDefaultData()

        mEdgeEffect = EdgeEffect(context)
        mEdgeEffect.color = Color.RED
    }

    private fun initDefaultData(){
        mHeadItem.also { p->
            p.id = 88
            p.code = "880"
            p.name = "菜单880"
            p.unfold = true
            p.children = mutableListOf()
            for (i in 0..3){
                val item = Item().also { item->
                    item.id = i
                    item.code = (id * 10).toString()
                    item.name = "列表$i"

                    if (i != 1)
                    item.unfold = true

                    item.parent = p
                    item.children = mutableListOf()
                    for (j in 10..12){
                        val k = Item().also { k->
                            k.id = j
                            k.code = (id * 10).toString()
                            k.name = "列表$i$j"
                            k.parent = item
                            k.unfold = true
                            k.parent = item
                            k.children = mutableListOf()

                            if ((j == 12 || j == 11) && i == 0)
                            for(pp in 20..25){
                                val kk = Item().also { kk ->
                                    kk.id = pp
                                    kk.code = (id * 10).toString()
                                    kk.name = "列表$pp$i$j"
                                    kk.parent = k
                                    kk.children = mutableListOf()
                                    if (pp == 23){
                                        for (kkk in 30..33){
                                            val bbb = Item().also {bbb->
                                                bbb.id = kkk
                                                bbb.code = (id * 10).toString()
                                                bbb.name = "列表$kkk$pp$i$j"
                                                bbb.parent = kk
                                                bbb.children = mutableListOf()
                                                var end = 43
                                                if (kkk == 30){
                                                    end = 42
                                                }else if (kkk == 31){
                                                    end = 41
                                                }
                                                for (kkkk in 40..end){
                                                    val bbbb = Item().apply {
                                                        id = kkkk
                                                        code = (id * 10).toString()
                                                        name = "列表$kkkk$kkk$pp$i$j"
                                                        parent = bbb
                                                    }

                                                    bbb.children!!.add(bbbb)
                                                }

                                            }
                                            kk.children!!.add(bbb)
                                        }

                                    }

                                }
                                k.children!!.add(kk)
                            }

                        }
                        item.children!!.add(k)
                    }
                }
                p.children!!.add(item)
            }
        }

    }

    private fun initPaint(){
        mPaint.color = mTextColor
        mPaint.isAntiAlias = true
        mPaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics)
    }
    private fun measureChild(){
        mHeight = 0
        mMaxWidth = 0

        val bound = Rect()
        recursiveMeasure(mHeadItem,0,bound)
        mLogoSize = bound.height() * 0.8f

        if ((mHeight < measuredHeight && scrollY != 0) || (mMaxWidth < measuredWidth && scrollX != 0)){
            scrollTo(0,0)
        }
    }
    private fun recursiveMeasure(item: Item, index: Int,bound: Rect){

        mPaint.getTextBounds(item.name,0,item.name.length,bound)

        val w = bound.width()
        val h = bound.height()

        item.sWidth = w

        item.sHeight = h

        val p = item.parent

        if(p == null){
            mMaxWidth = w
            item.sX = mLogoSize + mLogoGap
        }else if (p.unfold){
            item.sX = p.sX + mPreGap + mLogoSize + mLogoGap
            if ((w + item.sX) > mMaxWidth){
                mMaxWidth = (w + item.sX).toInt()
            }
        }

        if (p?.unfold == true){
            if (index == 0) {
                item.sY = p.sY + p.sHeight + mVerGap
            }else{
                p.children?.get(index - 1)?.also {sibling->
                    item.sY = sibling.sY + calItemHeight(sibling,bound)
                }
            }
            item.sAnimY = item.sY
        }
        mHeight = item.sY.toInt() + h
        if (item.unfold){
            val ch = item.children;
            if (!ch.isNullOrEmpty()){
                for (i in 0 until ch.size)
                    recursiveMeasure(ch[i],i,bound)
            }
        }
    }

    private fun calItemHeight(item: Item, bound:Rect):Int{
        mPaint.getTextBounds(item.name,0,item.name.length,bound)
        var h = bound.height() + mVerGap.toInt()
        if (item.unfold){
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

        measureChild()

        when(widthSpec){
            MeasureSpec.AT_MOST,MeasureSpec.UNSPECIFIED ->{
                realWidthMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxWidth + paddingLeft + paddingRight,MeasureSpec.EXACTLY)
            }
            MeasureSpec.EXACTLY ->{
                realWidthMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec),MeasureSpec.EXACTLY)
            }
        }

        when(heightSpec){
            MeasureSpec.AT_MOST,MeasureSpec.UNSPECIFIED->{
                realHeightMeasureSpec = MeasureSpec.makeMeasureSpec(mHeight + paddingTop + paddingBottom,MeasureSpec.EXACTLY)
            }
            MeasureSpec.EXACTLY->{
                realHeightMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec),MeasureSpec.EXACTLY)
            }
        }

        super.onMeasure(realWidthMeasureSpec, realHeightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        layoutChild(mHeadItem,left + paddingLeft - paddingRight,top + paddingTop - paddingBottom)
        super.onLayout(changed, left, top, right, bottom)
    }
    private fun layoutChild(item: Item, l: Int, t: Int){
        item.sX += l.toFloat()
        item.sY += t.toFloat()
        item.sAnimY = item.sY
        val ch = item.children;
        if (!ch.isNullOrEmpty()){
            for (i in 0 until ch.size){
                layoutChild(ch[i],l,t)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mEdgeEffect.setSize(max(measuredWidth,mMaxWidth), mHeight shr 2)
    }

    override fun onDraw(canvas: Canvas) {
        drawChild(canvas)
        super.onDraw(canvas)
    }
    private fun drawChild(canvas: Canvas){
        drawLine(canvas)
        recursiveDraw(mHeadItem,canvas)
    }

    private fun recursiveDraw(item: Item, canvas: Canvas){
        val p = item.parent
        if (p == null || p.unfold){

            val baseLineY = item.sHeight / 2 + (abs(mPaint.fontMetrics.ascent) - mPaint.fontMetrics.descent) / 2
            val offsetH = if (p == null && hasSelect) mLogoSize + mLogoGap else 0f
            canvas.drawText(item.name,item.sX + offsetH,item.sAnimY + baseLineY,mPaint)

            if (item.click){
                val offset = mVerGap * 0.25f
                mPaint.style = Paint.Style.STROKE
                if (mDashPathEffect == null)mDashPathEffect = DashPathEffect(floatArrayOf(4f,4f),0f)
                mPaint.pathEffect = mDashPathEffect
                canvas.drawRoundRect(item.sX - offset + offsetH,item.sAnimY - offset,item.sX + item.sWidth + offset + offsetH,item.sAnimY + item.sHeight + offset,2f,2f,mPaint)
                mPaint.style = Paint.Style.FILL
                mPaint.pathEffect = null
            }

            drawLogo(canvas,item)
            if (hasSelect)drawSelectBox(canvas,item)

            val ch = item.children;
            if (!ch.isNullOrEmpty()){
                ch.forEach {
                    recursiveDraw(it,canvas)
                }
            }
        }
    }

    private fun drawLogo(canvas: Canvas,item: Item){
        if (!item.children.isNullOrEmpty()){
            val sX = item.parent?.sX?:item.sX

            mPaint.color = mFoldLogoColor
            mPaint.style = Paint.Style.STROKE

            val y = item.sAnimY + item.sHeight / 2
            val startX = sX - mLogoSize + 5 - mLogoGap
            val stopX = sX - mLogoGap - 5

            val cX = sX - mLogoSize /2f - mLogoGap

            canvas.drawCircle(cX, y, mLogoSize * 0.5f , mPaint)

            if (item.unfold){
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

    private fun drawLine(canvas: Canvas){
        if (mHeadItem.unfold){
            if (mDashPathEffect == null)mDashPathEffect = DashPathEffect(floatArrayOf(4f,4f),0f)
            mPaint.pathEffect = mDashPathEffect
            drawConnectingLine(canvas,mHeadItem)
            mPaint.pathEffect = null
        }
    }

    private fun drawConnectingLine(canvas: Canvas,item: Item){
        val children = item.children
        if (!children.isNullOrEmpty()){
            val offset = mLogoSize * 0.5f
            val offsetBox = if (hasSelect) mBoxSize * 2.3f else 0f

            var child = children[0]
            var gap = if (!child.children.isNullOrEmpty()) offset else 0f
            var lastGap = if (item.parent == null) offset else 0f

            var startX = item.sX - offset - mLogoGap
            var startY = item.sAnimY + (item.sHeight shr 1)

            var stopX = startX
            var stopY = child.sAnimY + (child.sHeight shr 1)

            if (item.parent == null)
                canvas.drawLine(startX,startY + lastGap ,stopX,stopY - gap,mPaint)
            else
                canvas.drawLine(startX,startY + lastGap + offsetBox * 0.5f,stopX,stopY - gap,mPaint)

            startX = stopX
            startY = stopY

            stopX = child.sX - mLogoGap


            canvas.drawLine(startX + gap,startY,stopX - offsetBox,stopY,mPaint)


            if (child.unfold)drawConnectingLine(canvas,child)

            for (i in 1 until children.size){
                child = children[i]
                gap = if (!child.children.isNullOrEmpty()) offset else 0f
                lastGap = if (!children[i - 1].children.isNullOrEmpty()) offset else 0f

                stopX = startX
                stopY = child.sAnimY + (child.sHeight shr 1)

                canvas.drawLine(startX,startY + lastGap,stopX,stopY - gap,mPaint)

                startX = stopX
                startY = stopY

                stopX = child.sX - mLogoGap

                canvas.drawLine(startX + gap,startY,stopX - offsetBox,stopY,mPaint)

                if (child.unfold)drawConnectingLine(canvas,child)
            }
        }
    }

    private fun drawSelectBox(canvas: Canvas,item:Item){

        val centreX = if (item.parent != null) item.sX - mLogoGap - mLogoSize * 0.5f else item.sX + mLogoSize * 0.5f
        val centreY = item.sAnimY  + item.sHeight * 0.5f

        mPaint.color = mSelectBoxColor

        val l = centreX - mBoxSize
        val t = centreY - mBoxSize
        val r = centreX + mBoxSize
        val b = centreY + mBoxSize

        val old = mPaint.strokeWidth

        if (item.selectedState == SELECT_STATE.ALLSEL){
            if (mSingleSelection){
                canvas.drawCircle(centreX ,centreY ,mBoxSize,mPaint)
            }else
                canvas.drawRoundRect(l ,t ,r,b ,5f,5f,mPaint)

            mPaint.style = Paint.Style.STROKE

            canvas.save()
            canvas.scale(0.8f,0.8f,centreX,centreY)

            val path = Path()
            path.moveTo(centreX - mBoxSize * 0.7f,centreY + mBoxSize * 0.1f)
            path.lineTo(centreX - mBoxSize * 0.2f ,centreY + mBoxSize * 0.6f)

            path.lineTo(centreX + mBoxSize * 0.7f,centreY - mBoxSize * 0.6f)


            val oldCap = mPaint.strokeCap

            mPaint.strokeWidth = 6f
            mPaint.strokeCap = Paint.Cap.ROUND

            val a = Color.alpha(mSelectBoxColor)
            mPaint.color = Color.argb(a,255 - Color.red(mSelectBoxColor),255 - Color.green(mSelectBoxColor),255 - Color.blue(mSelectBoxColor))

            canvas.drawPath(path,mPaint)

            mPaint.strokeCap = oldCap

            canvas.restore()
        }else{
            mPaint.style = Paint.Style.STROKE
            mPaint.strokeWidth = 2f
            if (mSingleSelection){
                canvas.drawCircle(centreX,centreY,mBoxSize,mPaint)
            }else
                canvas.drawRoundRect(l ,t ,r,b ,5f,5f,mPaint)
            if (item.selectedState == SELECT_STATE.HALFSEL){
                mPaint.style = Paint.Style.FILL
                if (mSingleSelection){
                    canvas.drawCircle(centreX,centreY,mBoxSize - 6f,mPaint)
                }else
                    canvas.drawRoundRect(l + 6f ,t + 6f ,r - 6f,b -6f ,5f,5f,mPaint)
            }
        }

        mPaint.color = mTextColor
        mPaint.style = Paint.Style.FILL
        mPaint.strokeWidth = old
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.action){
            MotionEvent.ACTION_DOWN->{
                downX = event.x
                downY = event.y
                hasMove = false

                return true
            }
            MotionEvent.ACTION_MOVE->{
                val moveX = event.x
                val moveY = event.y

                val xDiff = abs(moveX - downX)
                val yDiff = abs(moveY - downY)
                val squareRoot = sqrt((xDiff * xDiff + yDiff * yDiff).toDouble())
                val degreeX = asin(yDiff / squareRoot) * 180 / Math.PI
                val degreeY = asin(xDiff / squareRoot) * 180 / Math.PI

                if (degreeX < 45){
                    if (mMaxWidth > width){
                        if (moveX > downX){
                            if (scrollX > 0){
                                mSlideDirection = SLIDE.RIGHT
                                mScroller.startScroll(moveX.toInt(),0,(downX - moveX).toInt(),0)
                                invalidate()
                            }
                        }else{
                            mSlideDirection = SLIDE.LIFT
                            if (mMaxWidth - width > scrollX){
                                mScroller.startScroll(moveX.toInt(),0,(downX - moveX).toInt(),0)
                                invalidate()
                            }
                        }
                    }
                    hasMove = true
                }else if (degreeY < 45 && mHeight > height){
                     if (moveY > downY){
                         if (scrollY > 0){
                             mSlideDirection = SLIDE.DOWN
                             mScroller.startScroll(0, moveY.toInt(),0,(downY - moveY).toInt())
                             invalidate()
                         }else edgeVerPull(event)
                    }else{
                         mSlideDirection = SLIDE.UP
                        if (mHeight - height > scrollY){
                            mScroller.startScroll(0, moveY.toInt(),0,(downY - moveY).toInt())
                            invalidate()
                        }else edgeVerPull(event)
                    }
                    hasMove = true
                }
                downX = moveX
                downY = moveY
            }
            MotionEvent.ACTION_UP->{
                if (!hasMove){
                    clickItem(mHeadItem,event.x,event.y)
                }
                edgeRelease()
                return performClick()
            }
        }

        return super.onTouchEvent(event)
    }

    override fun computeScroll() {
        super.computeScroll()
        if (mScroller.computeScrollOffset()){
            val offsetX = mScroller.finalX - mScroller.currX
            val offsetY = mScroller.finalY - mScroller.currY
            when(mSlideDirection){
                SLIDE.DOWN ->{
                    if (scrollY > 0){
                        scrollBy(0,offsetY)
                    }else scrollTo(scrollX,0)
                }
                SLIDE.UP ->{
                    if (mHeight - height > scrollY){
                        scrollBy(0,offsetY)
                    }else scrollTo(scrollX,mHeight - height)
                }
                SLIDE.RIGHT ->{
                    if (scrollX > 0){
                        scrollBy(offsetX,0)
                    }else scrollTo(0,scrollY)
                }
                SLIDE.LIFT ->{
                    if (mMaxWidth - width > scrollX){
                        scrollBy(offsetX,0)
                    }else scrollTo(mMaxWidth - width,scrollY)
                }
            }

        }
    }

    private fun edgeVerPull(event: MotionEvent){
        mEdgeEffect.onPull(event.y / height,if (mHeight - height <= scrollY) 1 - event.x / mMaxWidth else event.x / width)
        postInvalidateOnAnimation()
    }
    private fun edgeRelease(){
        mEdgeEffect.onRelease()
        postInvalidateOnAnimation()
    }

    override fun onDrawForeground(canvas: Canvas) {
        super.onDrawForeground(canvas)
        if (!mEdgeEffect.isFinished){
            when {
                mHeight - height <= scrollY -> {
                    canvas.save()
                    canvas.translate(-width.toFloat(), 0f)
                    canvas.rotate(180f,width.toFloat(),0f)
                    canvas.translate(0f, (-mHeight).toFloat())
                    mEdgeEffect.draw(canvas)
                    canvas.restore()
                }
                else -> mEdgeEffect.draw(canvas)
            }

            invalidate()
        }
    }

    private fun unfold(child: Item){
        if (child.sAnimY < child.sY){
            val step = 10
            if (child.sAnimY + step > child.sY){
                child.sAnimY = child.sY
            }else
                child.sAnimY += step

            unfoldParent(child)

            postDelayed({
                unfold(child)
            },5)

            invalidate()
        }
    }

    private fun unfoldParent(item: Item){
        val parent = item.parent
        if (parent != null){
            if (parent.unfold){
                val grandpa = parent.parent
                if (grandpa != null){
                    val sibling = grandpa.children!!
                    val index = sibling.indexOf(parent)
                    if (index + 1 < sibling.size){
                        for (i in index + 1 until sibling.size){
                            sibling[i].sAnimY = calLastChildAnimY(sibling[i - 1])
                            adjustChildren(sibling[i])
                        }
                    }
                }
                unfoldParent(parent)
            }
        }
    }

    private fun calLastChildAnimY(item: Item):Float{
        if (item.unfold){
            val ch = item.children
            if (!ch.isNullOrEmpty()){
                return calLastChildAnimY(ch[ch.size - 1])
            }
        }
        return item.sAnimY + item.sHeight + mVerGap
    }

    private fun adjustChildren(parent:Item){
        if (parent.unfold){
            val children = parent.children
            if (!children.isNullOrEmpty()){
                children[0].apply {
                    sAnimY = parent.sAnimY + parent.sHeight + mVerGap
                    adjustChildren(this)
                }
                for (i in 1 until children.size){
                    children[i].sAnimY = calLastChildAnimY(children[i - 1])
                    adjustChildren(children[i])
                }
            }
        }
    }

    private fun unfoldAnimation(item: Item){
        val ch = item.children
        if (!ch.isNullOrEmpty()){
            for (i in 0 until ch.size){
                val it = ch[i]
                it.sAnimY = item.sY + item.sHeight
                unfold(it)
                unfoldAnimation(it)
            }
        }
    }

    private fun startAnimation(item: Item){
        if (item.unfold){
            unfoldAnimation(item)
        }
    }

    private fun clickItem(item: Item?, x:Float, y:Float):Boolean{
        if (item == null)return false
        val realSX = item.sX - scrollX
        val realSY = item.sY - scrollY
        val bH = y >= realSY && y <= realSY + item.sHeight

        val checkX = (item.parent?.sX?:item.sX) - scrollX
        if (x>= checkX - mLogoSize && x<= checkX && bH){
            if (!item.children.isNullOrEmpty()){
                item.unfold = !item.unfold
                measureChild()
                startAnimation(item)
                return true
            }
        }else if(x >= realSX && x <= realSX + item.sWidth && bH){
            if (mCurItem != item){
                mCurItem?.click = false
                mCurItem = item
            }
            item.click = true
        }else if (x >= realSX - mBoxSize * 2f && x <= realSX + mBoxSize * 2f && bH){
            selectItem(item)
            return true
        }else if (item.unfold){
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
        return false
    }

    private fun selectItem(item: Item){
        item.selectedState = if (item.selectedState == SELECT_STATE.ALLSEL) SELECT_STATE.UNSEL else SELECT_STATE.ALLSEL
        if (hasSelect){
            if (mSingleSelection){
                if (mSelectedList.isNotEmpty()){
                    val c = mSelectedList.removeAt(0)
                    c.selectedState = SELECT_STATE.UNSEL
                    selectParentItem(c)
                }
                if (item.selectedState == SELECT_STATE.ALLSEL){
                    mSelectedList.add(item)
                }
            }else{
                if (item.selectedState == SELECT_STATE.ALLSEL){
                    mSelectedList.add(item)
                }else {
                    mSelectedList.remove(item)
                }
                selectChildItem(item)
            }
            selectParentItem(item)

            invalidate()
        }
    }
    private fun selectChildItem(item: Item){
        item.children?.forEach {
            if (it.selectedState != item.selectedState){
                it.selectedState = item.selectedState
                if (it.selectedState == SELECT_STATE.ALLSEL){
                    mSelectedList.add(it)
                }else if (it.selectedState == SELECT_STATE.UNSEL){
                    mSelectedList.remove(it)
                }
            }
            selectChildItem(it)
        }
    }
    private fun selectParentItem(item: Item){
        var p = item.parent
        while (p != null && !p.children.isNullOrEmpty()){
            when(p.children!!.count { if (mSingleSelection) (it.selectedState != SELECT_STATE.UNSEL) else it.selectedState == SELECT_STATE.ALLSEL }){
                0->{
                    p.selectedState = SELECT_STATE.UNSEL
                }
                p.children!!.size ->{
                    p.selectedState = SELECT_STATE.ALLSEL
                }else ->{
                p.selectedState = SELECT_STATE.HALFSEL
                }
            }
            p = p.parent
        }
    }

    class Item{
        var sX = 0f
        var sY = 0f
        var sWidth = 0
        var sHeight = 0
        var unfold:Boolean = false
        var click:Boolean = false

        var sAnimY = 0f
        var selectedState = SELECT_STATE.UNSEL

        var id:Int = 0
        var code:String = ""
        var name:String = ""
        var parent: Item? = null
        var children:MutableList<Item>? = null
        var data:Any? = null
        override fun toString(): String {
            return "Item(id=$id,sAnimY=$sAnimY,sX=$sX, sY=$sY, sWidth=$sWidth, sHeight=$sHeight, fold=$unfold, name='$name')"
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

    private enum class SLIDE{
        LIFT,RIGHT,UP,DOWN
    }
    enum class SELECT_STATE{
        UNSEL,HALFSEL,ALLSEL
    }


}