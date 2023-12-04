package com.wyc.map;

import android.content.Context;
import android.graphics.Path;

import androidx.annotation.NonNull;
import androidx.core.graphics.PathParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class Utils {
    public static boolean parseSvg(Context context,@NonNull SvgInfo svgInfo){
        try {
            final InputStream inputStream = context.getResources().openRawResource(svgInfo.getRawId());
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document doc = builder.parse(inputStream);
            final Element rootElement = doc.getDocumentElement();
            final String strWidth = rootElement.getAttribute("width");
            final String strHeight = rootElement.getAttribute("height");
            svgInfo.setWidth(Integer.parseInt(strWidth));
            svgInfo.setHeight(Integer.parseInt(strHeight));

            final NodeList items = rootElement.getElementsByTagName("path");
            final List<SvgItem> itemList = svgInfo.getSvgItems();
            for (int i = 1; i < items.getLength(); i++) {
                Element element = (Element) items.item(i);
                String pathData = element.getAttribute("d");
                String name = element.getAttribute("id");
                Path path = PathParser.createPathFromPathData(pathData);
                SvgItem item = new SvgItem(path, name);
                itemList.add(item);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean parseVector(Context context,@NonNull SvgInfo svgInfo){
        try {
            final InputStream inputStream = context.getResources().openRawResource(svgInfo.getRawId());
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document doc = builder.parse(inputStream);
            final Element rootElement = doc.getDocumentElement();
            final String strWidth = rootElement.getAttribute("android:width");
            final String strHeight = rootElement.getAttribute("android:height");
            svgInfo.setWidth(Integer.parseInt(strWidth.replace("dp","")));
            svgInfo.setHeight(Integer.parseInt(strHeight.replace("dp","")));

            final NodeList items = rootElement.getElementsByTagName("path");
            final List<SvgItem> itemList = svgInfo.getSvgItems();
            for (int i = 1; i < items.getLength(); i++) {
                Element element = (Element) items.item(i);
                String pathData = element.getAttribute("pathData");
                Path path = PathParser.createPathFromPathData(pathData);
                SvgItem item = new SvgItem(path, "");
                itemList.add(item);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
