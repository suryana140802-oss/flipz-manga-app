package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_chapters")
data class DownloadedChapterEntity(
    @PrimaryKey
    val chapterUrl: String,
    val comicUrl: String,
    val comicTitle: String,
    val chapterTitle: String,
    val coverUrl: String,
    val localDirectoryPath: String,
    val pageCount: Int,
    val downloadedAt: Long = System.currentTimeMillis()
)
