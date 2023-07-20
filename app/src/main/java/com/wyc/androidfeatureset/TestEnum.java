package com.wyc.androidfeatureset;

import androidx.annotation.NonNull;

import com.wyc.label.printer.IType;

public enum TestEnum implements IType {
    WycTest("wyccs",PrinterTest.class.getCanonicalName(),0);
    private String name;
    private String cls;
    private int type;

    TestEnum(String name, String cls, int type){
        this.name = name;
        this.cls = cls;
        this.type = type;
    }

    @NonNull
    @Override
    public String description() {
        return name;
    }

    @NonNull
    @Override
    public String cls() {
        return cls;
    }

    @Override
    public int getDeviceType() {
        return 0;
    }


    @NonNull
    @Override
    public String getEnumName() {
        return this.name();
    }
}
