package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UploadHistoryDao {
  @Query("SELECT * FROM upload_history ORDER BY timestamp DESC")
  fun getAllHistory(): Flow<List<UploadHistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(history: UploadHistoryEntity): Long

  @Query("DELETE FROM upload_history WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM upload_history")
  suspend fun clearAll()
}
