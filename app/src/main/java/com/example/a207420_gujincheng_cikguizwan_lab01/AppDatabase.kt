package com.example.a207420_gujincheng_cikguizwan_lab01

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TranslationData::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun translationDao(): TranslationDao
}
