package com.wyc.label;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * @ProjectName: AndroidClient
 * @Package: com.wyc.cloudapp.design
 * @ClassName: LabelTemplate1
 * @Description: 标签模板
 * @Author: wyc
 * @CreateDate: 2022/4/14 15:50
 * @UpdateUser: 更新者：
 * @UpdateDate: 2022/4/14 15:50
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */
@Entity(tableName = "labelTemplate")
public class LabelTemplate implements Parcelable {
    @PrimaryKey
    @NonNull
    Integer templateId = UUID.randomUUID().toString().hashCode();
    @NonNull
    String templateName = "";
    /**
     * 打印物理尺寸 单位毫米
     * */
    @NonNull
    Integer width = 0;
    @NonNull
    Integer height = 0;

    /**
     * 用于重新计算item尺寸，同一个格式可能会加载到不同尺寸的界面
     * */
    @NonNull
    Integer realWidth = width2Pixel(this, LabelApp.Companion.getInstance());
    @NonNull
    Integer realHeight = height2Pixel(this, LabelApp.Companion.getInstance());
    @NonNull
    String itemList = "[]";

    /**
     * 背景base64字符串
     * */
    @NonNull
    String backgroundImg = "";

    @JSONField(serialize = false)
    @Ignore
    List<ItemBase> printItem = new ArrayList<>();

    public LabelTemplate(){
        width = 70;
        height = 40;
        templateName = String.format(Locale.CHINA,"%s_%d_%d","未命名",width,height);
    }

    public boolean hasItem(){
        return !printItem.isEmpty();
    }

    protected LabelTemplate(Parcel in) {
        templateId = in.readInt();
        templateName = in.readString();
        width = in.readInt();
        height = in.readInt();
        realWidth = in.readInt();
        realHeight = in.readInt();
        itemList = in.readString();
        backgroundImg = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(templateId);
        dest.writeString(templateName);
        dest.writeInt(width);
        dest.writeInt(height);
        dest.writeInt(realWidth);
        dest.writeInt(realHeight);
        dest.writeString(itemList);
        dest.writeString(backgroundImg);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LabelTemplate> CREATOR = new Creator<LabelTemplate>() {
        @Override
        public LabelTemplate createFromParcel(Parcel in) {
            return new LabelTemplate(in);
        }

        @Override
        public LabelTemplate[] newArray(int size) {
            return new LabelTemplate[size];
        }
    };

    public int width2Dot(int dpi){
        return (int) (width * dpi * (1.0f / 25.4f));
    }
    public int height2Dot(int dpi){
        return (int) (height * dpi * (1.0f / 25.4f));
    }

    public List<ItemBase> printSingleGoodsById(@Nullable DataItem.LabelGoods goods){
        generatePrinterDataItem(goods);
        return printItem;
    }

    public void generatePrinterDataItem(@Nullable DataItem.LabelGoods goods){
        if (goods != null){
            for(ItemBase item : printItem){
                assignItemValue(item,goods);
            }
        }
    }

    public static void assignItemValue(ItemBase item ,DataItem.LabelGoods goods){
        if (item instanceof DataItem){
            DataItem i = (DataItem)item;
            i.setContent(goods.getValueByField(i.getField()));
            i.updateNewline();
        }else if (item instanceof CodeItemBase && !((CodeItemBase)item).getField().isEmpty()){
            CodeItemBase i = (CodeItemBase)item;
            i.setHasMark(false);
            i.setContent(goods.getValueByField(i.getField()));
        }
    }

    public static List<LabelSize> getDefaultSize(){
        final List<LabelSize> list = new ArrayList<>();
        list.add(new LabelSize(70,40));
        list.add(new LabelSize(50,40));
        list.add(new LabelSize(30,20));
        return list;
    }

    public static LabelTemplate parse(String json){
        try {
            return JSONObject.parseObject(json,LabelTemplate.class);
        }catch (JSONException e){
            Utils.showToast(R.string.c_label_failure,e.getMessage());
        }
        return new LabelTemplate();
    }

    public static int width2Pixel(LabelTemplate labelTemplate , Context context){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, labelTemplate.getWidth(),context.getResources().getDisplayMetrics());
    }

    public static int height2Pixel(LabelTemplate labelTemplate , Context context){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, labelTemplate.getHeight(),context.getResources().getDisplayMetrics());
    }

    public List<ItemBase> printSingleGoods(DataItem.LabelGoods goods){
        List<ItemBase> itemCopy = new ArrayList<>();
        for (ItemBase i:printItem) {
            final ItemBase item = i.clone();
            assignItemValue(item,goods);
            itemCopy.add(item);
        }
        return itemCopy;
    }

    private List<ItemBase> generatePrintItem(){
        float scaleX = (float)width2Dot(LabelPrintSetting.getSetting().getDpi()) / (float)realWidth;
        float scaleY =(float) height2Dot(LabelPrintSetting.getSetting().getDpi()) / (float)realHeight;
        List<ItemBase> content = toItem();
        for (ItemBase i:content) {
            i.transform(scaleX,scaleY);
            if (i instanceof DataItem){
                ((DataItem) i).setHasMark(false);
            }
        }
        return content;
    }

    public List<ItemBase>  toItem(){
        final JSONArray a =  JSONArray.parseArray(itemList);
        final List<ItemBase> list = new ArrayList<>();

        for (int i = 0,size = a.size(); i < size; i ++){
            final JSONObject obj = a.getJSONObject(i);
            try {
                list.add((ItemBase) obj.toJavaObject(Class.forName("com.wyc.label." + obj.getString("clsType"))));
            }catch (ClassNotFoundException ignore){

            }
        }
        return list;
    }

    public Integer getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Integer templateId) {
        this.templateId = templateId;
    }

    public @NonNull String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(@NonNull String templateName) {
        this.templateName = templateName;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getRealWidth() {
        return realWidth;
    }

    public void setRealWidth(Integer realWidth) {
        this.realWidth = realWidth;
    }

    public Integer getRealHeight() {
        return realHeight;
    }

    public void setRealHeight(Integer realHeight) {
        this.realHeight = realHeight;
    }

    public @NonNull String getItemList() {
        return itemList;
    }

    public void setItemList(String itemList) {
        if (itemList == null)itemList = "[]";

        this.itemList = itemList;

        if (!printItem.isEmpty())printItem.clear();
        printItem.addAll(generatePrintItem());
    }

    public @NonNull String getBackgroundImg() {
        return backgroundImg;
    }

    public void setBackgroundImg(@NonNull String backgroundImg) {
        this.backgroundImg = backgroundImg;
    }

    public List<ItemBase> getPrintItem() {
        return printItem;
    }

    public void setPrintItem(List<ItemBase> printItem) {
        this.printItem = printItem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabelTemplate that = (LabelTemplate) o;
        return templateId.equals(that.templateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(templateId);
    }

    @Override
    public String toString() {
        return "LabelTemplate1{" +
                "templateId=" + templateId +
                ", templateName='" + templateName + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", realWidth=" + realWidth +
                ", realHeight=" + realHeight +
                ", itemList='" + itemList + '\'' +
                ", backgroundImg='" + backgroundImg + '\'' +
                ", printItem=" + printItem +
                '}';
    }

    static public class LabelSize{
        private int rW;
        private int rH;
        private String description;

        public LabelSize(int w, int h) {
            this.rW = w;
            this.rH = h;
            description = String.format(Locale.CHINA,"%d*%d",w,h);
        }

        public int getrW() {
            return rW;
        }

        public void setrW(int rW) {
            this.rW = rW;
        }

        public int getrH() {
            return rH;
        }

        public void setrH(int rH) {
            this.rH = rH;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LabelSize labelSize = (LabelSize) o;
            return rW == labelSize.rW && rH == labelSize.rH;
        }

        @Override
        public int hashCode() {
            return Objects.hash(rW, rH);
        }
    }
}
