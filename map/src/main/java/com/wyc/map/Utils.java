package com.wyc.map;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.graphics.PathParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class Utils {
    public static boolean parseMap(Context context,@NonNull SvgInfo svgInfo){
        try {
            final InputStream inputStream = context.getResources().openRawResource(svgInfo.getRawId());
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document doc = builder.parse(inputStream);
            final Element rootElement = doc.getDocumentElement();
            final String eleName = rootElement.getTagName();
            if ("svg".equals(eleName)){
                parseSvg(rootElement,svgInfo);
            }else if ("vector".equals(eleName)){
                parseVector(rootElement,svgInfo);
            }else throw new InvalidParameterException("invalid xml");
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
    private static void parseSvg(final Element rootElement, @NonNull SvgInfo svgInfo){
        final String strWidth = rootElement.getAttribute("width");
        final String strHeight = rootElement.getAttribute("height");
        svgInfo.setWidth(Integer.parseInt(strWidth));
        svgInfo.setHeight(Integer.parseInt(strHeight));

        final NodeList items = rootElement.getElementsByTagName("path");
        final List<SvgItem> itemList = svgInfo.getSvgItems();
        for (int i = 1; i < items.getLength(); i++) {
            final Element element = (Element) items.item(i);
            itemList.add(new SvgItem(PathParser.createPathFromPathData(element.getAttribute("d")),
                    element.getAttribute("id"),element.getAttribute("stroke"),element.getAttribute("fill")));
        }
    }

    private static void parseVector(final Element rootElement, @NonNull SvgInfo svgInfo){
        final String strWidth = rootElement.getAttribute("android:width");
        final String strHeight = rootElement.getAttribute("android:height");
        svgInfo.setWidth(Integer.parseInt(strWidth.replace("dp","")));
        svgInfo.setHeight(Integer.parseInt(strHeight.replace("dp","")));

        final NodeList items = rootElement.getElementsByTagName("path");
        final List<SvgItem> itemList = svgInfo.getSvgItems();
        for (int i = 1; i < items.getLength(); i++) {
            final Element element = (Element) items.item(i);
            itemList.add(new SvgItem(PathParser.createPathFromPathData(element.getAttribute("android:pathData")), "",
                    element.getAttribute("android:strokeColor"),element.getAttribute("android:fillColor")));
        }
    }

    public static String dealColor(final String color){
        int index = color.indexOf("#");
        if (index != -1){
            if (color.length() == 7){
                return color;
            }
            final String c = color.substring(index + 1);
            int f = 6 - c.length();
            if (f > 0){
                final StringBuilder c_f = new StringBuilder();
                while (f-- > 0){
                    c_f.append("f");
                }
                return String.format("#%s%s",c_f,c);
            }
        }
        return "#ffffff";
    }
}
