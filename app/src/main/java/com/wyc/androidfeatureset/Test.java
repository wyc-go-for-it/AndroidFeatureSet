package com.wyc.androidfeatureset;

import androidx.annotation.NonNull;

import com.wyc.label.LabelGoods;
import com.wyc.label.LabelTemplate;
import com.wyc.label.lib_annotation.Printer;
import com.wyc.label.printer.AbstractPrinter;
import com.wyc.label.printer.IType;

/**
 * @ProjectName: AndroidFeatureSet
 * @Package: com.wyc.androidfeatureset
 * @ClassName: Test
 * @Description: 作用描述
 * @Author: wyc
 * @CreateDate: 2023-07-31 14:12
 * @UpdateUser: 更新者：
 * @UpdateDate: 2023-07-31 14:12
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */
@Printer
public class Test extends AbstractPrinter implements IType {
    @NonNull
    @Override
    public String getEnumName() {
        return "SSSSS";
    }

    @NonNull
    @Override
    public String description() {
        return "PPPPP";
    }

    @NonNull
    @Override
    public String cls() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public int getDeviceType() {
        return 0;
    }

    @Override
    public void open(@NonNull String arg) {

    }

    @Override
    public void print(@NonNull LabelTemplate labelTemplate, @NonNull LabelGoods goods) {

    }
}
