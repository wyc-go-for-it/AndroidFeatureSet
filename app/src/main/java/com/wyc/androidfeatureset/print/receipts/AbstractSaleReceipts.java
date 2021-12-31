package com.wyc.androidfeatureset.print.receipts;

import com.wyc.androidfeatureset.print.order.PrintItem;
import com.wyc.androidfeatureset.print.parameter.SalePrintParameter;

import java.util.List;

public abstract class AbstractSaleReceipts implements IReceipts<SalePrintParameter> {
    @Override
    public final SalePrintParameter getPrintFormat() {
        return new SalePrintParameter();
    }

    @Override
    public final List<PrintItem> getPrintItem() {
        return format58(getPrintFormat(),"check");
    }
    abstract List<PrintItem> format58(SalePrintParameter format, String content);
}
