package com.wyc.label.room

import androidx.room.*
import com.wyc.label.LabelTemplate

@Dao
interface LabelTemplateDao {
    @Query("select * from labelTemplate")
    fun getAll():MutableList<LabelTemplate>
    @Query("select * from labelTemplate where templateId=:id")
    fun getLabelTemplateById(id:Int):LabelTemplate?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTemplate(labelTemplate: LabelTemplate): Long
    @Delete
    fun deleteTemplateById(labelTemplate: LabelTemplate)
}