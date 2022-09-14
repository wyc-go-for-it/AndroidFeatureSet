package com.wyc.label

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.ObjectStreamException


/**
 *
 * @ProjectName:    AndroidClient
 * @Package:        com.wyc.cloudapp.design
 * @ClassName:      DataItem
 * @Description:    数据列
 * @Author:         wyc
 * @CreateDate:     2022/3/28 17:32
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/3/28 17:32
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

internal class DataItem: TextItem() {
    var field = ""
    var hasMark = true

    @Throws(ObjectStreamException::class)
    private fun readResolve(): Any {
        serializableInit()
        return this
    }

    override fun drawItem(offsetX: Float, offsetY: Float, canvas: Canvas, paint: Paint) {
        super.drawItem(offsetX, offsetY, canvas, paint)
        if (hasMark){
            val r = radian % 360 != 0f
            if (r){
                canvas.save()
                canvas.rotate(-radian,offsetX+ left + width / 2f,offsetY + top + height / 2f)
            }

            paint.color = Color.RED
            paint.style = Paint.Style.STROKE
            canvas.drawRect(cRECT,paint)

            if (r){
                canvas.restore()
            }
        }
    }

    companion object {
        const val serialVersionUID = 1L
        @JvmStatic
        fun testGoods(): LabelGoods {
            val goods = LabelGoods()
            goods.only_coding = "88888"
            goods.barcode = "6922711043401"
            goods.goodsTitle = "test内容"
            goods.level = "高级"
            goods.origin = "长沙"
            goods.spec = "箱"
            goods.unit = "件"
            goods.retail_price = 18.68
            goods.yh_price = 12.08
            goods.special_price = goods.retail_price * 0.5
            return goods
        }
    }

    enum class FIELD(f:String,n:String){
        Title("goodsTitle","商品名称"),ProductionPlace("origin","产地"),Unit("unit","单位"),
        Spec("spec_str","规格"),Level("level","等级"),Barcode("barcode","条码"),OnlyCoding("only_coding","货号"),
        VipPrice("yh_price","会员价"),RetailPrice("retail_price","零售价"),SpecialPrice("special_price","零售特价");
        val field = f
        val description = n
    }

    override fun toString(): String {
        return "DataItem(field='$field', hasMark=$hasMark) ${super.toString()}"
    }
}