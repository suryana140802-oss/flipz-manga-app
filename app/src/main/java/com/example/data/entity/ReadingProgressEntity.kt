package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
  @PrimaryKey
  val comicId: String,
  val comicTitle: String,
  val coverUrl: String = "",
  val chapterUrl: String = "",
  val chapterTitle: String = "",
  val pageIndex: Int = 0,
  val spreadIndex: Int = 0,
  val totalPages: Int = 0,
  val readingDirection: String = "RTL",
  val lastReadTimestamp: Long = System.currentTimeMillis(),
)
