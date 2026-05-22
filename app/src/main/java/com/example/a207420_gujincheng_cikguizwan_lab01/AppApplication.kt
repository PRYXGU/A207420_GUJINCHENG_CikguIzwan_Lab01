package com.example.a207420_gujincheng_cikguizwan_lab01

import android.app.Application
import androidx.room.Room

class AppApplication : Application() {
    // 数据库单例：整个 App 只创建一份实例
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "transsistant_database"
        ).build()
    }

    // Repository 单例：基于上面的数据库 DAO 构建，供 ViewModel 使用
    val repository: TranslationRepository by lazy {
        TranslationRepository(database.translationDao())
    }
}
