package com.wyc.label

import com.wyc.label.LabelApp.Companion.getInstance
import com.wyc.label.Utils.Companion.showToast
import com.wyc.label.printer.GPPrinter
import com.wyc.label.printer.HTPrinter
import com.wyc.label.printer.IType
import com.wyc.label.printer.ThermalPrinter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.util.*

/**
 *
 * @ProjectName:    AndroidClient
 * @Package:        com.wyc.cloudapp.bean
 * @ClassName:      LabelPrintSetting
 * @Description:    标签打印设置
 * @Author:         wyc
 * @CreateDate:     2022/3/25 16:53
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/3/25 16:53
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

internal class LabelPrintSetting:Serializable {
    @Transient private var change = false

    enum class Way(s: String)  {
        BLUETOOTH_PRINT(getInstance().getString(R.string.com_wyc_label_bluetooth_way)),WIFI_PRINT(getInstance().getString(R.string.com_wyc_label_wifi_way));
        val description:String = s
    }
    var way: Way  = Way.BLUETOOTH_PRINT

    enum class Type(s: String,c:String) : IType  {
        BT_GP_M322(getInstance().getString(R.string.com_wyc_label_gp_m322),GPPrinter::class.java.simpleName),
        WIFI_HPRT_HT300(getInstance().getString(R.string.com_wyc_label_wifi_hprt_ht300),HTPrinter::class.java.simpleName),NULL("",""),
        BT_TPrinter(getInstance().getString(R.string.com_wyc_label_tp),ThermalPrinter::class.java.simpleName);
        private val description:String = s
        private val cls:String = c

        override fun getEnumName(): String {
            return name
        }
        override fun description():String{
            return description
        }
        override fun cls():String{
            return cls
        }
        override fun getDeviceType():Int{
            return 0
        }
    }
    var type: IType = Type.BT_GP_M322
    fun getValidPrinterType(): List<IType> {
        val l = mutableListOf<IType>()

        l.addAll(Type.values().filter {
            when(way){
                Way.BLUETOOTH_PRINT->{
                    it.name.startsWith("BT")
                }
                Way.WIFI_PRINT ->{
                    it.name.startsWith("WIFI")
                }else -> false
            }
        })

        l.addAll(LabelApp.getDriverList().filter {
            Way.BLUETOOTH_PRINT == way && it.getDeviceType() == 0 || Way.WIFI_PRINT == way && it.getDeviceType() == 1
        })

       return l
    }

    enum class Rotate(degree:Int){
        D_0(0),D_180(180);
        val description:String = degree.toString()
        val value = degree
    }

    var paperType: PaperType  = PaperType.GAP
    enum class PaperType(s: String,v:Int){
        SEQUENCE("连续纸",0),GAP("间隔纸",5),BLACK_LABEL("黑标纸",2);
        val description:String = s
        var value = v//单位 mm
    }

    /**
     * 打印偏移 单位mm
     * */
    var offsetX = 0
    var offsetY = 0

    var dpi = 203

    var rotate: Rotate = Rotate.D_0

    var labelTemplateId: Int  = 0
    var labelTemplateName:String = ""

    var printNum  = 1

    var printer:String = ""
    fun getPrinterAddress():String{
        if (printer.contains("@")){
            return printer.split("@")[1]
        }
        return ""
    }

    var density = 4
        get() {
            if (field < 1)return 1
            if (field > 15)return 15
            return field
        }


    @Throws(ObjectStreamException::class)
    private fun readResolve(): Any {
        if (paperType == null)
            paperType = PaperType.GAP
        if (type == null)
            type = Type.BT_GP_M322
        return this
    }

    companion object{
        const val serialVersionUID = 1L

        @JvmStatic
        fun valueOf(id:String):IType{
            var i = LabelApp.getDriverList().find { it.getEnumName() == id }
            if (i == null){
                i = Type.values().find { it.getEnumName() == id }
                if (i == null)i = Type.NULL
            }
            return i
        }

        @JvmStatic
        private fun getFile(): File {
            val dirPath =String.format("%s%s%s",LabelApp.getDir(), File.separator,"setting")
            val dir = File(dirPath)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val name = String.format(Locale.CHINA, "%s%s%s", dir.absolutePath, File.separator,"setting")
            return File(name)
        }
        @JvmStatic
        fun combinationPrinter(a:String,n:String):String{
            return String.format("%s@%s",n,a)
        }
        @JvmStatic
        fun getSetting(): LabelPrintSetting {
            try {
                ObjectInputStream(FileInputStream(getFile())).use {
                    val obj = it.readObject()
                    if (obj is LabelPrintSetting) return obj
                }
            } catch (e: IOException) {
                e.printStackTrace()
                if (e !is FileNotFoundException)showToast(e.message)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                showToast(e.message)
            }
            return LabelPrintSetting()
        }
    }

    fun saveSetting(){
        if (type == Type.NULL){
            showToast(R.string.com_wyc_label_print_type_not_empty)
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val file = getFile()
            file.delete()
            try {
                ObjectOutputStream(FileOutputStream(file)).use { fileOutputStream ->
                    fileOutputStream.writeObject(
                        this@LabelPrintSetting
                    )
                }
                showToast(R.string.com_wyc_label_success)
            } catch (e: IOException) {
                e.printStackTrace()
                showToast(e.message)
            }
        }
    }

    fun hasChange():Boolean{
        return change
    }

    override fun toString(): String {
        return "LabelPrintSetting(way=$way, type=$type, paperType=$paperType, offsetX=$offsetX, offsetY=$offsetY, dpi=$dpi, rotate=$rotate, labelTemplateId=$labelTemplateId, labelTemplateName='$labelTemplateName', printNum=$printNum, printer='$printer', density=$density)"
    }


}