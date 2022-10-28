package com.wyc.androidfeatureset.obsevable;

/**
 * @ProjectName: AndroidFeatureSet
 * @Package: com.wyc.androidfeatureset.obsevable
 * @ClassName: ADecorator
 * @Description: 作用描述
 * @Author: wyc
 * @CreateDate: 2022/10/28 11:51
 * @UpdateUser: 更新者：
 * @UpdateDate: 2022/10/28 11:51
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */
public class ADecorator implements IDecorator{
    private double amt = 5;
    private final IDecorator next;
    public ADecorator(double a,IDecorator n){
        amt =a;
        next = n;
    }
    @Override
    public double cal() {
        if (next != null)
            return amt + next.cal();
        else return amt;
    }
}
