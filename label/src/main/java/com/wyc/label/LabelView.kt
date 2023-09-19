package com.wyc.label

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Base64
import android.view.*
import com.wyc.label.LabelPrintSetting.Companion.getSetting
import com.wyc.label.LabelTemplate.height2Pixel
import com.wyc.label.LabelTemplate.width2Pixel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan


/**
 *
 * @ProjectName:    AndroidClient
 * @Package:        com.wyc.cloudapp.design
 * @ClassName:      格式设计界面
 * @Description:    作用描述
 * @Author:         wyc
 * @CreateDate:     2022/3/16 15:01
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/3/16 15:01
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class LabelView: View {
    private val mOffsetX = context.resources.getDimensionPixelOffset(R.dimen.com_wyc_label_size_14)
    private val mOffsetY = context.resources.getDimensionPixelOffset(R.dimen.com_wyc_label_size_14)

    private var realWidth = 1
    private var realHeight = 1

    private val contentList = mutableListOf<ItemBase>()
    private val mPaint = Paint()

    private val mTextBound = Rect()
    private var mBackground:Bitmap?  = null

    private var mCurItem: ItemBase? = null

    private var mLastX = 0f
    private var mLastY = 0f
    private var mMoveX = 0f
    private var mMoveY = 0f

    private var mLabelTemplate:LabelTemplate = LabelTemplate()

    private val mLabelSize = LabelTemplate.getDefaultSize()

    private var mItemChange = false

    private var mRotate = 0
    /**
     * 当前模式 true 预览 false 编辑
     * */
    private var mModel = false


    private var count = 0
    private var firClick: Long = 0

    private val mMaxAttrIndex = 5
    private val mItemAttrList:Array<ActionObject?> = arrayOfNulls(mMaxAttrIndex)
    private var mCurAttrIndex = 0
        set(value) {
            field = if (value >= mMaxAttrIndex){
                0
            }else value
        }
        get() {
            return if ( field < 0) mMaxAttrIndex - 1 else field
        }


    constructor(context: Context):this(context, null)
    constructor(context: Context, attrs: AttributeSet?):this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int):super(context, attrs, defStyleAttr){
        initPaint()
    }

    fun updateLabelTemplate(labelTemplate: LabelTemplate){
        mLabelTemplate = labelTemplate
        mItemChange = true
        adjustLabelSize(labelTemplate.width,labelTemplate.height)
        contentList.clear()
        contentList.addAll(labelTemplate.printItem)
        generateBackground()
        requestLayout()
        invalidate()
    }

    fun getLabelTemplate():LabelTemplate{
        return mLabelTemplate
    }

    private fun initPaint(){
        mPaint.isAntiAlias = true
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }
    private fun generateBackground(){
        CoroutineScope(Dispatchers.IO).launch {
            val bmp = decodeImage(mLabelTemplate.backgroundImg)
            mBackground = bmp
            postInvalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!mModel && checkTouchRegion(event.x,event.y)){
            when(event.action){
                MotionEvent.ACTION_DOWN->{
                    mCurItem?.checkDeleteClick(event.x,event.y)
                    if (activeItem(event.x,event.y)){
                        addActiveItem(mCurItem)

                        mLastX = event.x
                        mLastY = event.y
                        return true
                    }
                };
                MotionEvent.ACTION_MOVE->{
                    mCurItem?.let {
                        mMoveX = event.x - mLastX
                        mMoveY = event.y - mLastY

                        mLastX = event.x
                        mLastY = event.y

                        it.moveCurItem(realWidth,realHeight,event.x,event.y,mMoveX,mMoveY)
                        run loop@{
                            contentList.forEach { c->
                                if (c != it){
                                    if (it.isAlign(c))return@loop
                                }
                            }
                        }
                        invalidate()
                    }
                };
                MotionEvent.ACTION_UP->{
                    if (!deleteItem(event.x,event.y) && !checkDoubleClick()){
                        checkActiveItemAttr(mCurItem)
                        releaseCurItem()
                    }
                }
            }
            return true
        }else releaseCurItem()

        return super.onTouchEvent(event)
    }

    private fun checkDoubleClick():Boolean{
        mCurItem?.apply {
            count++
            val interval = 200
            if (1 == count) {
                firClick = System.currentTimeMillis()
            } else if (2 == count) {
                val secClick = System.currentTimeMillis()
                if (secClick - firClick < interval) {
                    popMenu(this@LabelView)
                    count = 0
                    firClick = 0
                    return true
                } else {
                    firClick = secClick
                    count = 1
                }
            }
        }
        return false
    }

    private fun checkTouchRegion(clickX:Float,clickY:Float):Boolean{
        return clickX - mOffsetX in 0f..realWidth.toFloat() && clickY - mOffsetY in 0f..realHeight.toFloat()
    }
    private fun deleteItem(clickX:Float,clickY:Float):Boolean{
        mCurItem?.let {
            it.checkDeleteClick(clickX,clickY)
            if (it.hasDelete()){
                contentList.remove(it)

                addDelAction(it)

                mCurItem = null
                invalidate()
                return true
            }
        }
        return false
    }

    private fun activeItem(clickX:Float, clickY:Float):Boolean{
        for (i in contentList.size -1 downTo 0){
            val it = contentList[i]
            if (it.hasSelect(clickX,clickY,mOffsetX,mOffsetY)){
                if (mCurItem != it){
                    mCurItem?.apply {
                        disableItem()
                    }
                    contentList[i] = contentList[contentList.size -1]
                    contentList[contentList.size -1] = it
                    mCurItem = it
                }
                return true
            }
        }
        if (mCurItem != null){
            mCurItem!!.disableItem()
            mCurItem = null
            invalidate()
        }
        return false
    }

    private fun releaseCurItem(){
        mCurItem?.let {
            it.releaseItem()
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthSpec = MeasureSpec.getMode(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightSpec = MeasureSpec.getMode(heightMeasureSpec)

        var resultWidthSize = 0
        var resultHeightSize = 0

        when(widthSpec){
            MeasureSpec.EXACTLY -> {
                resultWidthSize = widthSize
            }
            MeasureSpec.AT_MOST -> {
                resultWidthSize = selectMeasureWidth()
            }
            MeasureSpec.UNSPECIFIED -> {
                resultWidthSize = selectMeasureWidth()
            }
        }

        when(heightSpec){
            MeasureSpec.EXACTLY,MeasureSpec.AT_MOST,MeasureSpec.UNSPECIFIED -> {
                resultHeightSize = selectMeasureHeight(widthSpec,widthSize)
            }
        }
        super.onMeasure(MeasureSpec.makeMeasureSpec(resultWidthSize,widthSpec), MeasureSpec.makeMeasureSpec(resultHeightSize ,heightSpec))

        calculateContentSize()
    }
    /**
     * 已宽度为参照进行缩放,如果宽度是具体尺寸，则需要先计算缩放后的高度。否则可能缩放后高度比实际高度要大
     * */
    private fun selectMeasureHeight(widthSpec:Int,widthSize:Int):Int{
        var h = min(height2Pixel(mLabelTemplate), mLabelTemplate.realHeight)
        if (widthSpec == MeasureSpec.EXACTLY){
            val rightMargin = context.resources.getDimensionPixelOffset(R.dimen.com_wyc_label_size_5)
            val hh = ((widthSize  - mOffsetX - rightMargin) / width2Pixel(mLabelTemplate).toFloat() * height2Pixel(
                mLabelTemplate
            )).toInt()
            h = max(hh,h)
        }
        return h + mOffsetY + 8
    }
    private fun selectMeasureWidth():Int{
        return max(width2Pixel(mLabelTemplate), mLabelTemplate.realWidth) + mOffsetX
    }

    private fun measureItem(){
        contentList.forEach {
            it.measure(realWidth, realHeight)
        }
    }

    private fun calculateContentSize(){
        val margin = context.resources.getDimensionPixelOffset(R.dimen.com_wyc_label_size_5)
        realWidth = measuredWidth  - mOffsetX - margin
        realHeight = ((realWidth.toFloat() /  width2Pixel(mLabelTemplate).toFloat()) * height2Pixel(
            mLabelTemplate
        )).toInt()
        measureItem()
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (changed || mItemChange)
            layoutItem()
    }
    private fun layoutItem(){
        if (mItemChange){
            mItemChange = false
            val tWidth = mLabelTemplate.realWidth
            val tHeight = mLabelTemplate.realHeight
            if (tWidth != 0 && tHeight != 0){
                val scaleX = realWidth.toFloat() / tWidth
                val scaleY = realHeight.toFloat()/ tHeight
                contentList.forEach {
                    it.transform(scaleX,scaleY)
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.apply {
            mPaint.reset()
            drawRule(this)
            drawBackground(this)
            drawContent(this)
        }
    }

    private fun drawRule(canvas: Canvas){
        val physicsWidth = mLabelTemplate.width
        val physicsHeight = mLabelTemplate.height
        var perGap = realWidth / physicsWidth.toFloat()
        var coordinate: Float
        var num:String
        val lineHeight = context.resources.getDimension(R.dimen.com_wyc_label_size_4)

        mPaint.textSize = context.resources.getDimension(R.dimen.com_wyc_label_font_size_6)
        mPaint.color = Color.GRAY
        mPaint.style = Paint.Style.FILL
        mPaint.strokeWidth = 0f
        for (i in 0..physicsWidth){
            num = i.toString()
            coordinate = if (i == physicsWidth){
                i * perGap + mOffsetX - mPaint.strokeWidth
            }else
                i * perGap + mOffsetX

            if (i % 10 == 0){
                canvas.drawLine(coordinate,
                    mOffsetY.toFloat(),coordinate,mOffsetY - lineHeight * 2,mPaint)
                canvas.drawText(num,coordinate - mPaint.measureText(num) / 2,mOffsetY - lineHeight * 2,mPaint)
            }else
                canvas.drawLine(coordinate,
                    mOffsetY.toFloat(),coordinate,mOffsetY - lineHeight,mPaint)
        }
        canvas.drawLine(mOffsetX.toFloat(), mOffsetY.toFloat(),
            (realWidth + mOffsetX).toFloat(), mOffsetY.toFloat(),mPaint)

        perGap = realHeight / physicsHeight.toFloat()
        for (i in 0..physicsHeight){
            num = i.toString()
            coordinate = if (i == physicsHeight){
                i * perGap + mOffsetY - mPaint.strokeWidth
            }else
                i * perGap + mOffsetY

            if (i % 10 == 0) {
                canvas.drawLine(mOffsetY.toFloat(), coordinate, mOffsetY - lineHeight * 2, coordinate, mPaint)

                mPaint.getTextBounds(num,0,num.length,mTextBound)
                canvas.save()
                canvas.rotate(-90f,mOffsetY - lineHeight * 2 ,coordinate)
                canvas.drawText(num,mOffsetY - lineHeight * 2 - mPaint.measureText(num) / 2,coordinate,mPaint)
                canvas.restore()
            }else
                canvas.drawLine(mOffsetY.toFloat(),coordinate,mOffsetY - lineHeight,coordinate,mPaint)
        }
        canvas.drawLine(mOffsetX.toFloat(), mOffsetY.toFloat(),
            mOffsetX.toFloat(), (realHeight + mOffsetY).toFloat(),mPaint)
    }
    private fun drawBackground(canvas: Canvas){
        //画阴影
        val color = mPaint.color
        mPaint.color = Color.WHITE
        mPaint.setShadowLayer(15f,0f,8f,Color.GRAY)
        canvas.drawRect(
            mOffsetX.toFloat(), mOffsetY.toFloat(),
            (realWidth + mOffsetX).toFloat(), (realHeight + mOffsetY).toFloat(),mPaint)
        mPaint.color = color
        mPaint.setShadowLayer(0f,0f,0f,Color.GRAY)

        mBackground?.apply {
            val matrix = Matrix()
            matrix.setScale(realWidth.toFloat() / width.toFloat(),realHeight.toFloat() / height.toFloat())
            canvas.save()
            canvas.translate(mOffsetX.toFloat(),mOffsetY.toFloat())
            canvas.drawBitmap(this,matrix,null)
            canvas.restore()
        }
    }
    private fun drawContent(canvas: Canvas){
        if (contentList.isNotEmpty()){
            canvas.clipRect(mOffsetX,mOffsetY,realWidth + mOffsetX,realHeight + mOffsetY)
            contentList.forEach {
                it.draw(mOffsetX.toFloat(),mOffsetY.toFloat(),canvas,mPaint)
            }
        }
    }

    /**
     * @param bg 背景位图。如果为null则清除当前背景
     * */
    internal fun setLabelBackground(bg:Bitmap?){
        if (bg == null){
            if (mBackground != null){
                mBackground!!.recycle()
                mBackground = null

                mLabelTemplate.backgroundImg = ""
            }
        }else{
            if (mBackground != null){
                mBackground!!.recycle()
            }
            mBackground = bg
            mLabelTemplate.backgroundImg = encodeImage(bg)
        }
        postInvalidate()
    }

    private fun encodeImage(bitmap: Bitmap):String {
        ByteArrayOutputStream().use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it)
            val bytes: ByteArray = it.toByteArray()
            return Base64.encodeToString(bytes, Base64.DEFAULT)
        }
    }
    private fun decodeImage(bmMsg: String):Bitmap? {
        val input = Base64.decode(bmMsg, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(input, 0, input.size)
    }

    private fun addItem(item: ItemBase){
        if (mCurItem != null){
            mCurItem!!.disableItem()
        }
        item.activeItem()
        item.measure(realWidth, realHeight)
        mCurItem = item

        item.left = (width - item.width - ItemBase.ACTION_RADIUS * 4).toInt()
        item.top = (height - item.height - ItemBase.ACTION_RADIUS * 4).toInt()

        contentList.add(item)

        addNewAction(item)

        postInvalidate()
    }
    private fun addNewAction(item: ItemBase){
        mItemAttrList[mCurAttrIndex++] = ActionObject(item,ActionObject.Action.ADD,null)
    }

    private fun addDelAction(item: ItemBase){
        mItemAttrList[mCurAttrIndex++] = ActionObject(item,ActionObject.Action.DEL,null)
    }

    internal fun addModifyAction(item: ItemBase, vararg vars:ActionObject.FieldObject){

        val fieldList:MutableList<ActionObject.FieldObject> = mutableListOf()

        vars.forEach {
            fieldList.add(it)
        }
        mItemAttrList[mCurAttrIndex++] = ActionObject(item,ActionObject.Action.MOD,fieldList)
    }
    private fun addModifyAction(item: ItemBase, fieldList:MutableList<ActionObject.FieldObject>){
        mItemAttrList[mCurAttrIndex++] = ActionObject(item,ActionObject.Action.MOD,fieldList)
    }

    private fun addActiveItem(item: ItemBase?){
        item?.apply {
            var index = mCurAttrIndex - 1
            if (index < 0)index = mMaxAttrIndex - 1
            val oldActionObject = mItemAttrList[index]

            if (!(oldActionObject != null && oldActionObject.action == ActionObject.Action.ACTIVE && oldActionObject.actionObj == this)){
                val w = ActionObject.FieldObject(getWidthField(),width,width)
                val h = ActionObject.FieldObject(getHeightField(),height,height)
                val l = ActionObject.FieldObject(getLeftField(),left,left)
                val r = ActionObject.FieldObject(getTopField(),top,top)

                mItemAttrList[mCurAttrIndex++] = if (this is TextItem){
                    ActionObject(this,ActionObject.Action.ACTIVE, mutableListOf(w,h,l,r,
                        ActionObject.FieldObject(getFontSizeField(),mFontSize,mFontSize)))
                }else ActionObject(this,ActionObject.Action.ACTIVE, mutableListOf(w,h,l,r))
            }
        }
    }
    private fun checkActiveItemAttr(curItem: ItemBase?){
        curItem?.apply {
            var index = mCurAttrIndex - 1
            if (index < 0)index = mMaxAttrIndex - 1
            val actionObject = mItemAttrList[index]
            if (actionObject != null){
                if (actionObject.actionObj == this){
                    actionObject.fieldList?.let {list->

                        val fieldList:MutableList<ActionObject.FieldObject> = mutableListOf()

                        list.forEach {
                            when(it.field?.name){
                                "width" ->{
                                    if (it.oldValue != this.width){
                                        fieldList.add(ActionObject.FieldObject(getWidthField(),it.oldValue,this.width))
                                    }
                                }
                                "height" ->{
                                    if (it.oldValue != this.height){
                                        fieldList.add(ActionObject.FieldObject(getHeightField(),it.oldValue,this.height))
                                    }
                                }
                                "left" ->{
                                    if (it.oldValue != this.left){
                                        fieldList.add(ActionObject.FieldObject(getLeftField(),it.oldValue,this.left))
                                    }
                                }
                                "top" ->{
                                    if (it.oldValue != this.top){
                                        fieldList.add(ActionObject.FieldObject(getTopField(),it.oldValue,this.top))
                                    }
                                }
                                "mFontSize" ->{
                                    if (this is TextItem && it.oldValue != this.mFontSize){
                                        fieldList.add(ActionObject.FieldObject(getFontSizeField(),it.oldValue,this.mFontSize))
                                    }
                                }
                            }
                        }
                        if (fieldList.isNotEmpty())addModifyAction(this,fieldList)
                    }
                }
            }
        }
    }

    internal fun restAction(){
        val actionObject = mItemAttrList[--mCurAttrIndex]
        if (actionObject != null){
            mItemAttrList[mCurAttrIndex] = null
            val item = actionObject.actionObj
            when(actionObject.action){
                ActionObject.Action.ADD ->{
                    contentList.remove(item)
                }
                ActionObject.Action.DEL ->{
                    contentList.add(0,item)
                    swapCurItem(item)
                }
                ActionObject.Action.MOD->{
                    swapCurItem(item)
                    actionObject.fieldList?.forEach {
                        it.field?.apply {
                            isAccessible = true
                            set(item,it.oldValue)
                            item.resetAttr(name)
                        }
                    }
                }
                ActionObject.Action.ACTIVE ->{
                    var i = mCurAttrIndex - 1
                    if (i < 0) i = mMaxAttrIndex - 1
                    val ac = mItemAttrList[i]
                    ac?.apply {
                        swapCurItem(ac.actionObj)
                    }
                }
            }
            postInvalidate()
        }
    }

    private fun swapCurItem(item: ItemBase?){
        if (mCurItem != null){
            mCurItem!!.disableItem()
        }
        item?.activeItem()
        mCurItem = item
        postInvalidate()
    }

    internal fun getRealWidth():Int{
        return realWidth
    }
    internal fun getRealHeight():Int{
        return realHeight
    }

    internal fun addTextItem(){
        addItem(TextItem())
    }
    internal fun addLineItem(){
        addItem(LineItem())
    }

    internal fun addRectItem(){
        addItem(RectItem())
    }

    internal fun addCircleItem(){
        addItem(CircleItem())
    }

    internal fun addBarcodeItem(){
        addItem(BarcodeItem())
    }

    internal fun addQRCodeItem(){
        val item = QRCodeItem()
        addItem(item)
    }

    internal fun addDateItem(){
        addItem(DateItem())
    }

    internal fun addDataItem(){
        val selectDialog = SelectDialog(context)
        DataItem.FIELD.values().forEach {
            val item = SelectDialog.Item(it.field,it.description)
            selectDialog.addContent(item)
        }
        selectDialog.setSelectListener(object :SelectDialog.OnSelect{
            override fun select(content: SelectDialog.Item) {
                selectDialog.dismiss()
                if (DataItem.FIELD.Barcode.field == content.id){
                    val item = BarcodeItem()
                    item.field = content.id
                    addItem(item)
                }else{
                    val item = DataItem()
                    item.field = content.id
                    item.content = content.name
                    addItem(item)
                }
            }
        })
        selectDialog.show()
    }

    internal fun deleteItem(){
        mCurItem?.apply {
            contentList.remove(this)

            addDelAction(this)

            swapCurItem(null)
        }
    }

    internal fun shrinkItem(){
        mCurItem?.apply {

            val oldW = width
            val oldH = height
            val oldFont = if (this is TextItem) mFontSize else 0f

            shrink()

            val w = ActionObject.FieldObject(getWidthField(),oldW,width)
            val h = ActionObject.FieldObject(getHeightField(),oldH,height)

            if (this is TextItem){
                addModifyAction(this,w,h,ActionObject.FieldObject(getFontSizeField(),oldFont,mFontSize))
            }else
                addModifyAction(this,w,h)

            invalidate()
        }
    }
    internal fun zoomItem(){
        mCurItem?.apply {

            val oldW = width
            val oldH = height
            val oldFont = if (this is TextItem) mFontSize else 0f

            zoom()

            val w = ActionObject.FieldObject(getWidthField(),oldW,width)
            val h = ActionObject.FieldObject(getHeightField(),oldH,height)

            if (this is TextItem){
                addModifyAction(this,w,h,ActionObject.FieldObject(getFontSizeField(),oldFont,mFontSize))
            }else
                addModifyAction(this,w,h)

            invalidate()
        }
    }

    internal fun rotateItem(){
        mCurItem?.apply {
            radian += 15

            addModifyAction(this,ActionObject.FieldObject(getRadiaField(),radian - 15,radian))

            invalidate()
        }
    }

    fun previewModel(){
        mModel = true
    }
    fun editModel(){
        mModel = false
    }
    private fun hasPreviewModel():Boolean{
        return mModel
    }
    internal fun hasEditModel():Boolean{
        return !mModel
    }

    internal fun getLabelName():String{
        return mLabelTemplate.templateName
    }

    internal fun updateLabelName(n:String){
        mLabelTemplate.templateName = n
    }

    internal fun updateLabelSize(w:Int,h: Int){
        adjustLabelSize(w,h)
        calculateContentSize()

        requestLayout()
        postInvalidate()
    }
    private fun adjustLabelSize(w:Int, h: Int){
        sortLabelSize(w,h)
        mLabelTemplate.width = w
        mLabelTemplate.height = h
    }
    private fun sortLabelSize(w: Int,h: Int){
        mLabelSize.forEachIndexed{index,size ->
            if (size.getrW() == w && size.getrH() == h){
                if (index != 0){
                    val t = mLabelSize[0]
                    mLabelSize[0] = mLabelSize[index]
                    mLabelSize[index] = t
                }
                return
            }
        }
        mLabelSize.add(0,LabelTemplate.LabelSize(w,h))
    }

    internal fun save(){
        CoroutineScope(Dispatchers.IO).launch {
            mItemAttrList.fill(null)
            val template = mLabelTemplate
            template.realWidth = realWidth
            template.realHeight = realHeight
            template.printItem = contentList
            if (template.save()) Utils.showToast(R.string.com_wyc_label_success)
        }
    }


    internal  fun printSingleGoodsBitmap(barcodeId:String = "",labelGoods: LabelGoods):Bitmap{
        val dpi = getSetting().dpi

        val wDot = mLabelTemplate.width2Dot(dpi).toFloat()
        val hDot = mLabelTemplate.height2Dot(dpi).toFloat()

        val bmp = Bitmap.createBitmap(wDot.toInt(), hDot.toInt(),Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)

        mBackground?.apply {
            val matrix = Matrix()
            matrix.setScale(bmp.width / width.toFloat(),bmp.height / height.toFloat())
            c.save()
            c.rotate(mRotate.toFloat(),bmp.width / 2f,bmp.height / 2f)
            c.drawBitmap(this,matrix,null)
            c.restore()
        }

        val p = Paint()
        p.style = Paint.Style.STROKE

        val scaleX = wDot / realWidth.toFloat()
        val scaleY = hDot / realHeight.toFloat()

        val itemCopy: MutableList<ItemBase> = ArrayList()

        labelGoods.apply {
            contentList.forEach {
                val item = it.clone()

                item.transform(scaleX, scaleY)

                if (mRotate != 0){
                    item.rotateByPoint(mRotate.toFloat(),wDot  / 2f, hDot / 2f)
                }

                if (item is DataItem) {
                    item.hasMark = false
                }
                LabelTemplate.assignItemValue(item, this)
                itemCopy.add(item)
            }
        }

        itemCopy.forEach {
            val b = it.createItemBitmap(Color.TRANSPARENT)
            c.save()

            c.translate(it.left.toFloat(), it.top.toFloat())

            c.drawBitmap(b,0f,0f,null)

            c.restore()
        }

        return bmp
    }

    fun setPreviewData(labelGoods: LabelGoods){
        if (hasPreviewModel()){
            labelGoods.apply {
                contentList.forEach {
                    LabelTemplate.assignItemValue(it,this)
                }
                postInvalidate()
            }
        }
    }

    internal fun setRotate(degree:Int){
        mRotate = degree
        postInvalidate()
    }

    internal fun getLabelSize(): MutableList<LabelTemplate.LabelSize> {
        return mLabelSize
    }

    internal fun hasModify():Boolean{
        return mItemAttrList.any { it != null && it.action != ActionObject.Action.ACTIVE }
    }
}