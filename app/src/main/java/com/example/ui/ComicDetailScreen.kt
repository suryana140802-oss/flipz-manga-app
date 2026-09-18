package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.ComicDatabase
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.ReadChapterEntity
import com.example.domain.model.ComicDetail
import com.example.network.CloudflareException
import com.example.network.MgkomikScraper
import com.example.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ComicDetailScreen(
    comicUrl: String,
    scraper: MgkomikScraper,
    onChapterClick: (com.example.domain.model.Chapter, ComicDetail) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val db = remember(context) { ComicDatabase.getInstance(context) }
    val bookmarkDao = remember(db) { db.bookmarkDao() }
    val readChapterDao = remember(db) { db.readChapterDao() }

    val isBookmarked by bookmarkDao.isBookmarked(comicUrl).collectAsState(initial = false)
    val readChapterUrls by readChapterDao.getReadChapterUrlsForComic(comicUrl).collectAsState(initial = emptyList())
    val downloadedChapterDao = remember(db) { db.downloadedChapterDao() }
    val downloadedChapters by downloadedChapterDao.getDownloadedChaptersForComic(comicUrl).collectAsState(initial = emptyList())
    val downloadedUrls = remember(downloadedChapters) { downloadedChapters.map { it.chapterUrl }.toSet() }
    val downloadProgressMap by com.example.data.repository.DownloadManager.downloadProgress.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<ComicDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var needsCloudflare by remember { mutableStateOf(false) }
    var isSynopsisExpanded by remember { mutableStateOf(false) }

    fun loadDetail() {
        coroutineScope.launch {
            isLoading = true
            needsCloudflare = false
            try {
                detail = scraper.getComicDetail(comicUrl)
            } catch (e: CloudflareException) {
                needsCloudflare = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(comicUrl) {
        loadDetail()
    }

    if (needsCloudflare) {
        CloudflareSolverScreen(
            url = comicUrl,
            onSolved = { loadDetail() }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.deskDark)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else if (detail != null) {
            val d = detail!!
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // --- 1. Immersive Hero Cover Section ---
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    ) {
                        val coverRequest = ImageRequest.Builder(context)
                            .data(d.thumbnailUrl)
                            .addHeader("Referer", "https://web1.mgkomik.cc/")
                            .crossfade(true)
                            .build()

                        // Ambient blurred background cover
                        AsyncImage(
                            model = coverRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Multi-stop modern gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Black.copy(alpha = 0.5f),
                                            0.4f to colors.deskDark.copy(alpha = 0.55f),
                                            0.85f to colors.deskDark.copy(alpha = 0.95f),
                                            1.0f to colors.deskDark
                                        )
                                    )
                                )
                        )

                        // Floating Glass Back button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .statusBarsPadding()
                                .padding(12.dp)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Hero Content: Cover Thumbnail + Title + Quick Actions
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Floating Artwork with rounded borders & soft glow
                            Box(
                                modifier = Modifier
                                    .width(108.dp)
                                    .height(154.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.22f)), RoundedCornerShape(16.dp))
                            ) {
                                AsyncImage(
                                    model = coverRequest,
                                    contentDescription = "Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Metadata Column
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(bottom = 2.dp)
                            ) {
                                Text(
                                    text = d.title,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    lineHeight = 25.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Total Chapters Pill
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = colors.primary.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "${d.chapters.size} Chapter",
                                            color = colors.primary,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }

                                    // Bookmark Interactive Pill
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isBookmarked) Color(0xFFF43F5E).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isBookmarked) Color(0xFFF43F5E).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable {
                                                coroutineScope.launch {
                                                    if (isBookmarked) {
                                                        bookmarkDao.deleteBookmark(comicUrl)
                                                    } else {
                                                        bookmarkDao.insertBookmark(
                                                            BookmarkEntity(
                                                                comicUrl = comicUrl,
                                                                title = d.title,
                                                                coverUrl = d.thumbnailUrl,
                                                                latestChapter = d.chapters.firstOrNull()?.title ?: ""
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isBookmarked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                                contentDescription = if (isBookmarked) "Tersimpan" else "Bookmark",
                                                tint = if (isBookmarked) Color(0xFFF43F5E) else Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isBookmarked) "Tersimpan" else "Bookmark",
                                                color = if (isBookmarked) Color(0xFFFDA4AF) else Color.White,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 2. Genre Tags Section ---
                if (d.genres.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                d.genres.forEach { genre ->
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = colors.primary.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = genre,
                                            color = colors.primary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 3. Synopsis Card Section ---
                if (d.synopsis.isNotBlank()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = colors.deskMedium.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, colors.hudBorder.copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(16.dp)
                                            .background(colors.primary, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Sinopsis",
                                        color = colors.textPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = d.synopsis,
                                    color = colors.textSecondary,
                                    fontSize = 13.5.sp,
                                    lineHeight = 21.sp,
                                    maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 4,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (d.synopsis.length > 180) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isSynopsisExpanded) "Sembunyikan ▲" else "Baca Selengkapnya ▼",
                                        color = colors.primary,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // --- 4. Chapter List Header ---
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(16.dp)
                                .background(colors.primary, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Daftar Chapter",
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colors.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "${d.chapters.size} Chapter",
                                color = colors.primary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // --- 5. Chapter Items (Modern Cards) ---
                items(d.chapters) { chapter ->
                    val isRead = readChapterUrls.contains(chapter.url)
                    val isDownloaded = downloadedUrls.contains(chapter.url)
                    val downloadProg = downloadProgressMap[chapter.url]

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                coroutineScope.launch {
                                    readChapterDao.markChapterAsRead(
                                        ReadChapterEntity(
                                            chapterUrl = chapter.url,
                                            comicUrl = comicUrl,
                                            chapterTitle = chapter.title
                                        )
                                    )
                                }
                                onChapterClick(chapter, d)
                            },
                        color = if (isRead) colors.deskMedium.copy(alpha = 0.38f) else colors.deskMedium.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isRead) colors.hudBorder.copy(alpha = 0.15f) else colors.hudBorder.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            // Leading chapter icon pill
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isRead) colors.deskHighlight.copy(alpha = 0.3f)
                                        else colors.primary.copy(alpha = 0.12f),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                    contentDescription = null,
                                    tint = if (isRead) colors.textMuted.copy(alpha = 0.6f) else colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Chapter Title & Date Column
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chapter.title,
                                    color = if (isRead) colors.textMuted.copy(alpha = 0.55f) else colors.textPrimary,
                                    fontSize = 14.5.sp,
                                    fontWeight = if (isRead) FontWeight.Normal else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (chapter.date.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = chapter.date,
                                        color = colors.textMuted.copy(alpha = if (isRead) 0.4f else 0.8f),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // "Dibaca" Indicator Chip
                            if (isRead) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = colors.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = colors.primary,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Dibaca",
                                            color = colors.primary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            // ── Download Action Button (Maintained at trailing edge) ──
                            if (downloadProg != null) {
                                Box(
                                    modifier = Modifier.size(36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        progress = { downloadProg },
                                        modifier = Modifier.size(20.dp),
                                        color = colors.primary,
                                        strokeWidth = 2.dp
                                    )
                                }
                            } else if (isDownloaded) {
                                IconButton(
                                    onClick = {
                                        com.example.data.repository.DownloadManager.deleteDownloadedChapter(context, chapter.url, db)
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.OfflinePin,
                                        contentDescription = "Tersimpan offline (Klik untuk hapus)",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        com.example.data.repository.DownloadManager.downloadChapter(
                                            context = context,
                                            okHttpClient = scraper.okHttpClient,
                                            scraper = scraper,
                                            comicUrl = comicUrl,
                                            comicTitle = d.title,
                                            coverUrl = d.thumbnailUrl,
                                            chapter = chapter,
                                            db = db
                                        )
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DownloadForOffline,
                                        contentDescription = "Download Chapter Offline",
                                        tint = colors.textSecondary.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom padding
                item { Spacer(modifier = Modifier.height(36.dp)) }
            }
        }
    }
}
