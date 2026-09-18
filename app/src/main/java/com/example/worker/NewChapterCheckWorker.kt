package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.ComicDatabase
import com.example.network.MgkomikScraper
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class NewChapterCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = ComicDatabase.getInstance(context)
            val bookmarks = database.bookmarkDao().getAllBookmarksList()
            if (bookmarks.isEmpty()) return Result.success()

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            val scraper = MgkomikScraper(okHttpClient)

            // Check up to 10 bookmarks per periodic run to preserve battery and data
            val bookmarksToCheck = bookmarks.take(10)

            for (bookmark in bookmarksToCheck) {
                try {
                    val detail = scraper.getComicDetail(bookmark.comicUrl)
                    val latestOnlineChapter = detail.chapters.firstOrNull() ?: continue

                    // Check if a new chapter is available
                    val previousLatest = bookmark.latestChapter.trim()
                    val newLatest = latestOnlineChapter.title.trim()

                    if (previousLatest.isNotBlank() && newLatest.isNotBlank() && previousLatest != newLatest) {
                        // Update latest chapter in database
                        database.bookmarkDao().insertBookmark(
                            bookmark.copy(latestChapter = newLatest)
                        )

                        // Show notification
                        showNewChapterNotification(
                            comicTitle = bookmark.title,
                            chapterTitle = newLatest,
                            comicUrl = bookmark.comicUrl
                        )
                    } else if (previousLatest.isBlank() && newLatest.isNotBlank()) {
                        // First time recording latest chapter
                        database.bookmarkDao().insertBookmark(
                            bookmark.copy(latestChapter = newLatest)
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun showNewChapterNotification(
        comicTitle: String,
        chapterTitle: String,
        comicUrl: String
    ) {
        val channelId = "flipz_new_chapters"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Update Komik Favorit",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi otomatis saat komik favorit merilis chapter baru"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_COMIC_URL", comicUrl)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            comicUrl.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Chapter Baru Rilis! 📖")
            .setContentText("$comicTitle: $chapterTitle sudah tersedia! Baca sekarang.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$comicTitle: $chapterTitle baru saja rilis di Flipz Manga! Ketuk untuk langsung membaca.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(comicUrl.hashCode(), notification)
    }
}
