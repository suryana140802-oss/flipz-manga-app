package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.widget.Toast
import com.example.ui.CloudflareSolverScreen
import com.example.ui.HomeScreen
import com.example.ui.OnlineComicsScreen
import com.example.ui.ComicDetailScreen
import com.example.ui.ReaderScreen
import com.example.ui.ReaderViewModel
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.MyApplicationTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class ScreenState {
    HOME,
    READER,
    ONLINE_CATALOG,
    COMIC_DETAIL
}

class MainActivity : ComponentActivity() {

    private val readerViewModel: ReaderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inisialisasi cookie cache untuk Cloudflare bypass
        com.example.network.CookieCache.init(this)

        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor(com.example.network.CloudflareInterceptor())
            .build()
        val scraper = com.example.network.MgkomikScraper(okHttpClient)
        val database = com.example.data.ComicDatabase.getInstance(this)
        val onlineViewModel = com.example.ui.OnlineViewModel(scraper, database.bookmarkDao())

        coil.Coil.setImageLoader(
            coil.ImageLoader.Builder(this)
                .okHttpClient(okHttpClient)
                .build()
        )

        // Inisialisasi Start.io Ads Monetization (Live Ads: testMode = false untuk rilis publik)
        com.example.monetization.StartIoAdsManager.initialize(this, testMode = false)

        // Jadwalkan pengecekan rilis chapter baru di latar belakang (WorkManager)
        com.example.worker.NotificationScheduler.schedulePeriodicChapterCheck(applicationContext)

        val notificationComicUrl = intent?.getStringExtra("OPEN_COMIC_URL")
        val forceCheckUpdate = intent?.getBooleanExtra("SHOW_UPDATE_DIALOG", false) == true

        setContent {
            var isDarkTheme by remember { mutableStateOf(true) } // default: dark mode
            var currentScreen by remember {
                mutableStateOf(if (!notificationComicUrl.isNullOrBlank()) ScreenState.COMIC_DETAIL else ScreenState.HOME)
            }
            var selectedComicUrl by remember {
                mutableStateOf(notificationComicUrl ?: "")
            }
            var availableUpdate by remember { mutableStateOf<com.example.updater.AppUpdateInfo?>(null) }
            val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

            // Pemeriksaan pembaruan aplikasi otomatis (GitHub Releases API)
            androidx.compose.runtime.LaunchedEffect(Unit) {
                try {
                    val info = com.example.updater.AppUpdateManager.checkForUpdate(this@MainActivity)
                    if (info != null && (info.isUpdateAvailable || forceCheckUpdate)) {
                        availableUpdate = info
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Gagal cek update: ${e.message}")
                }
            }

            androidx.activity.compose.BackHandler(enabled = currentScreen != ScreenState.HOME) {
                if (currentScreen == ScreenState.COMIC_DETAIL) {
                    currentScreen = ScreenState.ONLINE_CATALOG
                } else if (currentScreen == ScreenState.READER) {
                    currentScreen = if (selectedComicUrl.isNotEmpty()) ScreenState.COMIC_DETAIL else ScreenState.HOME
                } else {
                    currentScreen = ScreenState.HOME
                }
            }

            val archivePicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    readerViewModel.loadFromArchiveUri(uri)
                    currentScreen = ScreenState.READER
                }
            }

            val imagesPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenMultipleDocuments()
            ) { uris ->
                if (uris.isNotEmpty()) {
                    readerViewModel.loadFromImageUris(uris)
                    currentScreen = ScreenState.READER
                }
            }

            var needsCloudflareBypass by remember { mutableStateOf(false) }
            var cloudflareChallengeUrl by remember { mutableStateOf("https://web1.mgkomik.cc/") }
            var pendingChapterAction by remember { mutableStateOf<(() -> Unit)?>(null) }
            var isLoadingChapter by remember { mutableStateOf(false) }

            fun openChapter(
                comicId: String,
                comicTitle: String,
                comicCoverUrl: String,
                comicDetailUrl: String,
                chapterUrl: String,
                chapterTitle: String
            ) {
                if (chapterUrl.isBlank()) return
                selectedComicUrl = comicId.ifBlank { comicDetailUrl }

                com.example.monetization.StartIoAdsManager.onChapterOpened(this@MainActivity) {
                    coroutineScope.launch {
                        isLoadingChapter = true
                        try {
                            val offlinePages = com.example.data.repository.DownloadManager.getOfflinePages(chapterUrl, database)
                            val pages = offlinePages ?: scraper.getChapterPages(chapterUrl)

                            if (pages.isEmpty()) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Tidak dapat memuat halaman chapter. Silakan coba lagi.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                isLoadingChapter = false
                                return@launch
                            }

                            val comicPages = pages.mapIndexed { i, url ->
                                com.example.model.ComicPage(pageNumber = i, label = "Page $i", imageUrl = url)
                            }
                            val comicBook = com.example.model.ComicBook(
                                id = comicId.ifBlank { chapterUrl },
                                title = comicTitle,
                                author = "",
                                pages = comicPages,
                                coverUrl = comicCoverUrl,
                                comicUrl = comicDetailUrl,
                                chapterUrl = chapterUrl,
                                chapterTitle = chapterTitle
                            )
                            readerViewModel.loadComicBook(comicBook)
                            currentScreen = ScreenState.READER
                        } catch (e: com.example.network.CloudflareException) {
                            com.example.network.CookieCache.clear()
                            cloudflareChallengeUrl = chapterUrl.ifBlank { "https://web1.mgkomik.cc/" }
                            pendingChapterAction = {
                                openChapter(comicId, comicTitle, comicCoverUrl, comicDetailUrl, chapterUrl, chapterTitle)
                            }
                            needsCloudflareBypass = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(
                                this@MainActivity,
                                "Gagal memuat chapter: ${e.localizedMessage ?: "Periksa koneksi internet"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } finally {
                            isLoadingChapter = false
                        }
                    }
                }
            }

            fun resumeReading(progress: com.example.data.entity.ReadingProgressEntity) {
                if (progress.chapterUrl.isBlank()) return
                openChapter(
                    comicId = progress.comicId,
                    comicTitle = progress.comicTitle,
                    comicCoverUrl = progress.coverUrl,
                    comicDetailUrl = progress.comicId,
                    chapterUrl = progress.chapterUrl,
                    chapterTitle = progress.chapterTitle
                )
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LocalAppColors.current.deskDark,
                ) {
                    if (needsCloudflareBypass) {
                        CloudflareSolverScreen(
                            url = cloudflareChallengeUrl,
                            onSolved = {
                                needsCloudflareBypass = false
                                val action = pendingChapterAction
                                pendingChapterAction = null
                                action?.invoke()
                            }
                        )
                    } else {
                        when (currentScreen) {
                            ScreenState.HOME -> {
                                HomeScreen(
                                    onNavigateToReaderOnline = {
                                        currentScreen = ScreenState.ONLINE_CATALOG
                                    },
                                    onImportArchiveClick = { archivePicker.launch(arrayOf("*/*")) },
                                    onImportImagesClick = { imagesPicker.launch(arrayOf("image/*")) },
                                    onSupportClick = {
                                        com.example.monetization.MonetagManager.openDirectLink(this@MainActivity)
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Terima kasih banyak atas dukunganmu untuk Flipz Manga! ❤️",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    },
                                    onResumeReading = { resumeReading(it) }
                                )
                            }
                            ScreenState.ONLINE_CATALOG -> {
                                OnlineComicsScreen(
                                    viewModel = onlineViewModel,
                                    isDarkTheme = isDarkTheme,
                                    onThemeToggle = { isDarkTheme = !isDarkTheme },
                                    onResumeReading = { resumeReading(it) },
                                    onComicClick = { url ->
                                        selectedComicUrl = url
                                        currentScreen = ScreenState.COMIC_DETAIL
                                    }
                                )
                            }
                            ScreenState.COMIC_DETAIL -> {
                                ComicDetailScreen(
                                    comicUrl = selectedComicUrl,
                                    scraper = scraper,
                                    onChapterClick = { chapter, detail ->
                                        openChapter(
                                            comicId = detail.url.ifBlank { chapter.url },
                                            comicTitle = detail.title,
                                            comicCoverUrl = detail.thumbnailUrl,
                                            comicDetailUrl = detail.url,
                                            chapterUrl = chapter.url,
                                            chapterTitle = chapter.title
                                        )
                                    },
                                    onBack = { currentScreen = ScreenState.ONLINE_CATALOG }
                                )
                            }
                            ScreenState.READER -> {
                                ReaderScreen(
                                    viewModel = readerViewModel,
                                    onBack = {
                                        currentScreen = if (selectedComicUrl.isNotEmpty()) ScreenState.COMIC_DETAIL else ScreenState.HOME
                                    }
                                )
                            }
                        }

                        if (isLoadingChapter) {
                            Dialog(onDismissRequest = { /* Loading in progress */ }) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                    color = LocalAppColors.current.deskMedium,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, LocalAppColors.current.hudBorder),
                                    shadowElevation = 8.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = LocalAppColors.current.primary,
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 3.dp
                                        )
                                        Text(
                                            text = "Membuka chapter...",
                                            color = LocalAppColors.current.textPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Dialog Pembaruan Aplikasi
                availableUpdate?.let { updateInfo ->
                    com.example.ui.widgets.UpdateDialog(
                        updateInfo = updateInfo,
                        onUpdateClick = {
                            val url = updateInfo.downloadUrl
                            val ver = updateInfo.latestVersion
                            availableUpdate = null
                            com.example.updater.AppUpdateManager.startDownloadAndInstall(
                                context = this@MainActivity,
                                downloadUrl = url,
                                versionName = ver
                            )
                        },
                        onDismissRequest = {
                            availableUpdate = null
                        }
                    )
                }
            }
        }
    }
}
