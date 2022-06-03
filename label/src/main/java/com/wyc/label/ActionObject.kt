package com.wyc.label

import java.lang.reflect.Field


/**
 *
 * @ProjectName:    AndroidClient
 * @Package:        com.wyc.cloudapp.design
 * @ClassName:      ActionObject
 * @Description:    作用描述
 * @Author:         wyc
 * @CreateDate:     2022/4/12 16:54
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/4/12 16:54
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

internal class ActionObject(var actionObj: ItemBase, var action:Action, var fieldList: MutableList<FieldObject>?) {



    class FieldObject(var field: Field?,var oldValue:Any,var newValue:Any){


        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as FieldObject

            if (field != other.field) return false
            if (oldValue != other.oldValue) return false
            if (newValue != other.newValue) return false

            return true
        }

        override fun hashCode(): Int {
            var result = field.hashCode()
            result = 31 * result + oldValue.hashCode()
            result = 31 * result + newValue.hashCode()
            return result
        }

        override fun toString(): String {
            return "FieldObject(field=$field, oldValue=$oldValue, newValue=$newValue)"
        }
    }
    enum class Action{
        ADD,DEL,MOD,ACTIVE
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ActionObject

        if (actionObj != other.actionObj) return false
        if (action != other.action) return false
        if (fieldList != other.fieldList) return false

        return true
    }

    override fun hashCode(): Int {
        var result = actionObj.hashCode()
        result = 31 * result + action.hashCode()
        result = 31 * result + fieldList.hashCode()
        return result
    }

    override fun toString(): String {
        return "ActionObject(actionObj=$actionObj, action=$action, fieldList=${fieldList?.toTypedArray().contentToString()})"
    }

}