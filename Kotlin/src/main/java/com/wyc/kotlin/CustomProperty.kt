package com.wyc.kotlin

import com.wyc.logger.Logger
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.kotlin
 * @ClassName:      CustomProperty
 * @Description:    作用描述
 * @Author:         wyc
 * @CreateDate:     2023-07-18 11:08
 * @UpdateUser:     更新者：
 * @UpdateDate:     2023-07-18 11:08
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

class CustomProperty<T> : ReadWriteProperty<Any?,T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
         return "" as T
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        Logger.d(value)
    }
}