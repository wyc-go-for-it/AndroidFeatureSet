package com.wyc.androidfeatureset.print.receipts;

import com.wyc.androidfeatureset.print.order.CheckOrder;
import com.wyc.androidfeatureset.print.parameter.BusinessPrintParameter;
import com.wyc.androidfeatureset.print.order.PrintItem;
import com.wyc.androidfeatureset.print.parameter.SalePrintParameter;

import java.util.ArrayList;
import java.util.List;

public class CheckReceipts extends AbstractSaleReceipts {

    @Override
    List<PrintItem> format58(SalePrintParameter format, String content) {
        new CheckOrder();
        final List<PrintItem> printItems = new ArrayList<>();
        printItems.add(new PrintItem("CheckOrder"));
        return printItems;
    }
}
