package com.example.a207420_gujincheng_cikguizwan_lab01

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {
    @Query("SELECT * FROM history ORDER BY id DESC")
    fun getAll(): Flow<List<TranslationData>>

    @Insert
    suspend fun insert(item: TranslationData): Long

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Int): Int

    @Query("DELETE FROM history")
    suspend fun clearAll(): Int
}
