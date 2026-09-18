package com.example.service

import android.content.Context
import android.net.Uri
import com.example.model.ComicBook
import com.example.model.ComicPage
import com.example.model.ReadingDirection
import com.github.junrar.Archive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ComicArchiveParser {

  private val NATURAL_SORT_PATTERN = Pattern.compile("(\\d+)|(\\D+)")
  private val VALID_IMAGE_EXTENSIONS = listOf(
    ".jpg", ".jpeg", ".png", ".webp", ".bmp", ".gif", ".avif", ".heic", ".heif", ".tiff", ".tif", ".ico"
  )

  /**
   * Check if a filename or byte content matches valid comic images.
   */
  fun isImageEntry(filename: String, bytes: ByteArray? = null): Boolean {
    val nameLower = filename.lowercase()
    if (nameLower.contains("__macosx") || nameLower.contains("/.") || nameLower.startsWith(".")) {
      return false
    }
    if (VALID_IMAGE_EXTENSIONS.any { nameLower.endsWith(it) }) {
      return true
    }
    if (bytes != null && bytes.size >= 4) {
      // Check magic numbers for common images without standard extensions
      val b0 = bytes[0].toInt() and 0xFF
      val b1 = bytes[1].toInt() and 0xFF
      val b2 = bytes[2].toInt() and 0xFF
      val b3 = bytes[3].toInt() and 0xFF
      // JPEG: FF D8 FF
      if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) return true
      // PNG: 89 50 4E 47
      if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) return true
      // GIF: 47 49 46
      if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46) return true
      // BMP: 42 4D
      if (b0 == 0x42 && b1 == 0x4D) return true
      // WEBP: RIFF ... WEBP
      if (b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46 && bytes.size >= 12) {
        val b8 = bytes[8].toInt() and 0xFF
        val b9 = bytes[9].toInt() and 0xFF
        val b10 = bytes[10].toInt() and 0xFF
        val b11 = bytes[11].toInt() and 0xFF
        if (b8 == 0x57 && b9 == 0x45 && b10 == 0x42 && b11 == 0x50) return true
      }
    }
    return false
  }

  /**
   * Natural alphanumeric comparator (e.g. 1.jpg, 2.jpg, 10.jpg)
   */
  fun naturalCompare(a: String, b: String): Int {
    val matcherA = NATURAL_SORT_PATTERN.matcher(a)
    val matcherB = NATURAL_SORT_PATTERN.matcher(b)

    while (matcherA.find() && matcherB.find()) {
      val tokenA = matcherA.group()
      val tokenB = matcherB.group()

      val numA = tokenA.toLongOrNull()
      val numB = tokenB.toLongOrNull()

      if (numA != null && numB != null) {
        val diff = numA.compareTo(numB)
        if (diff != 0) return diff
      } else {
        val diff = tokenA.compareTo(tokenB, ignoreCase = true)
        if (diff != 0) return diff
      }
    }
    return a.length.compareTo(b.length)
  }

  /**
   * Parse archive from Context & Uri, automatically detecting ZIP, CBZ, RAR, CBR, or custom archives.
   */
  suspend fun parseArchiveFromUri(context: Context, uri: Uri, title: String): ComicBook = withContext(Dispatchers.IO) {
    val tempFile = File.createTempFile("comic_extract_", ".tmp", context.cacheDir)
    try {
      context.contentResolver.openInputStream(uri)?.use { input ->
        tempFile.outputStream().use { output ->
          input.copyTo(output)
        }
      } ?: throw IllegalStateException("Tidak dapat membuka file arsip")

      parseArchiveFromFile(tempFile, title)
    } finally {
      if (tempFile.exists()) {
        tempFile.delete()
      }
    }
  }

  /**
   * Parse archive from InputStream (kept for backward compatibility or streaming)
   */
  suspend fun parseArchive(inputStream: InputStream, title: String): ComicBook = withContext(Dispatchers.IO) {
    val bytes = inputStream.readBytes()
    val tempFile = File.createTempFile("comic_stream_", ".tmp")
    try {
      tempFile.writeBytes(bytes)
      parseArchiveFromFile(tempFile, title)
    } finally {
      if (tempFile.exists()) {
        tempFile.delete()
      }
    }
  }

  /**
   * Detect and parse archive from File, supporting ZIP/CBZ and RAR/CBR.
   */
  private fun parseArchiveFromFile(file: File, title: String): ComicBook {
    val rawFiles = mutableListOf<Pair<String, ByteArray>>()

    val isRarHeader = isRarArchive(file)
    if (isRarHeader) {
      // Extract with Junrar
      extractRar(file, rawFiles)
    } else {
      // Try extracting as ZIP first
      try {
        extractZip(file, rawFiles)
      } catch (_: Exception) {
        // Fallback to RAR if ZIP fails
        extractRar(file, rawFiles)
      }
    }

    // Sort naturally by file path
    rawFiles.sortWith { o1, o2 -> naturalCompare(o1.first, o2.first) }

    val pages = rawFiles.mapIndexed { index, pair ->
      val simpleName = pair.first.substringAfterLast('/')
      ComicPage(
        pageNumber = index,
        label = simpleName,
        imageBytes = pair.second,
      )
    }

    val cleanTitle = title.replace(Regex("\\.(cbz|zip|cbr|rar|7z)$", RegexOption.IGNORE_CASE), "")
    val comicId = "cbz_" + cleanTitle.lowercase().replace(Regex("[^a-z0-9_]"), "_")

    return ComicBook(
      id = comicId,
      title = cleanTitle.ifEmpty { "Comic Archive" },
      author = "Local Archive",
      pages = pages,
      defaultDirection = ReadingDirection.RTL,
    )
  }

  private fun isRarArchive(file: File): Boolean {
    if (file.length() < 7) return false
    val header = ByteArray(7)
    file.inputStream().use { it.read(header) }
    // RAR 1.5 - 4.x: 52 61 72 21 1A 07 00 ("Rar!\x1a\x07\x00")
    // RAR 5.0:       52 61 72 21 1A 07 01 00
    return header[0] == 0x52.toByte() &&
      header[1] == 0x61.toByte() &&
      header[2] == 0x72.toByte() &&
      header[3] == 0x21.toByte()
  }

  private fun extractZip(file: File, outList: MutableList<Pair<String, ByteArray>>) {
    ZipInputStream(file.inputStream()).use { zipStream ->
      var entry: ZipEntry? = zipStream.nextEntry
      while (entry != null) {
        val name = entry.name
        if (!entry.isDirectory && isImageEntry(name)) {
          val buffer = ByteArray(8192)
          val baos = ByteArrayOutputStream()
          var len: Int
          while (zipStream.read(buffer).also { len = it } != -1) {
            baos.write(buffer, 0, len)
          }
          val bytes = baos.toByteArray()
          if (isImageEntry(name, bytes)) {
            outList.add(Pair(name, bytes))
          }
        }
        zipStream.closeEntry()
        entry = zipStream.nextEntry
      }
    }
  }

  private fun extractRar(file: File, outList: MutableList<Pair<String, ByteArray>>) {
    try {
      Archive(file).use { archive ->
        var header = archive.nextFileHeader()
        while (header != null) {
          if (!header.isDirectory) {
            val name = header.fileName
            if (isImageEntry(name)) {
              val baos = ByteArrayOutputStream()
              archive.extractFile(header, baos)
              val bytes = baos.toByteArray()
              if (isImageEntry(name, bytes)) {
                outList.add(Pair(name, bytes))
              }
            }
          }
          header = archive.nextFileHeader()
        }
      }
    } catch (_: Exception) {
      // Silently catch unrar issues if file is encrypted or corrupted
    }
  }

  /**
   * Import batch images of ANY format from list of URIs.
   */
  suspend fun createFromUris(context: Context, uris: List<Uri>): ComicBook = withContext(Dispatchers.IO) {
    val items = mutableListOf<Pair<String, ByteArray>>()
    for (uri in uris) {
      val name = uri.lastPathSegment ?: "image_${items.size}.jpg"
      context.contentResolver.openInputStream(uri)?.use { stream ->
        val baos = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var len: Int
        while (stream.read(buffer).also { len = it } != -1) {
          baos.write(buffer, 0, len)
        }
        val bytes = baos.toByteArray()
        if (bytes.isNotEmpty()) {
          items.add(Pair(name, bytes))
        }
      }
    }

    items.sortWith { o1, o2 -> naturalCompare(o1.first, o2.first) }

    val pages = items.mapIndexed { index, pair ->
      ComicPage(
        pageNumber = index,
        label = pair.first,
        imageBytes = pair.second,
      )
    }

    val firstIdentifier = items.firstOrNull()?.first ?: "custom"
    val comicId = "gallery_" + kotlin.math.abs(firstIdentifier.hashCode()) + "_" + pages.size

    ComicBook(
      id = comicId,
      title = "Custom Gallery (${pages.size} Hal)",
      author = "Local Storage",
      pages = pages,
      defaultDirection = ReadingDirection.RTL,
    )
  }

  fun getEmptyComic(): ComicBook {
    return ComicBook(
      id = "empty",
      title = "Tidak ada komik",
      author = "-",
      defaultDirection = ReadingDirection.RTL,
      pages = emptyList()
    )
  }
}
