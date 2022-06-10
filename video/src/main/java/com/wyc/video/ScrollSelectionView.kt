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
    private val mCurItemList = mutableListOf<ScrollItem>()
    private val mPaint = Paint()
    private var mFontSize:Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics)
    private var mDirection = 1
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

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSpec = MeasureSpec.getMode(widthMeasureSpec)
        val heightSpec = MeasureSpec.getMode(heightMeasureSpec)

        var realWidthMeasureSpec = widthMeasureSpec
        var realHeightMeasureSpec = heightMeasureSpec

        mCurItemList.clear()

        val index = findSelectionIndex()
        if (index != -1 && (mDirection == 1 && widthSpec == MeasureSpec.AT_MOST || mDirection == 0 && heightSpec == MeasureSpec.AT_MOST)){
            if (index == 0){
                mCurItemList.add(mContentList[index])
                if (mContentList.size > 1){
                    mCurItemList.add(mContentList[index + 1])
                }
            }else if (index == 1){
                mCurItemList.add(mContentList[index - 1])
                mCurItemList.add(mContentList[index])
                if (mContentList.size > 2){
                    mCurItemList.add(mContentList[index + 1])
                }
            }else{
                mCurItemList.add(mContentList[index - 1])
                mCurItemList.add(mContentList[index])
                if (mContentList.size > index + 1){
                    mCurItemList.add(mContentList[index + 1])
                }
            }
        }

        if(mCurItemList.isNotEmpty()){

            val bound = Rect()
            if (mDirection == 1){//horizontal
                val realWidth = mCurItemList.fold(0){sum,item -> mPaint.getTextBounds(item.name,0,item.name.length,bound);sum + bound.width()}
                val maxHeight = mCurItemList.maxOf { mPaint.getTextBounds(it.name,0,it.name.length,bound);bound.height()}
                if (widthSpec == MeasureSpec.AT_MOST){
                    if (realWidth > 0){
                        realWidthMeasureSpec = MeasureSpec.makeMeasureSpec((realWidth + (mCurItemList.size - 1) * mCurSpace).toInt(),MeasureSpec.EXACTLY)
                    }
                    if (maxHeight > 0){
                        realHeightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight,MeasureSpec.EXACTLY)
                    }
                }
            }else{//vertical
                if (heightSpec == MeasureSpec.AT_MOST){
                    val realHeight = mCurItemList.sumOf{mPaint.getTextBounds(it.name,0,it.name.length,bound);bound.height()}
                    val maxWidth = mCurItemList.maxOf { mPaint.getTextBounds(it.name,0,it.name.length,bound);bound.width()}
                    if (maxWidth > 0){
                        realWidthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth,MeasureSpec.EXACTLY)
                    }
                    if (realHeight > 0){
                        realHeightMeasureSpec = MeasureSpec.makeMeasureSpec((realHeight + (mCurItemList.size - 1) * mCurSpace).toInt(),MeasureSpec.EXACTLY)
                    }
                }
            }

        }

        super.onMeasure(realWidthMeasureSpec, realHeightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (mCurItemList.isEmpty()){
            val index = findSelectionIndex()

            Utils.logInfo("index:$index")

            if (index == -1)return


            val width = right - left
            val height = bottom  -top

            val bound = Rect()
            var item:ScrollItem
            var rSize = 0
            var sizeDiff = 0
            if (index == 0){
                item = mContentList[index]
                mCurItemList.add(item)

                if (mContentList.size > 1){
                    mPaint.getTextBounds(item.name,0,item.name.length,bound)
                    if (mDirection == 1){
                        sizeDiff = width - bound.width()
                        if (sizeDiff > 0){
                            for (i in index + 1 until mContentList.size){
                                item = mContentList[i]
                                mPaint.getTextBounds(item.name,0,item.name.length,bound)
                                rSize = bound.width()

                                sizeDiff -= rSize
                                if (sizeDiff > 0){
                                    mCurItemList.add(item)
                                }else break
                            }
                        }
                    }else{
                        sizeDiff = height - bound.height()
                        if (sizeDiff > 0){
                            for (i in index + 1 until mContentList.size){
                                item = mContentList[i]
                                mPaint.getTextBounds(item.name,0,item.name.length,bound)
                                rSize = bound.height()

                                sizeDiff -= rSize
                                if (sizeDiff > 0){
                                    mCurItemList.add(item)
                                }
                            }
                        }
                    }
                }

            }else if (index == 1){
                item = mContentList[index]
                mCurItemList.add(item)

                mPaint.getTextBounds(item.name,0,item.name.length,bound)

                if (mDirection == 1){
                    sizeDiff = width - bound.width()

                    if (sizeDiff > 0){
                        item = mContentList[index - 1]
                        mPaint.getTextBounds(item.name,0,item.name.length,bound)
                        rSize = bound.width()

                        sizeDiff -= rSize
                        if (sizeDiff > 0){
                            mCurItemList.add(item)

                            for (i in index + 1 until mContentList.size){
                                item = mContentList[i]
                                mPaint.getTextBounds(item.name,0,item.name.length,bound)
                                rSize = bound.width()

                                sizeDiff -= rSize
                                if (sizeDiff > 0){
                                    mCurItemList.add(item)
                                }else break
                            }
                        }
                    }

                }else{
                    sizeDiff = height - bound.height()
                    if (sizeDiff > 0){
                        item = mContentList[index - 1]
                        mPaint.getTextBounds(item.name,0,item.name.length,bound)
                        rSize = bound.height()

                        sizeDiff -= rSize
                        if (sizeDiff > 0){
                            mCurItemList.add(item)

                            for (i in index + 1 until mContentList.size){
                                item = mContentList[i]
                                mPaint.getTextBounds(item.name,0,item.name.length,bound)
                                rSize = bound.height()

                                sizeDiff -= rSize
                                if (sizeDiff > 0){
                                    mCurItemList.add(item)
                                }else break
                            }
                        }
                    }
                }
            }else{
                item = mContentList[index]
                mCurItemList.add(item)

                mPaint.getTextBounds(item.name,0,item.name.length,bound)

                var preIndex = 0
                var nextIndex = 0
                if (mDirection == 1){
                    sizeDiff = width - bound.width()
                    if (sizeDiff > 0){

                        preIndex = index + 1
                        nextIndex = index + 2

                        while (preIndex-- > 0 || nextIndex ++ > 0){
                            if (preIndex > 0){
                                item = mContentList[preIndex]
                                mPaint.getTextBounds(item.name,0,item.name.length,bound)
                                rSize = bound.width()

                                sizeDiff -= rSize
                                if (sizeDiff > 0){
                                    mCurItemList.add(preIndex,item)
                                }else break
                            }
                           if (nextIndex < mContentList.size){
                               item = mContentList[nextIndex]
                               mPaint.getTextBounds(item.name,0,item.name.length,bound)
                               rSize = bound.width()

                               sizeDiff -= rSize
                               if (sizeDiff > 0){
                                   mCurItemList.add(nextIndex,item)
                               }else break
                           }
                        }
                    }
                }else{
                    sizeDiff = height - bound.height()
                    if (sizeDiff > 0){

                        preIndex = index + 1
                        nextIndex = index + 2

                        while (preIndex-- > 0 || nextIndex ++ > 0){
                            if (preIndex > 0){
                                item = mContentList[preIndex]
                                mPaint.getTextBounds(item.name,0,item.name.length,bound)
                                rSize = bound.height()

                                sizeDiff -= rSize
                                if (sizeDiff > 0){
                                    mCurItemList.add(preIndex,item)
                                }else break
                            }
                            if (nextIndex < mContentList.size){
                                item = mContentList[nextIndex]
                                mPaint.getTextBounds(item.name,0,item.name.length,bound)
                                rSize = bound.height()

                                sizeDiff -= rSize
                                if (sizeDiff > 0){
                                    mCurItemList.add(nextIndex,item)
                                }else break
                            }
                        }
                    }
                }
            }
        }

        Utils.logInfo(mCurItemList.toTypedArray().contentToString())
    }

    private fun findSelectionIndex():Int{
        run out@{
            mContentList.forEachIndexed{index,item ->
                if (item.hasSel){
                    return index
                }
            }
        }
        return -1
    }

    fun addAll(items: Collection<ScrollItem>){
        mContentList.addAll(items)
    }
    fun addItem(item: ScrollItem){
        mContentList.add(item)
    }

    data class ScrollItem(private val id:Int,val name:String,val hasSel:Boolean = false)
}