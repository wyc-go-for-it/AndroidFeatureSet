package com.wyc.androidfeatureset;

import android.util.Log;

import androidx.annotation.NonNull;

import com.wyc.label.LabelGoods;
import com.wyc.label.LabelTemplate;
import com.wyc.label.Utils;
import com.wyc.label.printer.AbstractPrinter;

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
public class PrinterTest extends AbstractPrinter {
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
}
