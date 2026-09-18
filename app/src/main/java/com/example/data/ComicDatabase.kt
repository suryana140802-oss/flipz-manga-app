package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.BookmarkDao
import com.example.data.dao.DownloadedChapterDao
import com.example.data.dao.ReadChapterDao
import com.example.data.dao.ReadingProgressDao
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.DownloadedChapterEntity
import com.example.data.entity.ReadChapterEntity
import com.example.data.entity.ReadingProgressEntity

@Database(
  entities = [
    ReadingProgressEntity::class,
    BookmarkEntity::class,
    ReadChapterEntity::class,
    DownloadedChapterEntity::class
  ],
  version = 4,
  exportSchema = false
)
abstract class ComicDatabase : RoomDatabase() {

  abstract fun readingProgressDao(): ReadingProgressDao
  abstract fun bookmarkDao(): BookmarkDao
  abstract fun readChapterDao(): ReadChapterDao
  abstract fun downloadedChapterDao(): DownloadedChapterDao

  companion object {
    @Volatile
    private var INSTANCE: ComicDatabase? = null

    fun getInstance(context: Context): ComicDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          ComicDatabase::class.java,
          "flipz_manga.db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
