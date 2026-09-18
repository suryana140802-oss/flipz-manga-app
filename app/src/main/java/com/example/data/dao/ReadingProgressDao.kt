package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.ReadingProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingProgressDao {

  @Query("SELECT * FROM reading_progress WHERE comicId = :comicId LIMIT 1")
  fun getProgressForComic(comicId: String): Flow<ReadingProgressEntity?>

  @Query("SELECT * FROM reading_progress WHERE comicId = :comicId LIMIT 1")
  suspend fun getProgressDirect(comicId: String): ReadingProgressEntity?

  @Query("SELECT * FROM reading_progress ORDER BY lastReadTimestamp DESC LIMIT 1")
  fun getLatestProgress(): Flow<ReadingProgressEntity?>

  @Query("SELECT * FROM reading_progress ORDER BY lastReadTimestamp DESC")
  fun getAllProgress(): Flow<List<ReadingProgressEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun saveProgress(progress: ReadingProgressEntity)

  @Query("DELETE FROM reading_progress WHERE comicId = :comicId")
  suspend fun deleteProgress(comicId: String)
}
