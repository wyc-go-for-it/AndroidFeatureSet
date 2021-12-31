package com.wyc.androidfeatureset.print;

import com.wyc.androidfeatureset.print.printer.PrinterUtils;
import com.wyc.androidfeatureset.print.receipts.BusinessReceipts;
import com.wyc.androidfeatureset.print.receipts.CheckReceipts;

public class App {
    public static void main(String[] args) {
        PrinterUtils.print(new CheckReceipts(),"USBPrinter");
        PrinterUtils.print(new CheckReceipts(),"BluetoothPrinter");
        PrinterUtils.print(new BusinessReceipts(),"USBPrinter");
        PrinterUtils.print(new BusinessReceipts(),"BluetoothPrinter");
    }
}
