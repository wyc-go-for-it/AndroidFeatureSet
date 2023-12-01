package com.wyc.map;

import java.util.ArrayList;
import java.util.List;

public class SvgInfo {
    private int rawId;
    private int width;
    private int height;
    private List<SvgItem> svgItems = new ArrayList<>();


    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public List<SvgItem> getSvgItems() {
        return svgItems;
    }

    public void setSvgItems(List<SvgItem> svgItems) {
        this.svgItems = svgItems;
    }

    public int getRawId() {
        return rawId;
    }

    public void setRawId(int rawId) {
        this.rawId = rawId;
    }
}
