package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.ReadChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadChapterDao {

    @Query("SELECT chapterUrl FROM read_chapters WHERE comicUrl = :comicUrl")
    fun getReadChapterUrlsForComic(comicUrl: String): Flow<List<String>>

    @Query("SELECT chapterUrl FROM read_chapters")
    fun getAllReadChapterUrls(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM read_chapters WHERE chapterUrl = :chapterUrl LIMIT 1)")
    fun isChapterRead(chapterUrl: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markChapterAsRead(readChapter: ReadChapterEntity)

    @Query("DELETE FROM read_chapters WHERE chapterUrl = :chapterUrl")
    suspend fun unmarkChapter(chapterUrl: String)
}
