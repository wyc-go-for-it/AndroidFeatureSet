package com.wyc.label.room

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wyc.label.App
import com.wyc.label.LabelTemplate


/**
 *
 * @ProjectName:    AndroidFeatureSet
 * @Package:        com.wyc.label
 * @ClassName:      AppDatabase
 * @Description:    数据库
 * @Author:         wyc
 * @CreateDate:     2022/4/22 16:17
 * @UpdateUser:     更新者：
 * @UpdateDate:     2022/4/22 16:17
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */

@Database(entities = [LabelTemplate::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun LabelTemplateDao(): LabelTemplateDao

    companion object{
        private var DB: AppDatabase? = null
        @JvmStatic
        fun getInstance(): AppDatabase {
            if (DB == null) {
                synchronized(AppDatabase::class.java) {
                    if (DB == null) {
                        DB = Room.databaseBuilder(App.getInstance(), AppDatabase::class.java,"label_db")
                                .allowMainThreadQueries()
                                .build()
                    }
                }
            }
            return DB!!
        }
    }
}