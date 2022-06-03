package com.wyc.label;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
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
public class LabelTemplate implements Serializable {
    private final static long serialVersionUID = 1L;
    @NonNull
    private Integer templateId = UUID.randomUUID().toString().hashCode();
    @NonNull
    private String templateName = "";
    /**
     * 打印物理尺寸 单位毫米
     * */
    @NonNull
    private Integer width = 0;
    @NonNull
    private Integer height = 0;

    /**
     * 用于重新计算item尺寸，同一个格式可能会加载到不同尺寸的界面
     * */
    @NonNull
    private Integer realWidth = width2Pixel(this, LabelApp.Companion.getInstance());
    @NonNull
    private Integer realHeight = height2Pixel(this, LabelApp.Companion.getInstance());

    transient private static final String mLabelDir = LabelApp.getDir();

    /**
     * 背景base64字符串
     * */
    @NonNull
    private String backgroundImg = "";

    private List<ItemBase> printItem = new ArrayList<>();

    public LabelTemplate(){
        width = 70;
        height = 40;
        templateName = String.format(Locale.CHINA,"%s_%d_%d","未命名",width,height);
    }

    public boolean save(){
        File file = getFile();
        file.delete();
        try{
            write(new FileOutputStream(file),this);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Utils.showToast(e.getMessage());
        }
        return false;
    }

    public static List<LabelTemplate> getLabelList(){
        final List<LabelTemplate> list = new ArrayList<>();
        File dir = new File(mLabelDir);
        File[] names = dir.listFiles();
        if (names != null){
            for (File f : names){
                if (f.isDirectory())continue;
                try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(f))){
                    final Object obj = objectInputStream.readObject();
                    if (obj instanceof LabelTemplate)list.add((LabelTemplate)obj );
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    Utils.showToast(e.getMessage());
                    break;
                }
            }
        }
        return list;
    }

    public boolean deleteLabel(){
        return getFile().delete();
    }

    public static LabelTemplate getLabelById(int templateId){
        File dir = new File(mLabelDir);
        File[] names = dir.listFiles();
        if (names != null){
            for (File f : names){
                if (String.valueOf(templateId).equals(f.getName())){
                    try{
                        return read(new FileInputStream(f));
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                        Utils.showToast(e.getMessage());
                        break;
                    }
                }
            }
        }
        return new LabelTemplate();
    }
    public static LabelTemplate read(InputStream inputStream) throws IOException, ClassNotFoundException {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)){
            return (LabelTemplate) objectInputStream.readObject();
        }
    }
    public static void write(OutputStream outputStream, @NonNull LabelTemplate labelTemplate) throws IOException {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)){
            objectOutputStream.writeObject(labelTemplate);
        }
    }

    private File getFile(){
        File file = new File(mLabelDir);
        if (!file.exists()){
            file.mkdirs();
        }
        final String name = String.format(Locale.CHINA,"%s%s%d",file.getAbsolutePath(),File.separator,templateId);
        return new File(name);
    }

    public boolean hasItem(){
        return !printItem.isEmpty();
    }

    public int width2Dot(int dpi){
        return (int) (width * dpi * (1.0f / 25.4f));
    }
    public int height2Dot(int dpi){
        return (int) (height * dpi * (1.0f / 25.4f));
    }

    public void generatePrinterDataItem(@Nullable LabelGoods goods){
        if (goods != null){
            for(ItemBase item : generatePrintItem()){
                assignItemValue(item,goods);
            }
        }
    }

    public static void assignItemValue(ItemBase item ,LabelGoods goods){
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

    public static int width2Pixel(LabelTemplate labelTemplate , Context context){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, labelTemplate.getWidth(),context.getResources().getDisplayMetrics());
    }

    public static int height2Pixel(LabelTemplate labelTemplate , Context context){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, labelTemplate.getHeight(),context.getResources().getDisplayMetrics());
    }

    public List<ItemBase> printSingleGoods(LabelGoods goods){
        List<ItemBase> itemCopy = new ArrayList<>();
        for (ItemBase i:generatePrintItem()) {
            final ItemBase item = i.clone();
            assignItemValue(item,goods);
            itemCopy.add(item);
        }
        return itemCopy;
    }

    private List<ItemBase> generatePrintItem(){
        float scaleX = (float)width2Dot(LabelPrintSetting.getSetting().getDpi()) / (float)realWidth;
        float scaleY =(float) height2Dot(LabelPrintSetting.getSetting().getDpi()) / (float)realHeight;
        List<ItemBase> content = new ArrayList<>();
        for (ItemBase i:printItem) {
            final ItemBase item = i.clone();
            item.transform(scaleX,scaleY);
            if (item instanceof DataItem){
                ((DataItem) item).setHasMark(false);
            }
            content.add(item);
        }
        return content;
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

    public @NonNull String getBackgroundImg() {
        return backgroundImg;
    }

    public void setBackgroundImg(@NonNull String backgroundImg) {
        this.backgroundImg = backgroundImg;
    }

    public List<ItemBase> getPrintItem() {
        List<ItemBase> content = new ArrayList<>();
        for (ItemBase i:printItem) {
            content.add(i.clone());
        }
        return content;
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
