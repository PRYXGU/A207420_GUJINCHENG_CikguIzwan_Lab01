package com.example.a207420_gujincheng_cikguizwan_lab01

import kotlinx.coroutines.flow.Flow

// 【Repository 层】
// Tutorial 要求：Repository 负责连接 ViewModel ↔ DAO。
// ViewModel 不再直接接触 DAO/数据库，而是通过这一层访问数据，
// 保持各层职责清晰（UI -> ViewModel -> Repository -> DAO -> Room/SQLite）。
class TranslationRepository(private val dao: TranslationDao) {

    // 把 DAO 的数据流向上暴露给 ViewModel（数据库变化时会自动发新值）
    val allHistory: Flow<List<TranslationData>> = dao.getAll()

    suspend fun insert(item: TranslationData): Long = dao.insert(item)

    suspend fun deleteById(id: Int): Int = dao.deleteById(id)

    suspend fun clearAll(): Int = dao.clearAll()
}
