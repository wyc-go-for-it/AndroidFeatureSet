package com.wyc.androidfeatureset.print.printer;

import com.wyc.androidfeatureset.print.receipts.IReceipts;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class PrinterUtils {
    public static void print(IReceipts<?> receipts, final String name){
        try {
            final Class<?> c = Class.forName("com.wyc.androidfeatureset.print.printer." + name);
            Constructor<?> constructor = c.getConstructor();
            IPrinter printer = (IPrinter) constructor.newInstance();
            printer.print(receipts);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }

    }
}
