package com.wyc.androidfeatureset.reflect;

import com.wyc.logger.Logger;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @ProjectName: AndroidFeatureSet
 * @Package: com.wyc.androidfeatureset.reflect
 * @ClassName: TypeTest
 * @Description: Type测试类
 * @Author: wyc
 * @CreateDate: 2022/10/27 15:17
 * @UpdateUser: 更新者：
 * @UpdateDate: 2022/10/27 15:17
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */
public class TypeTest extends TypeBase<TClass,KClass,VClass> {

    public static void main(String[] args){
        test();
    }

    public static void test(){
        TypeArrayTest<Integer> typeTest = new TypeArrayTest<>();
        Class<?> c = typeTest.getClass();
        Type type = c.getGenericSuperclass();
        ParameterizedType parameterizedType = ((ParameterizedType)type);
        Type ownType = parameterizedType.getOwnerType();
        Type rawType = parameterizedType.getRawType();
        Type[] types = parameterizedType.getActualTypeArguments();

        Logger.d("ownType:%s",ownType);
        Logger.d("rawType:%s",rawType);
        Logger.d("types:%s", Arrays.toString(types));

        ParameterizedType pArray = (ParameterizedType) types[0];

        Type ownTypeArray = pArray.getOwnerType();
        Type rawTypeArray = pArray.getRawType();
        Type[] typesArray = pArray.getActualTypeArguments();

        Logger.d("ownType:%s",ownTypeArray);
        Logger.d("rawType:%s",rawTypeArray);
        Logger.d("types:%s", Arrays.toString(typesArray));
    }

}

class TypeArrayTest<T> extends TypeBase<List<Map<? extends TypeBase,? super TClass>>,KClass,T>{

}