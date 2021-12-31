package com.wyc.androidfeatureset.print.receipts;

import com.wyc.androidfeatureset.print.parameter.BusinessPrintParameter;
import com.wyc.androidfeatureset.print.order.PrintItem;

import java.util.List;

public abstract class AbstractBusinessReceipts implements IReceipts<BusinessPrintParameter> {
    @Override
    public BusinessPrintParameter getPrintFormat() {
        return new BusinessPrintParameter();
    }

    @Override
    public List<PrintItem> getPrintItem() {
        return format58(getPrintFormat(),"business");
    }
    abstract List<PrintItem> format58(BusinessPrintParameter format, String content);
}
