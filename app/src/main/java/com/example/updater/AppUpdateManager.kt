package com.example.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Informasi rilis versi terbaru aplikasi.
 */
data class AppUpdateInfo(
    val latestVersion: String,
    val releaseTitle: String,
    val changelog: String,
    val downloadUrl: String,
    val releaseHtmlUrl: String,
    val isUpdateAvailable: Boolean
)

/**
 * Manager pengecekan dan instalasi pembaruan aplikasi secara gratis via GitHub Releases API.
 */
object AppUpdateManager {
    private const val TAG = "AppUpdateManager"
    private const val GITHUB_RELEASE_API = "https://api.github.com/repos/suryana140802-oss/flipz-manga-app/releases/latest"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Memeriksa rilis versi terbaru dari GitHub Releases.
     */
    suspend fun checkForUpdate(context: Context): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_RELEASE_API)
                .header("User-Agent", "FlipzManga-AndroidApp")
                .header("Accept", "application/vnd.github+json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Gagal memeriksa update: HTTP ${response.code}")
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            val json = JSONObject(responseBody)

            val rawTag = json.optString("tag_name", "")
            val releaseTitle = json.optString("name", "Flipz Manga Update")
            val changelog = json.optString("body", "Peningkatan performa dan pembaruan fitur.")
            val releaseHtmlUrl = json.optString("html_url", "https://github.com/suryana140802-oss/flipz-manga-app/releases")

            val latestVersion = rawTag.removePrefix("v").trim()
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()

            val isAvailable = isNewerVersion(currentVersion, latestVersion)

            // Seleksi asset APK terbaik berdasarkan arsitektur perangkat
            val assetsArray = json.optJSONArray("assets")
            var downloadUrl = ""

            if (assetsArray != null && assetsArray.length() > 0) {
                val isArm64 = Build.SUPPORTED_ABIS.contains("arm64-v8a")
                var preferredAssetUrl = ""
                var universalAssetUrl = ""
                var firstApkUrl = ""

                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val assetName = asset.optString("name", "").lowercase()
                    val assetDownload = asset.optString("browser_download_url", "")

                    if (assetName.endsWith(".apk")) {
                        if (firstApkUrl.isEmpty()) firstApkUrl = assetDownload
                        if (assetName.contains("arm64")) preferredAssetUrl = assetDownload
                        if (assetName.contains("universal")) universalAssetUrl = assetDownload
                    }
                }

                downloadUrl = when {
                    isArm64 && preferredAssetUrl.isNotEmpty() -> preferredAssetUrl
                    universalAssetUrl.isNotEmpty() -> universalAssetUrl
                    else -> firstApkUrl
                }
            }

            if (downloadUrl.isEmpty()) {
                downloadUrl = releaseHtmlUrl
            }

            Log.i(TAG, "Cek update: Current v$currentVersion vs Latest v$latestVersion | Update Available: $isAvailable")

            AppUpdateInfo(
                latestVersion = latestVersion,
                releaseTitle = releaseTitle,
                changelog = changelog,
                downloadUrl = downloadUrl,
                releaseHtmlUrl = releaseHtmlUrl,
                isUpdateAvailable = isAvailable
            )
        } catch (e: Exception) {
            Log.e(TAG, "Kesalahan saat memeriksa pembaruan: ${e.message}", e)
            null
        }
    }

    /**
     * Membandingkan dua string versi semantik (contoh: "2.1" vs "2.2").
     */
    fun isNewerVersion(current: String, latest: String): Boolean {
        if (current.isBlank() || latest.isBlank()) return false
        if (current == latest) return false

        try {
            val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
            val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(currentParts.size, latestParts.size)
            for (i in 0 until maxLen) {
                val curr = currentParts.getOrElse(i) { 0 }
                val lat = latestParts.getOrElse(i) { 0 }
                if (lat > curr) return true
                if (lat < curr) return false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal membandingkan versi: $e")
        }
        return false
    }

    /**
     * Memulai pengunduhan APK dan memicu jendela instalasi.
     */
    fun startDownloadAndInstall(context: Context, downloadUrl: String, versionName: String) {
        if (downloadUrl.isBlank()) return

        if (!downloadUrl.endsWith(".apk")) {
            openInBrowser(context, downloadUrl)
            return
        }

        try {
            val fileName = "FlipzManga-v$versionName.apk"
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (downloadManager == null) {
                openInBrowser(context, downloadUrl)
                return
            }

            // Hapus file lama jika ada
            val destinationDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val destinationFile = File(destinationDir, fileName)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Flipz Manga v$versionName")
                setDescription("Mengunduh pembaruan aplikasi...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadId = downloadManager.enqueue(request)

            val onCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(recvContext: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id == downloadId) {
                        try {
                            recvContext?.unregisterReceiver(this)
                        } catch (_: Exception) {}

                        installDownloadedApk(context, fileName)
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengunduh via DownloadManager: ${e.message}, membuka browser...")
            openInBrowser(context, downloadUrl)
        }
    }

    /**
     * Membuka installer Android untuk file APK yang telah diunduh.
     */
    fun installDownloadedApk(context: Context, fileName: String) {
        try {
            val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (!apkFile.exists()) {
                Log.e(TAG, "File APK tidak ditemukan: ${apkFile.absolutePath}")
                return
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menjalankan installer APK: ${e.message}", e)
        }
    }

    fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membuka tautan: ${e.message}")
        }
    }
}
