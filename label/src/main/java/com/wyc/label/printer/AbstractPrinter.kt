package com.wyc.label.printer

import android.util.Log
import androidx.annotation.CallSuper


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.label.printer
 * @ClassName:      AbstractPrinter
 * @Description:    打印机抽象类
 * @Author:         wyc
 * @CreateDate:     2022/9/14 15:20
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/9/14 15:20
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

internal abstract class AbstractPrinter:IPrinter {
    protected var mCallback:PrinterStateCallback? = null

    override fun setCallback(callback: PrinterStateCallback?) {
        mCallback = callback
    }
    @CallSuper
    override fun close() {
        if (mCallback != null){
            mCallback = null
        }
    }
    protected fun finalize(){
        Log.d(javaClass.simpleName,"finalized")
    }
    enum class PRINTER_STATE {
        OPEN,CLOSE,FAILURE;
    }
}