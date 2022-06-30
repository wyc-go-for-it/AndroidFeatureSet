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
import android.view.View
import com.wyc.logger.Logger
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import java.util.*
import kotlin.math.abs


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
    private var mFontSize:Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics)
    private var mDirection = 0
    private var mCurSpace = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics)

    constructor(context: Context):this(context, null)
    constructor(context: Context, attrs: AttributeSet?):this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int):super(context, attrs, defStyleAttr){
        init()
    }
    private fun init(){
        mPaint.color = Color.BLUE
        mPaint.isAntiAlias = true
        mPaint.textSize = mFontSize
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawContent(canvas)
    }

    private fun drawContent(canvas: Canvas){
        mContentList.filter { it.visibility }.forEach {
            val baseLineY = it.sHeight / 2 + (abs(mPaint.fontMetrics.ascent) - mPaint.fontMetrics.descent) / 2
            canvas.drawText(it.name,it.sX,it.sY + baseLineY,mPaint)
        }
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

        updatePosition(left,top,right,bottom)

        Utils.logInfo(mContentList.toTypedArray().contentToString())
    }

    private fun updatePosition(left: Int, top: Int, right: Int, bottom: Int){
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
        run out@{
            mContentList.forEachIndexed{index,item ->
                if (item.hasSel){
                    return index
                }
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
    }
}