package com.wyc.label;

import java.nio.charset.StandardCharsets;

public class Check {
    public static void c(Object o){
        check(o);
    }
    private native static void check(Object context);
    static {
        System.loadLibrary("core");
    }
}
