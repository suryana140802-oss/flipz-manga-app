package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey
    val comicUrl: String,
    val title: String,
    val coverUrl: String,
    val latestChapter: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
