package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.DownloadedChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedChapterDao {

    @Query("SELECT * FROM downloaded_chapters ORDER BY downloadedAt DESC")
    fun getAllDownloadedChapters(): Flow<List<DownloadedChapterEntity>>

    @Query("SELECT * FROM downloaded_chapters WHERE comicUrl = :comicUrl ORDER BY downloadedAt DESC")
    fun getDownloadedChaptersForComic(comicUrl: String): Flow<List<DownloadedChapterEntity>>

    @Query("SELECT * FROM downloaded_chapters WHERE chapterUrl = :chapterUrl LIMIT 1")
    suspend fun getDownloadedChapter(chapterUrl: String): DownloadedChapterEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_chapters WHERE chapterUrl = :chapterUrl LIMIT 1)")
    fun isChapterDownloaded(chapterUrl: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_chapters WHERE chapterUrl = :chapterUrl LIMIT 1)")
    suspend fun isChapterDownloadedDirect(chapterUrl: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedChapter(chapter: DownloadedChapterEntity)

    @Query("DELETE FROM downloaded_chapters WHERE chapterUrl = :chapterUrl")
    suspend fun deleteDownloadedChapter(chapterUrl: String)
}
