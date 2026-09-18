package com.example.data.repository

import com.example.data.dao.ReadingProgressDao
import com.example.data.entity.ReadingProgressEntity
import com.example.model.ReadingDirection
import kotlinx.coroutines.flow.Flow

class ReadingProgressRepository(private val dao: ReadingProgressDao) {

  fun getProgressForComic(comicId: String): Flow<ReadingProgressEntity?> =
    dao.getProgressForComic(comicId)

  suspend fun getProgressDirect(comicId: String): ReadingProgressEntity? =
    dao.getProgressDirect(comicId)

  val latestProgress: Flow<ReadingProgressEntity?> = dao.getLatestProgress()

  val allProgress: Flow<List<ReadingProgressEntity>> = dao.getAllProgress()

  suspend fun saveProgress(
    comicId: String,
    comicTitle: String,
    coverUrl: String = "",
    chapterUrl: String = "",
    chapterTitle: String = "",
    pageIndex: Int = 0,
    spreadIndex: Int = 0,
    totalPages: Int = 0,
    readingDirection: ReadingDirection = ReadingDirection.RTL,
  ) {
    dao.saveProgress(
      ReadingProgressEntity(
        comicId = comicId,
        comicTitle = comicTitle,
        coverUrl = coverUrl,
        chapterUrl = chapterUrl,
        chapterTitle = chapterTitle,
        pageIndex = pageIndex,
        spreadIndex = spreadIndex,
        totalPages = totalPages,
        readingDirection = readingDirection.name,
        lastReadTimestamp = System.currentTimeMillis(),
      )
    )
  }

  suspend fun deleteProgress(comicId: String) {
    dao.deleteProgress(comicId)
  }
}
