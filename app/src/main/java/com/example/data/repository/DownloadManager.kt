package com.example.data.repository

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.data.ComicDatabase
import com.example.data.entity.DownloadedChapterEntity
import com.example.domain.model.Chapter
import com.example.network.MgkomikScraper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object DownloadManager {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Maps chapterUrl -> progress (0.0f to 1.0f)
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    fun downloadChapter(
        context: Context,
        okHttpClient: OkHttpClient,
        scraper: MgkomikScraper,
        comicUrl: String,
        comicTitle: String,
        coverUrl: String,
        chapter: Chapter,
        db: ComicDatabase
    ) {
        if (_downloadProgress.value.containsKey(chapter.url)) return

        _downloadProgress.update { it + (chapter.url to 0.05f) }

        scope.launch {
            try {
                val pageUrls = scraper.getChapterPages(chapter.url)
                if (pageUrls.isEmpty()) {
                    showToast(context, "Gagal mendapatkan halaman chapter untuk didownload")
                    _downloadProgress.update { it - chapter.url }
                    return@launch
                }

                val safeComic = comicTitle.replace(Regex("[^a-zA-Z0-9_]"), "_").take(30).ifBlank { "comic" }
                val safeChapter = chapter.title.replace(Regex("[^a-zA-Z0-9_]"), "_").take(30).ifBlank { "ch" }
                val downloadDir = File(context.getExternalFilesDir("downloads"), "$safeComic/$safeChapter")
                if (!downloadDir.exists()) downloadDir.mkdirs()

                var successCount = 0
                for ((index, pageUrl) in pageUrls.withIndex()) {
                    val file = File(downloadDir, "page_%04d.jpg".format(index))
                    if (!file.exists() || file.length() == 0L) {
                        val request = Request.Builder()
                            .url(pageUrl)
                            .header("Referer", "https://web1.mgkomik.cc/")
                            .build()

                        okHttpClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                response.body?.byteStream()?.use { input ->
                                    FileOutputStream(file).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                successCount++
                            }
                        }
                    } else {
                        successCount++
                    }

                    val progress = (index + 1).toFloat() / pageUrls.size
                    _downloadProgress.update { it + (chapter.url to progress) }
                }

                if (successCount > 0) {
                    val entity = DownloadedChapterEntity(
                        chapterUrl = chapter.url,
                        comicUrl = comicUrl,
                        comicTitle = comicTitle,
                        chapterTitle = chapter.title,
                        coverUrl = coverUrl,
                        localDirectoryPath = downloadDir.absolutePath,
                        pageCount = successCount,
                        downloadedAt = System.currentTimeMillis()
                    )
                    db.downloadedChapterDao().insertDownloadedChapter(entity)
                    showToast(context, "Selesai mendownload ${chapter.title}! Siap dibaca offline 📥")
                } else {
                    showToast(context, "Gagal mendownload gambar chapter")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast(context, "Error download: ${e.message}")
            } finally {
                _downloadProgress.update { it - chapter.url }
            }
        }
    }

    fun deleteDownloadedChapter(
        context: Context,
        chapterUrl: String,
        db: ComicDatabase
    ) {
        scope.launch {
            try {
                val entity = db.downloadedChapterDao().getDownloadedChapter(chapterUrl)
                if (entity != null) {
                    val dir = File(entity.localDirectoryPath)
                    if (dir.exists()) {
                        dir.deleteRecursively()
                    }
                    db.downloadedChapterDao().deleteDownloadedChapter(chapterUrl)
                    showToast(context, "Chapter offline berhasil dihapus")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getOfflinePages(chapterUrl: String, db: ComicDatabase): List<String>? {
        return withContext(Dispatchers.IO) {
            val entity = db.downloadedChapterDao().getDownloadedChapter(chapterUrl) ?: return@withContext null
            val dir = File(entity.localDirectoryPath)
            if (!dir.exists()) return@withContext null

            val files = dir.listFiles { file ->
                val name = file.name.lowercase()
                name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")
            }?.sortedBy { it.name } ?: return@withContext null

            if (files.isEmpty()) return@withContext null

            files.map { it.absolutePath }
        }
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}
