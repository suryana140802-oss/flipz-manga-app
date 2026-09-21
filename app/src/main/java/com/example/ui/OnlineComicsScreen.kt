package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Download
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ComicDatabase
import com.example.domain.model.Comic
import com.example.ui.theme.LocalAppColors
import com.example.ui.widgets.ContinueReadingCard
import com.example.ui.widgets.DockItem
import com.example.ui.widgets.EmptyStateView
import com.example.ui.widgets.FloatingNavigationDock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineComicsScreen(
    viewModel: OnlineViewModel,
    isDarkTheme: Boolean = true,
    onThemeToggle: () -> Unit = {},
    onResumeReading: ((com.example.data.entity.ReadingProgressEntity) -> Unit)? = null,
    onComicClick: (String) -> Unit
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val db = remember(context) { ComicDatabase.getInstance(context) }
    val latestProgress by db.readingProgressDao().getLatestProgress().collectAsState(initial = null)

    val comics by viewModel.comics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val needsCloudflareBypass by viewModel.needsCloudflareBypass.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val activeStatus by viewModel.activeStatusFilter.collectAsState()
    val activeType by viewModel.activeTypeFilter.collectAsState()
    val activeGenre by viewModel.activeGenreFilter.collectAsState()

    var searchExpanded by remember { mutableStateOf(false) }
    var showGenreSheet by remember { mutableStateOf(false) }
    var isBackupCardVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBookmarks(
                context = context,
                uri = uri,
                onSuccess = { count ->
                    Toast.makeText(context, "Berhasil mencadangkan $count komik favorit! 📁", Toast.LENGTH_SHORT).show()
                },
                onError = { error ->
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBookmarks(
                context = context,
                uri = uri,
                onSuccess = { count ->
                    Toast.makeText(context, "Berhasil memulihkan $count komik ke Favorit! 🎉", Toast.LENGTH_SHORT).show()
                },
                onError = { error ->
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    if (needsCloudflareBypass) {
        CloudflareSolverScreen(onSolved = { viewModel.onCloudflareBypassSuccess() })
        return
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) searchExpanded = true
    }

    val dockItems: List<DockItem<ComicFilter>> = remember {
        listOf(
            DockItem(ComicFilter.Latest, "Terbaru", Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday),
            DockItem(ComicFilter.Popular, "Populer", Icons.Filled.LocalFireDepartment, Icons.Outlined.LocalFireDepartment),
            DockItem(ComicFilter.Ongoing, "Ongoing", Icons.Filled.AutoStories, Icons.Outlined.AutoStories),
            DockItem(ComicFilter.Completed, "Tamat", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircleOutline),
            DockItem(ComicFilter.Bookmark, "Favorit", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.deskDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── Top Bar ──────────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = if (isSearchActive) "Hasil Pencarian" else if (activeStatus == ComicFilter.Bookmark) "Koleksi Favorit" else "Flipz Manga",
                    color = colors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )

            // Switch Dark/Light Mode
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.deskMedium)
                    .border(1.dp, colors.hudBorder, CircleShape)
                    .clickable(onClick = onThemeToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                    contentDescription = "Ganti Tema",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            // Filter Genre Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (activeGenre != null) colors.primary.copy(alpha = 0.18f)
                        else colors.deskMedium
                    )
                    .border(
                        1.dp,
                        if (activeGenre != null) colors.primary.copy(alpha = 0.6f)
                        else colors.hudBorder,
                        CircleShape
                    )
                    .clickable { showGenreSheet = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "Filter Genre",
                    tint = if (activeGenre != null) colors.primary else colors.textSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            // Search Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (searchExpanded || isSearchActive) colors.primary.copy(alpha = 0.18f)
                        else colors.deskMedium
                    )
                    .border(
                        1.dp,
                        if (searchExpanded || isSearchActive) colors.primary.copy(alpha = 0.45f)
                        else colors.hudBorder,
                        CircleShape
                    )
                    .clickable {
                        searchExpanded = !searchExpanded
                        if (!searchExpanded) { viewModel.clearSearch(); focusManager.clearFocus() }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (searchExpanded && searchQuery.isNotEmpty()) Icons.Rounded.Clear
                                  else Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = if (searchExpanded || isSearchActive) colors.primary else colors.textSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // ── Collapsible Search Bar ────────────────────────────────────────────
        AnimatedVisibility(
            visible = searchExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp)
                    .focusRequester(focusRequester),
                placeholder = {
                    Text("Cari judul manga, manhwa, donghua...", color = colors.textMuted, fontSize = 13.sp)
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = colors.textMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.hudBorder,
                    focusedContainerColor = colors.deskMedium,
                    unfocusedContainerColor = colors.deskMedium,
                    cursorColor = colors.primary,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                )
            )
            LaunchedEffect(searchExpanded) {
                if (searchExpanded) focusRequester.requestFocus()
            }
        }

        // ── Lanjutkan Membaca Banner (Only when not searching & progress exists) ─
        val resumeItem = latestProgress
        if (!isSearchActive && onResumeReading != null && resumeItem != null && resumeItem.chapterUrl.isNotBlank()) {
            ContinueReadingCard(
                progress = resumeItem,
                onResume = { onResumeReading(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // ── Active Genre Indicator (When genre is selected) ────────────────────
        if (!isSearchActive && activeGenre != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.primary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Genre: $activeGenre",
                            color = colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Hapus filter genre",
                            tint = colors.primary,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { viewModel.setGenreFilter(null) }
                        )
                    }
                }
            }
        }

        // ── Type Filter Pills (only when not searching) ─────────────────────
        if (!isSearchActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val allTypes = listOf<ComicFilter?>(null) + typeFilters
                allTypes.forEach { filter ->
                    val selected = if (filter == null) activeType == null else activeType == filter
                    val label = filter?.label ?: "Semua"

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) colors.primary
                                else colors.deskMedium
                            )
                            .then(
                                if (!selected) Modifier.border(1.dp, colors.hudBorder, CircleShape)
                                else Modifier
                            )
                            .clickable {
                                viewModel.setTypeFilter(if (filter == null || !selected) filter else null)
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // ── Backup & Restore Favorit Header & Card (Only when on Bookmark tab & not searching) ──
        if (activeStatus == ComicFilter.Bookmark && !isSearchActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Daftar Favorit (${comics.size})",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isBackupCardVisible) colors.primary.copy(alpha = 0.15f) else colors.deskMedium,
                    border = BorderStroke(1.dp, if (isBackupCardVisible) colors.primary.copy(alpha = 0.4f) else colors.hudBorder),
                    modifier = Modifier.clickable { isBackupCardVisible = !isBackupCardVisible }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isBackupCardVisible) Icons.Rounded.Close else Icons.Rounded.Upload,
                            contentDescription = null,
                            tint = if (isBackupCardVisible) colors.primary else colors.textMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isBackupCardVisible) "Tutup Menu Cadangan" else "Cadangkan / Pulihkan",
                            color = if (isBackupCardVisible) colors.primary else colors.textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isBackupCardVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = colors.deskMedium,
                    border = BorderStroke(1.dp, colors.hudBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cadangan Favorit",
                                color = colors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Simpan atau pulihkan daftar favorit (JSON)",
                                color = colors.textMuted,
                                fontSize = 10.sp
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { exportLauncher.launch("flipz_favorit_backup.json") },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f)),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Rounded.Upload, contentDescription = null, tint = colors.primary, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Ekspor", color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Impor", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            IconButton(
                                onClick = { isBackupCardVisible = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Sembunyikan",
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Grid Content ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (comics.isEmpty() && isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            } else if (comics.isEmpty() && isSearchActive) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateView(
                        title = "Komik Tidak Ditemukan",
                        subtitle = "Tidak ada komik yang cocok dengan \"$searchQuery\". Coba cari dengan judul lain.",
                        actionButtonText = "Hapus Pencarian",
                        onActionClick = { viewModel.clearSearch(); focusManager.clearFocus() }
                    )
                }
            } else if (comics.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (activeStatus == ComicFilter.Bookmark) {
                        EmptyStateView(
                            title = "Belum Ada Favorit",
                            subtitle = "Tandai komik favoritmu dari katalog agar tersimpan rapi dan mudah dibaca kapan saja.",
                            actionButtonText = "+ Jelajah Katalog",
                            onActionClick = { viewModel.setStatusFilter(ComicFilter.Popular) }
                        )
                    } else {
                        EmptyStateView(
                            title = "Belum Ada Komik",
                            subtitle = "Tidak ada komik yang tersedia pada kategori ini saat ini.",
                            actionButtonText = "Muat Ulang",
                            onActionClick = { viewModel.loadLatestComics() }
                        )
                    }
                }
            } else {
                val listState = rememberLazyGridState()
                LaunchedEffect(listState) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo }
                        .collect { visible ->
                            val last = visible.lastOrNull() ?: return@collect
                            if (last.index >= comics.size - 4) viewModel.loadMoreComics()
                        }
                }

                LazyVerticalGrid(
                    state = listState,
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(comics, key = { it.url }) { comic ->
                        ComicGridItem(comic = comic, onClick = { onComicClick(comic.url) })
                    }
                    if (isLoading) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = colors.primary, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Floating Navigation Dock (Sesuai Referensi Gambar) ────────────────
    FloatingNavigationDock(
        items = dockItems,
        selectedItem = activeStatus,
        onItemSelected = { viewModel.setStatusFilter(it) },
        modifier = Modifier.align(Alignment.BottomCenter)
    )
}

    // ── Genre Filter Bottom Sheet ─────────────────────────────────────────────
    if (showGenreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGenreSheet = false },
            containerColor = if (isDarkTheme) Color(0xFF0F1218) else Color.White,
            scrimColor = Color.Black.copy(alpha = 0.65f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pilih Genre Komik",
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (activeGenre != null) {
                        TextButton(onClick = { viewModel.setGenreFilter(null); showGenreSheet = false }) {
                            Text("Reset", color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                val popularGenres = listOf(
                    "Action", "Adventure", "Comedy", "Drama", "Fantasy",
                    "Isekai", "Martial Arts", "Mystery", "Romance", "School Life",
                    "Sci-Fi", "Seinen", "Shounen", "Slice of Life", "Supernatural"
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    popularGenres.chunked(3).forEach { rowGenres ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowGenres.forEach { g ->
                                val isSelected = activeGenre.equals(g, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) colors.primary else colors.deskMedium)
                                        .border(
                                            1.dp,
                                            if (isSelected) colors.primary else colors.hudBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            viewModel.setGenreFilter(if (isSelected) null else g)
                                            showGenreSheet = false
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = g,
                                        color = if (isSelected) Color.White else colors.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                            if (rowGenres.size < 3) {
                                repeat(3 - rowGenres.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ComicGridItem(comic: Comic, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = colors.cardBackground,
        border = BorderStroke(1.dp, colors.borderSubtle),
        shadowElevation = 2.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(colors.deskHighlight)
            ) {
                AsyncImage(
                    model = comic.thumbnailUrl,
                    contentDescription = comic.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (comic.latestChapter.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = colors.primary.copy(alpha = 0.95f)
                    ) {
                        Text(
                            text = comic.latestChapter,
                            color = Color.White,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = comic.title,
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 10.dp)
            )
        }
    }
}
