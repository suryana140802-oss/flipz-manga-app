package com.example.domain.model

data class Comic(
    val title: String,
    val url: String,
    val thumbnailUrl: String,
    val latestChapter: String
)

data class Chapter(
    val title: String,
    val url: String,
    val date: String
)

data class ComicDetail(
    val title: String,
    val url: String,
    val thumbnailUrl: String,
    val synopsis: String,
    val chapters: List<Chapter>,
    val genres: List<String> = emptyList()
)
