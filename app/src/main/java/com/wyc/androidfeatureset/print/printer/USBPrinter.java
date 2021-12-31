package com.wyc.androidfeatureset.print.printer;

import com.wyc.androidfeatureset.print.order.PrintItem;
import com.wyc.androidfeatureset.print.receipts.IReceipts;
import com.wyc.logger.Logger;

public class USBPrinter implements IPrinter {
    @Override
    public void print(IReceipts<?> receipts) {
        System.out.println(getClass().getSimpleName() + " print "+receipts.getClass().getSimpleName());
        for (PrintItem item : receipts.getPrintItem()){
            System.out.println("item:" + item);
        }
    }
}
