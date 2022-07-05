package com.wyc.video

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import com.wyc.logger.Logger
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.sqrt


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.video
 * @ClassName:      ScrollSelectionView
 * @Description:    滚动选择
 * @Author:         wyc
 * @CreateDate:     2022/6/10 14:56
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/6/10 14:56
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class ScrollSelectionView:View {
    private val mContentList = mutableListOf<ScrollItem>()
    private val mPaint = Paint()
    private var mFontSize:Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics)
    private var mDirection = 1
    private var mCurSpace = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics)
    private var downX = 0f
    private var downY = 0f

    private var mChildOffset = 0f
    private var mScroller: OverScroller = OverScroller(context)
    private var mVelocityTracker: VelocityTracker? = null
    private var mMinimumVelocity = 0
    private var mMaximumVelocity = 0
    private var mActivePointerId = -1

    private var mSlideDirection = SLIDEDIRECTION.LIFT

    private var mListener:OnScrollFinish? = null

    constructor(context: Context):this(context, null)
    constructor(context: Context, attrs: AttributeSet?):this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int):super(context, attrs, defStyleAttr){
        init()

        initVelocity()
    }

    private fun initVelocity() {
        val configuration = ViewConfiguration.get(context)
        mMinimumVelocity = configuration.scaledMinimumFlingVelocity
        mMaximumVelocity = configuration.scaledMaximumFlingVelocity
    }

    private fun init(){
        mPaint.color = Color.WHITE
        mPaint.isAntiAlias = true
        mPaint.textSize = mFontSize
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.action){
            MotionEvent.ACTION_DOWN ->{
                initVelocityTrackerIfNotExists()
                downX = event.x
                downY = event.y

                mActivePointerId = event.getPointerId(0)
                mVelocityTracker!!.addMovement(event)

                return true
            }
            MotionEvent.ACTION_MOVE ->{
                mVelocityTracker!!.addMovement(event)

                val moveX = event.x
                val moveY = event.y

                val xDiff = abs(moveX - downX)
                val yDiff = abs(moveY - downY)

                if (mChildOffset == 0f){
                    mChildOffset = if (mDirection == 1) moveX else moveY
                } else{
                    val squareRoot = sqrt((xDiff * xDiff + yDiff * yDiff).toDouble())
                    val degreeX = asin(yDiff / squareRoot) * 180 / Math.PI
                    val degreeY = asin(xDiff / squareRoot) * 180 / Math.PI
                    if (mDirection == 1 && degreeX < 45){
                        if (moveX < downX){
                            mSlideDirection = SLIDEDIRECTION.LIFT
                            updatePosition(mChildOffset - moveX)
                        }else{
                            mSlideDirection = SLIDEDIRECTION.RIGHT
                            updatePosition(moveX - mChildOffset)
                        }
                    }
                    if (mDirection != 1 && degreeY < 45){
                        if (moveY < downY){
                            mSlideDirection = SLIDEDIRECTION.UP
                            updatePosition(mChildOffset - moveY)
                        }else{
                            mSlideDirection = SLIDEDIRECTION.DOWN
                            updatePosition(moveY - mChildOffset)
                        }
                    }
                    mChildOffset = 0f
                }
            }
            MotionEvent.ACTION_UP ->{
                mChildOffset = 0f

                mVelocityTracker?.apply {

                    computeCurrentVelocity(1000, mMaximumVelocity.toFloat())
                    val initialVelocity = if (mSlideDirection == SLIDEDIRECTION.LIFT || mSlideDirection == SLIDEDIRECTION.RIGHT)
                        getXVelocity(mActivePointerId).toInt() else getYVelocity(mActivePointerId).toInt()

                    mActivePointerId = -1

                    recycleVelocityTracker()

                    if ( abs(initialVelocity) > mMinimumVelocity) {
                        when(mSlideDirection){
                            SLIDEDIRECTION.LIFT->{
                                mScroller.fling(downX.toInt(), 0, initialVelocity / 8 ,0, 0  ,event.x.toInt(), 0,  0)
                            }
                            SLIDEDIRECTION.RIGHT->{
                                mScroller.startScroll(downX.toInt(),0,downX.toInt() - event.x.toInt(),0)
                            }
                            SLIDEDIRECTION.UP->{
                                mScroller.fling(0, downY.toInt(),  0,initialVelocity / 8, 0  ,0, 0,  event.y.toInt())
                            }
                            SLIDEDIRECTION.DOWN->{
                                mScroller.startScroll(0,downY.toInt(),0,downY.toInt() - event.y.toInt())
                            }
                        }
                        startScroll()
                    }else if (downX != event.x || downY != event.y) adjustPosition()
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun startScroll() {
        if (mScroller.computeScrollOffset()) {
            if (mScroller.isFinished){
                adjustPosition()
            }else{
                when(mSlideDirection){
                    SLIDEDIRECTION.LIFT,SLIDEDIRECTION.RIGHT->{
                        updatePosition(mScroller.currX.toFloat() - mScroller.finalX)
                        postDelayed({ startScroll() },40)
                    }
                    SLIDEDIRECTION.UP,SLIDEDIRECTION.DOWN->{
                        updatePosition(mScroller.currY.toFloat() - mScroller.finalY)
                        postDelayed({ startScroll() },40)
                    }
                }
            }
        }
    }

    private fun adjustPosition(){
        mContentList.find { it.hasSel }?.apply {
        val diff = when(mSlideDirection){
                SLIDEDIRECTION.LIFT->{
                    sX.toInt() + sWidth / 2 - (width shr 1)
                }
                SLIDEDIRECTION.RIGHT->{
                    -(sX.toInt() + sWidth / 2 - (width shr 1))
                }
                SLIDEDIRECTION.UP->{
                    sY.toInt() + sHeight / 2 - (height shr 1)
                }
                SLIDEDIRECTION.DOWN->{
                    -(sY.toInt() + sHeight / 2 - (height shr 1))
                }
            }
            updatePosition(diff.toFloat())

            mListener?.finish(this)
        }
    }

    fun setListener(scroll: OnScrollFinish){
        mListener = scroll
    }


    private fun initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
    }

    private fun recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker!!.recycle()
            mVelocityTracker = null
        }
    }

    enum class SLIDEDIRECTION{
        LIFT,RIGHT,UP,DOWN
    }

    private fun updatePosition(offset:Float){
        if (offset == 0f)return
        if (checkEdge(mSlideDirection,offset)){
            Logger.d("offset:%f",offset)
            when(mSlideDirection){
                SLIDEDIRECTION.LIFT->{
                    mContentList.forEach {
                        it.sX -= offset
                        selectItem(SLIDEDIRECTION.LIFT,it)
                    }
                }
                SLIDEDIRECTION.RIGHT->{
                    mContentList.forEach {
                        it.sX += offset
                        selectItem(SLIDEDIRECTION.RIGHT,it)
                    }
                }
                SLIDEDIRECTION.UP->{
                    mContentList.forEach {
                        it.sY -= offset
                        selectItem(SLIDEDIRECTION.UP,it)
                    }
                }
                SLIDEDIRECTION.DOWN->{
                    mContentList.forEach {
                        it.sY += offset
                        selectItem(SLIDEDIRECTION.DOWN,it)
                    }
                }
            }
            invalidate()
        }
    }

    private fun checkEdge(dire:SLIDEDIRECTION,offset:Float):Boolean{
        return when(dire){
            SLIDEDIRECTION.LIFT->{
                mContentList[mContentList.size - 1].sX + mContentList[mContentList.size - 1].sWidth > (width shr 1) + offset
            }
            SLIDEDIRECTION.RIGHT->{
                mContentList[0].sX < (width shr 1) - offset
            }
            SLIDEDIRECTION.UP->{
                mContentList[mContentList.size - 1].sY + mContentList[mContentList.size - 1].sHeight > (height shr 1) + offset
            }
            SLIDEDIRECTION.DOWN->{
                mContentList[0].sY < (height shr 1) - offset
            }
        }
    }

    private fun selectItem(slideDirection:SLIDEDIRECTION,item: ScrollItem){
        when(slideDirection){
            SLIDEDIRECTION.LIFT,SLIDEDIRECTION.RIGHT->{
                item.hasSel = item.sX + item.sWidth > (width shr 1) && (width shr 1) > item.sX
            }
            SLIDEDIRECTION.UP,SLIDEDIRECTION.DOWN->{
                item.hasSel = item.sY + item.sHeight > (height shr 1) && (height shr 1) > item.sY
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawContent(canvas)
    }

    private fun drawContent(canvas: Canvas){
        mContentList.filter { it.visibility }.forEach {
            val baseLineY = it.sHeight / 2 + (abs(mPaint.fontMetrics.ascent) - mPaint.fontMetrics.descent) / 2
            if (it.hasSel){
                mPaint.color = Color.RED
            }else mPaint.color = Color.WHITE
            canvas.drawText(it.name,it.sX,it.sY + baseLineY,mPaint)
        }
        mPaint.color = Color.GREEN
        if (mDirection == 1)
            canvas.drawCircle((width shr 1) - 4f, height - 8f,8f,mPaint)
        else
            canvas.drawCircle(8f, (height shr 1) - 4f,8f,mPaint)
        mPaint.color = Color.WHITE
    }

    private fun measureChild(){
        val bound = Rect()
        mContentList.forEach {
            mPaint.getTextBounds(it.name,0,it.name.length,bound)
            it.sWidth = bound.width()
            it.sHeight = bound.height()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSpec = MeasureSpec.getMode(widthMeasureSpec)
        val heightSpec = MeasureSpec.getMode(heightMeasureSpec)

        var realWidthMeasureSpec = widthMeasureSpec
        var realHeightMeasureSpec = heightMeasureSpec

        measureChild()

        if (widthSpec == MeasureSpec.AT_MOST || heightSpec == MeasureSpec.AT_MOST){
            var calc = 0

            if ((mDirection == 1 && widthSpec == MeasureSpec.AT_MOST) || (mDirection != 1 && heightSpec == MeasureSpec.AT_MOST)){
                val index = findSelectionIndex()
                mContentList.forEach { it.visibility = false }
                if (index == 0){
                    calc++
                    mContentList[index].visibility = true
                    if (mContentList.size > 1){
                        mContentList[index + 1].visibility = true
                    }
                }else if (index == 1){
                    calc += 2
                    mContentList[index - 1].visibility = true
                    mContentList[index].visibility = true
                    if (mContentList.size > 2){
                        mContentList[index + 1].visibility = true
                    }
                }else{
                    calc += 2
                    mContentList[index - 1].visibility = true
                    mContentList[index].visibility = true
                    if (mContentList.size > index + 1){
                        mContentList[index + 1].visibility = true
                    }
                }
            }


            if (mDirection == 1){//horizontal
                val realWidth = mContentList.fold(0){sum,item ->
                    if (item.visibility)sum + item.sWidth else sum + 0
                } + paddingLeft + paddingRight
                val maxHeight = mContentList.maxOf { if (it.visibility)it.sHeight else 0 } + paddingTop + paddingBottom
                if (realWidth > 0 && widthSpec == MeasureSpec.AT_MOST){
                    realWidthMeasureSpec = MeasureSpec.makeMeasureSpec((realWidth + calc * mCurSpace).toInt(),MeasureSpec.AT_MOST)
                }else{
                    showChild()
                }
                if (maxHeight > 0 && (heightSpec == MeasureSpec.AT_MOST || heightSpec == MeasureSpec.UNSPECIFIED)){
                    realHeightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight,MeasureSpec.AT_MOST)
                }
            }else{//vertical
                val realHeight = mContentList.sumOf{if (it.visibility)it.sHeight else 0} + paddingTop + paddingBottom
                val maxWidth = mContentList.maxOf { if (it.visibility)it.sWidth else 0} + paddingLeft + paddingRight
                if (realHeight > 0 && heightSpec == MeasureSpec.AT_MOST){
                    realHeightMeasureSpec = MeasureSpec.makeMeasureSpec((realHeight + calc * mCurSpace).toInt(),MeasureSpec.AT_MOST)
                }else{
                    showChild()
                }
                if (maxWidth > 0 && (widthSpec == MeasureSpec.AT_MOST || widthSpec == MeasureSpec.UNSPECIFIED)){
                    realWidthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth,MeasureSpec.AT_MOST)
                }
            }
        }
        super.onMeasure(realWidthMeasureSpec, realHeightMeasureSpec)
    }

    private fun showChild(){

        val width = measuredWidth
        val height = measuredHeight

        Utils.logInfo("left:$left,top:$top,right:$right,bottom:$bottom,width:$width,height:$height")

        val index = findSelectionIndex()

        Utils.logInfo("index:$index")

        var item:ScrollItem
        var rSize = 0
        var sizeDiff = 0
        if (index == 0){
            item = mContentList[index]
            item.visibility = true

            if (mContentList.size > 1){
                if (mDirection == 1){
                    sizeDiff = width - item.sWidth
                    if (sizeDiff > 0){
                        for (i in index + 1 until mContentList.size){
                            item = mContentList[i]

                            rSize = item.sWidth

                            sizeDiff -= rSize
                            if (sizeDiff > 0){
                                item.visibility = true
                            }else break
                        }
                    }
                }else{
                    sizeDiff = height - item.sHeight
                    if (sizeDiff > 0){
                        for (i in index + 1 until mContentList.size){
                            item = mContentList[i]

                            rSize = item.sHeight

                            sizeDiff -= rSize
                            if (sizeDiff > 0){
                                item.visibility = true
                            }
                        }
                    }
                }
            }

        }else if (index == 1){
            item = mContentList[index]
            item.visibility = true

            if (mDirection == 1){
                sizeDiff = width - item.sWidth

                if (sizeDiff > 0){
                    item = mContentList[index - 1]

                    rSize = item.sWidth

                    sizeDiff -= rSize
                    if (sizeDiff > 0){
                        item.visibility = true

                        for (i in index + 1 until mContentList.size){
                            item = mContentList[i]

                            rSize = item.sWidth

                            sizeDiff -= rSize
                            if (sizeDiff > 0){
                                item.visibility = true
                            }else break
                        }
                    }
                }

            }else{
                sizeDiff = height - item.sHeight
                if (sizeDiff > 0){
                    item = mContentList[index - 1]

                    rSize = item.sHeight

                    sizeDiff -= rSize
                    if (sizeDiff > 0){
                        item.visibility = true

                        for (i in index + 1 until mContentList.size){
                            item = mContentList[i]

                            rSize = item.sHeight

                            sizeDiff -= rSize
                            if (sizeDiff > 0){
                                item.visibility = true
                            }else break
                        }
                    }
                }
            }
        }else{
            item = mContentList[index]
            item.visibility = true

            var preIndex = index
            var nextIndex = index
            if (mDirection == 1){
                sizeDiff = width - item.sWidth
                if (sizeDiff > 0){

                    while (preIndex-- > 0 ){
                        item = mContentList[preIndex]

                        rSize = item.sWidth

                        sizeDiff -= rSize
                        if (sizeDiff > 0){
                            item.visibility = true
                        }else break
                    }

                    while (nextIndex ++ < mContentList.size - 1){
                        item = mContentList[nextIndex]

                        rSize = item.sWidth

                        sizeDiff -= rSize
                        if (sizeDiff > 0){
                            item.visibility = true
                        }else break
                    }
                }
            }else{
                sizeDiff = height - item.sHeight
                if (sizeDiff > 0){

                    while (preIndex-- > 0){
                        item = mContentList[preIndex]

                        rSize = item.sHeight

                        sizeDiff -= rSize
                        if (sizeDiff > 0){
                            item.visibility = true
                        }else break
                    }

                    while (nextIndex ++ < mContentList.size - 1){
                        item = mContentList[nextIndex]

                        rSize = item.sHeight

                        sizeDiff -= rSize
                        if (sizeDiff > 0){
                            item.visibility = true
                        }else break
                    }
                }
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        initPosition(left,top,right,bottom)

        Utils.logInfo(mContentList.toTypedArray().contentToString())
    }

    private fun initPosition(left: Int, top: Int, right: Int, bottom: Int){
        val index = findSelectionIndex()

        val width = right - left
        val height = bottom  -top

        val selItem = mContentList[index]
        if (!selItem.hasSel)selItem.hasSel = true

        selItem.sX = ((width - selItem.sWidth) shr 1).toFloat()
        selItem.sY = ((height - selItem.sHeight) shr 1).toFloat()

        var preIndex = index
        var nextIndex = index

        var sumHor = selItem.sX
        var item:ScrollItem
        var preSize: Int

        if (mDirection == 1){
            while (preIndex-- > 0){
                item = mContentList[preIndex]
                if (item.visibility){
                    item.sY = ((height - item.sHeight) shr 1).toFloat()

                    sumHor -= mCurSpace
                    item.sX = sumHor - item.sWidth
                    sumHor = item.sX
                }
            }

            sumHor = selItem.sX
            preSize = selItem.sWidth
            while (nextIndex ++ < mContentList.size - 1){
                item = mContentList[nextIndex]
                if (item.visibility){
                    item.sY = ((height - item.sHeight) shr 1).toFloat()

                    sumHor += mCurSpace
                    item.sX = sumHor + preSize
                    sumHor = item.sX

                    preSize = item.sWidth
                }
            }
        }else{
            sumHor = selItem.sY
            while (preIndex-- > 0){
                item = mContentList[preIndex]
                if (item.visibility){
                    item.sX = ((width - item.sWidth) shr 1).toFloat()

                    sumHor -= mCurSpace
                    item.sY = sumHor - item.sHeight
                    sumHor = item.sY

                }
            }

            sumHor = selItem.sY
            preSize = selItem.sHeight
            while (nextIndex ++ < mContentList.size - 1){
                item = mContentList[nextIndex]
                if (item.visibility){
                    item.sX = ((width - item.sWidth) shr 1).toFloat()

                    sumHor += mCurSpace
                    item.sY = sumHor + preSize
                    sumHor = item.sY

                    preSize = item.sHeight
                }
            }
        }
    }

    private fun findSelectionIndex():Int{
        mContentList.forEachIndexed{index,item ->
            if (item.hasSel){
                return index
            }
        }
        return mContentList.size / 2
    }

    fun addAll(items: Collection<ScrollItem>){
        mContentList.addAll(items)
    }
    fun addItem(item: ScrollItem){
        mContentList.add(item)
    }

    data class ScrollItem(val id:Int, val name:String, var hasSel:Boolean = false){
        var visibility:Boolean = false
        var sX = 0f
        var sY = 0f
        var sWidth = 0
        var sHeight = 0

        override fun toString(): String {
            return "ScrollItem(id=$id, name='$name', hasSel=$hasSel, visibility=$visibility, sX=$sX, sY=$sY, sWidth=$sWidth, sHeight=$sHeight)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ScrollItem

            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int {
            return id
        }
    }

    interface OnScrollFinish{
        fun finish(item: ScrollItem)
    }

}