package com.wyc.androidfeatureset.print.receipts;

import com.wyc.androidfeatureset.print.order.PrintItem;
import com.wyc.androidfeatureset.print.order.BusinessOrder;
import com.wyc.androidfeatureset.print.parameter.BusinessPrintParameter;

import java.util.ArrayList;
import java.util.List;

public class BusinessReceipts extends AbstractBusinessReceipts {
    @Override
    List<PrintItem> format58(BusinessPrintParameter format, String content) {
        new BusinessOrder();
        final List<PrintItem> printItems = new ArrayList<>();
        printItems.add(new PrintItem("BusinessOrder"));
        return printItems;
    }
}
