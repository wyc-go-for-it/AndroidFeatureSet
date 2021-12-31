package com.wyc.androidfeatureset.print.printer;

import com.wyc.androidfeatureset.print.receipts.IReceipts;

interface IPrinter {
    void print(IReceipts<?> receipts);
}
