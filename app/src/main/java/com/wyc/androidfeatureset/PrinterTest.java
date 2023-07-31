package com.wyc.androidfeatureset;

import android.util.Log;

import androidx.annotation.NonNull;

import com.wyc.label.LabelGoods;
import com.wyc.label.LabelTemplate;
import com.wyc.label.Utils;
import com.wyc.label.lib_annotation.Printer;
import com.wyc.label.printer.AbstractPrinter;
import com.wyc.label.printer.IType;

/**
 * @ProjectName: AndroidFeatureSet
 * @Package: com.wyc.androidfeatureset
 * @ClassName: PrinterTest
 * @Description: 作用描述
 * @Author: wyc
 * @CreateDate: 2023-07-19 15:40
 * @UpdateUser: 更新者：
 * @UpdateDate: 2023-07-19 15:40
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */
@Printer
public class PrinterTest extends AbstractPrinter implements IType {
    @Override
    public void open(@NonNull String arg) {
        if (getMCallback() != null){
            getMCallback().onSuccess(this);
        }else
            Utils.showToast(R.string.com_wyc_label_conn_success);
    }

    @Override
    public void print(@NonNull LabelTemplate labelTemplate, @NonNull LabelGoods goods) {
        Log.e("print:" ,goods.toString());
    }

    @NonNull
    @Override
    public String description() {
        return "注解打印机";
    }

    @Override
    public int getDeviceType() {
        return 0;
    }

    @NonNull
    @Override
    public String getEnumName() {
        return "processor test";
    }

    @NonNull
    @Override
    public String cls() {
        return this.getClass().getCanonicalName();
    }
}
