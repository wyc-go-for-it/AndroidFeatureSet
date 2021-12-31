package com.wyc.androidfeatureset.print.receipts;

import com.wyc.androidfeatureset.print.order.PrintItem;

import java.util.List;

public interface IReceipts<T> {
    T getPrintFormat();
    List<PrintItem> getPrintItem();
}
