package com.wyc.androidfeatureset.print.order;

import androidx.annotation.NonNull;

import com.wyc.logger.Logger;

public class PrintItem {
    private final String mName;
    public PrintItem(final String name){
        mName = name;
    }
    @NonNull
    @Override
    public String toString() {
        return "PrintItem{" +
                "mName='" + mName + '\'' +
                '}';
    }
}
