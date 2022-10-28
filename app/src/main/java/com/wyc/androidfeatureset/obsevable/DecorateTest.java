package com.wyc.androidfeatureset.obsevable;

/**
 * @ProjectName: AndroidFeatureSet
 * @Package: com.wyc.androidfeatureset.obsevable
 * @ClassName: DecorateTest
 * @Description: 作用描述
 * @Author: wyc
 * @CreateDate: 2022/10/28 11:52
 * @UpdateUser: 更新者：
 * @UpdateDate: 2022/10/28 11:52
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */
public class DecorateTest {
    public static void main(String[] args){
        IDecorator aDecorator = new ADecorator(0,new ADecorator(1,new ADecorator(2,new ADecorator(3,new ADecorator(4,null)))));
        double a = aDecorator.cal();
        System.out.println(a);
    }
}
