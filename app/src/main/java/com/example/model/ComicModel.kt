package com.example.model

enum class ReadingDirection {
  LTR, // Western
  RTL, // Manga
}

enum class PaperTexture {
  CLEAN,
  MATTE,
  MANGA_PULP,
  VINTAGE,
}

enum class PortraitReadingMode {
  SINGLE_PAGE_SLIDE,
  LONG_STRIP,
}

data class ComicPage(
  val pageNumber: Int,
  val label: String,
  val imageBytes: ByteArray? = null,
  val imageUrl: String? = null
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ComicPage

    if (pageNumber != other.pageNumber) return false
    if (label != other.label) return false
    if (imageBytes != null) {
      if (other.imageBytes == null) return false
      if (!imageBytes.contentEquals(other.imageBytes)) return false
    } else if (other.imageBytes != null) return false

    return true
  }

  override fun hashCode(): Int {
    var result = pageNumber
    result = 31 * result + label.hashCode()
    result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
    return result
  }
}

data class ComicBook(
  val id: String,
  val title: String,
  val author: String,
  val pages: List<ComicPage>,
  val defaultDirection: ReadingDirection = ReadingDirection.RTL,
  val coverUrl: String = "",
  val comicUrl: String = "",
  val chapterUrl: String = "",
  val chapterTitle: String = "",
) {
  val totalPages: Int get() = pages.size

  fun getPage(index: Int): ComicPage? = pages.getOrNull(index)
}
