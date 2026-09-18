package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "read_chapters")
data class ReadChapterEntity(
    @PrimaryKey
    val chapterUrl: String,
    val comicUrl: String,
    val chapterTitle: String,
    val readTimestamp: Long = System.currentTimeMillis()
)
