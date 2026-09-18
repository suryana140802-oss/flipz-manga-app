package com.example.ui

import com.example.ui.theme.LocalAppColors

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.model.ComicPage
import com.example.model.PortraitReadingMode
import com.example.model.ReadingDirection
import com.example.ui.widgets.BookSpineShadow
import com.example.ui.widgets.PageCurlLayer
import com.example.ui.widgets.PaperTextureLayer
import com.example.ui.widgets.ReaderBottomHud
import com.example.ui.widgets.ReaderTopHud
import com.example.ui.widgets.TranslateOverlay
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
  viewModel: ReaderViewModel,
  modifier: Modifier = Modifier,
  onBack: () -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsState()
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  BackHandler(enabled = uiState.isMagnifierActive) {
    viewModel.exitMagnifier()
  }

  // SAF File pickers: Archive (ZIP, CBZ, RAR, CBR) and Images (JPG, PNG, WEBP, etc.)
  val archivePicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      viewModel.loadFromArchiveUri(uri)
    }
  }

  val imagesPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments()
  ) { uris ->
    if (uris.isNotEmpty()) {
      viewModel.loadFromImageUris(uris)
    }
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(LocalAppColors.current.deskDark)
  ) {
    val isLandscape = constraints.maxWidth > constraints.maxHeight
    val screenWidthPx = constraints.maxWidth.toFloat()
    val (leftPage, rightPage) = uiState.getCurrentSpreadPages()
    val isCoverSpread = uiState.currentSpreadIndex == 0
    val isBackCoverSpread = uiState.currentSpreadIndex == uiState.totalSpreads - 1

    // 1. Ambient Reading Desk with Radial Vignette
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.radialGradient(
            colors = listOf(LocalAppColors.current.deskMedium, LocalAppColors.current.deskDark, Color(0xFF060709)),
            center = androidx.compose.ui.geometry.Offset(
              constraints.maxWidth / 2f,
              constraints.maxHeight / 2f
            ),
            radius = (constraints.maxWidth.coerceAtLeast(constraints.maxHeight)) * 0.85f,
          )
        )
    )

    // 2. Reading Content Area: Landscape (Comic Book Spread) vs Portrait (Slide / Long Strip)
    if (isLandscape) {
      // ==========================================
      // LANDSCAPE: Skeuomorphic Dual-Page Comic Book
      // ==========================================
      Box(
        modifier = Modifier
          .fillMaxSize()
          .let {
              if (uiState.isMagnifierActive) {
                  it.magnifier(
                      sourceCenter = { uiState.magnifierPosition },
                      zoom = 2f,
                      size = DpSize(260.dp, 160.dp),
                      cornerRadius = 18.dp
                  )
              } else it
          }
          .pointerInput(uiState.isMagnifierActive) {
            detectTapGestures(
              onTap = { viewModel.toggleHud() }
            )
          }
          .pointerInput(uiState.isMagnifierActive) {
            detectDragGestures(
              onDragStart = { offset ->
                if (uiState.isMagnifierActive) {
                  viewModel.updateMagnifierPosition(offset)
                } else {
                  viewModel.onDragStart()
                }
              },
              onDrag = { change, dragAmount ->
                change.consume()
                if (uiState.isMagnifierActive) {
                  viewModel.updateMagnifierPosition(change.position)
                } else {
                  viewModel.onDragUpdate(dragAmount.x, screenWidthPx)
                }
              },
              onDragEnd = {
                if (!uiState.isMagnifierActive) {
                  viewModel.onDragEnd()
                }
              },
              onDragCancel = {
                if (!uiState.isMagnifierActive) {
                  viewModel.onDragEnd()
                }
              }
            )
          },
        contentAlignment = Alignment.Center,
      ) {
        val bookWidth = (this@BoxWithConstraints.maxWidth * 0.98f).coerceIn(320.dp, 1200.dp)
        val bookHeight = (this@BoxWithConstraints.maxHeight * 0.95f).coerceIn(240.dp, 800.dp)

        Box(
          modifier = Modifier
            .width(bookWidth)
            .height(bookHeight)
            .shadow(32.dp, RoundedCornerShape(6.dp), spotColor = Color.Black.copy(alpha = 0.85f))
            .clip(RoundedCornerShape(6.dp))
        ) {
          val targetTranslatePage = uiState.currentBook.pages.getOrNull(uiState.currentSinglePageIndex)
          Row(modifier = Modifier.fillMaxSize()) {
            // LEFT SPREAD HALF
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            ) {
              if (leftPage != null && !isCoverSpread) {
                PageCurlLayer(
                  curlProgress = uiState.curlProgress,
                  isRightPage = false,
                  showCornerHint = false,
                  modifier = Modifier.fillMaxSize(),
                ) {
                  PaperTextureLayer(
                    texture = uiState.paperTexture,
                    modifier = Modifier.fillMaxSize(),
                  ) {
                    ComicPageContent(
                      page = leftPage,
                      isRight = false,
                      isStrip = false,
                      translatedBlocks = if (uiState.isTranslationActive && leftPage == targetTranslatePage) uiState.translatedBlocks else emptyList()
                    )
                  }
                }
              }
            }

            // PHYSICAL SPINE CREASE SHADOW
            if (!isCoverSpread && !isBackCoverSpread) {
              BookSpineShadow(width = 40.dp)
            }

            // RIGHT SPREAD HALF
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            ) {
              if (rightPage != null && !isBackCoverSpread) {
                PageCurlLayer(
                  curlProgress = uiState.curlProgress,
                  isRightPage = true,
                  showCornerHint = uiState.showCornerHint,
                  modifier = Modifier.fillMaxSize(),
                ) {
                  PaperTextureLayer(
                    texture = uiState.paperTexture,
                    modifier = Modifier.fillMaxSize(),
                  ) {
                    ComicPageContent(
                      page = rightPage,
                      isRight = true,
                      isStrip = false,
                      translatedBlocks = if (uiState.isTranslationActive && rightPage == targetTranslatePage) uiState.translatedBlocks else emptyList()
                    )
                  }
                }
              }
            }
          }
        }
      }
    } else {
      // ==========================================
      // PORTRAIT: Single Page Slide vs Long Strip Webtoon
      // ==========================================
      if (uiState.portraitMode == PortraitReadingMode.SINGLE_PAGE_SLIDE) {
        // MODE A: Single Page Slide (Swipe/Tap to navigate)
        val currentPageObj = uiState.currentBook.pages.getOrNull(uiState.currentSinglePageIndex)

        Box(
          modifier = Modifier
            .fillMaxSize()
            .let {
                if (uiState.isMagnifierActive) {
                    it.magnifier(
                        sourceCenter = { uiState.magnifierPosition },
                        zoom = 2f,
                        size = DpSize(260.dp, 160.dp),
                        cornerRadius = 18.dp
                    )
                } else it
            }
            .pointerInput(uiState.isMagnifierActive, uiState.readingDirection) {
              detectTapGestures(
                onTap = { offset ->
                  if (uiState.isMagnifierActive) {
                    viewModel.updateMagnifierPosition(offset)
                    return@detectTapGestures
                  }
                  val third = size.width / 3.5f
                  if (offset.x < third) {
                    // Left tap zone
                    if (uiState.readingDirection == ReadingDirection.RTL) {
                      viewModel.nextSinglePage()
                    } else {
                      viewModel.previousSinglePage()
                    }
                  } else if (offset.x > size.width - third) {
                    // Right tap zone
                    if (uiState.readingDirection == ReadingDirection.RTL) {
                      viewModel.previousSinglePage()
                    } else {
                      viewModel.nextSinglePage()
                    }
                  } else {
                    // Center tap: Toggle HUD
                    viewModel.toggleHud()
                  }
                }
              )
            }
            .pointerInput(uiState.isMagnifierActive) {
              detectDragGestures(
                onDragStart = { offset ->
                  if (uiState.isMagnifierActive) {
                    viewModel.updateMagnifierPosition(offset)
                  }
                },
                onDrag = { change, _ ->
                  if (uiState.isMagnifierActive) {
                    change.consume()
                    viewModel.updateMagnifierPosition(change.position)
                  }
                }
              )
            }
            .pointerInput(uiState.readingDirection, uiState.isMagnifierActive) {
              var accumulatedX = 0f
              detectHorizontalDragGestures(
                onDragStart = { accumulatedX = 0f },
                onHorizontalDrag = { change, dragAmount ->
                  if (uiState.isMagnifierActive) return@detectHorizontalDragGestures
                  change.consume()
                  accumulatedX += dragAmount
                },
                onDragEnd = {
                  if (uiState.isMagnifierActive) return@detectHorizontalDragGestures
                  if (accumulatedX < -50f) {
                    // Swiped Left
                    if (uiState.readingDirection == ReadingDirection.RTL) {
                      viewModel.nextSinglePage()
                    } else {
                      viewModel.previousSinglePage()
                    }
                  } else if (accumulatedX > 50f) {
                    // Swiped Right
                    if (uiState.readingDirection == ReadingDirection.RTL) {
                      viewModel.previousSinglePage()
                    } else {
                      viewModel.nextSinglePage()
                    }
                  }
                }
              )
            },
          contentAlignment = Alignment.Center
        ) {
          if (currentPageObj != null) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(top = if (uiState.isHudVisible) 52.dp else 0.dp, bottom = if (uiState.isHudVisible) 56.dp else 0.dp)
            ) {
              PaperTextureLayer(
                texture = uiState.paperTexture,
                modifier = Modifier.fillMaxSize()
              ) {
                ComicPageContent(
                  page = currentPageObj,
                  isRight = false,
                  isStrip = false,
                  translatedBlocks = if (uiState.isTranslationActive) uiState.translatedBlocks else emptyList()
                )
              }
            }
          }
        }
      } else {
        // MODE B: Long Strip (Continuous vertical scroll webtoon)
        val safeInitialIndex = if (uiState.currentBook.totalPages > 0) {
            uiState.currentSinglePageIndex.coerceIn(0, uiState.currentBook.totalPages - 1)
        } else 0
        val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = safeInitialIndex)

        // Prevent LaunchedEffect from re-executing during active scroll
        var hasRestoredInitialScroll by remember(uiState.currentBook.id, uiState.currentBook.chapterUrl) {
          mutableStateOf(false)
        }

        LaunchedEffect(uiState.currentBook.id, uiState.currentBook.chapterUrl) {
          if (!hasRestoredInitialScroll && uiState.currentSinglePageIndex > 0 && uiState.currentSinglePageIndex < uiState.currentBook.totalPages) {
            runCatching { lazyListState.scrollToItem(uiState.currentSinglePageIndex) }
            hasRestoredInitialScroll = true
          }
        }

        LaunchedEffect(lazyListState) {
          snapshotFlow { lazyListState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
              viewModel.onScrollToPage(index)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
          PaperTextureLayer(
            texture = uiState.paperTexture,
            modifier = Modifier.fillMaxSize()
          ) {
            LazyColumn(
              state = lazyListState,
              userScrollEnabled = !uiState.isMagnifierActive,
              modifier = Modifier
                .fillMaxSize()
                .let {
                    if (uiState.isMagnifierActive) {
                        it.magnifier(
                            sourceCenter = { uiState.magnifierPosition },
                            zoom = 2f,
                            size = DpSize(260.dp, 160.dp),
                            cornerRadius = 18.dp
                        )
                    } else it
                }
                .pointerInput(uiState.isMagnifierActive) {
                  detectTapGestures(
                    onTap = { offset ->
                      if (uiState.isMagnifierActive) {
                        viewModel.updateMagnifierPosition(offset)
                        return@detectTapGestures
                      }
                      viewModel.toggleHud()
                    }
                  )
                }
                .pointerInput(uiState.isMagnifierActive) {
                  detectDragGestures(
                    onDragStart = { offset ->
                      if (uiState.isMagnifierActive) {
                        viewModel.updateMagnifierPosition(offset)
                      }
                    },
                    onDrag = { change, _ ->
                      if (uiState.isMagnifierActive) {
                        change.consume()
                        viewModel.updateMagnifierPosition(change.position)
                      }
                    }
                  )
                }
            ) {
              val targetTranslatePage = uiState.currentBook.pages.getOrNull(uiState.currentSinglePageIndex)
              itemsIndexed(
                items = uiState.currentBook.pages,
                key = { index, page -> "${page.pageNumber}_$index" },
                contentType = { _, _ -> "comic_page" }
              ) { _, page ->
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
                ) {
                  ComicPageContent(
                    page = page,
                    isRight = false,
                    isStrip = true,
                    translatedBlocks = if (uiState.isTranslationActive && page == targetTranslatePage) uiState.translatedBlocks else emptyList()
                  )
                }
              }
            }
          }

          // Side Fast Scroller / Slider Layar di Samping
          if (uiState.currentBook.totalPages > 1) {
            ReaderFastScroller(
              lazyListState = lazyListState,
              totalPages = uiState.currentBook.totalPages,
              modifier = Modifier.align(Alignment.CenterEnd)
            )
          }
        }
      }
    }

    // 3. Loading Indicator
    if (uiState.isLoading) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator(color = LocalAppColors.current.primary)
      }
    }

    // 4. Translate Overlay (Moved to ComicPageContent)

    val translationError = uiState.translationError
    if (uiState.isTranslating) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = LocalAppColors.current.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Menerjemahkan...", color = Color.White)
            }
        }
    } else if (translationError != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.background(Color.Black.copy(alpha=0.7f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                Text("Gagal menerjemahkan", color = Color.Red, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(translationError, color = Color.White, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.Button(onClick = { viewModel.toggleTranslate() }) {
                    Text("Tutup")
                }
            }
        }
    }

    if (uiState.showDownloadDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.cancelDownloadModel() },
            title = { Text("Download Bahasa", color = LocalAppColors.current.textPrimary) },
            text = { Text("Model bahasa untuk terjemahan ini belum tersedia (sekitar ~30MB). Apakah Anda ingin mendownloadnya sekarang agar bisa menerjemahkan tanpa batas kuota?", color = LocalAppColors.current.textSecondary) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.confirmDownloadModel() }) {
                    Text("Download", color = LocalAppColors.current.primary)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.cancelDownloadModel() }) {
                    Text("Batal", color = LocalAppColors.current.textMuted)
                }
            },
            containerColor = LocalAppColors.current.hudGlass,
            titleContentColor = LocalAppColors.current.textPrimary,
            textContentColor = LocalAppColors.current.textSecondary
        )
    }

    if (uiState.isDownloadingModel) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .pointerInput(Unit) {},
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.background(Color.Black.copy(alpha=0.8f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                CircularProgressIndicator(color = LocalAppColors.current.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Mendownload model bahasa...", color = Color.White, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Mohon tunggu sebentar, ini hanya perlu dilakukan sekali.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }

    // 5. Ghost Top HUD
    ReaderTopHud(
      visible = uiState.isHudVisible,
      title = uiState.currentBook.title,
      readingDirection = uiState.readingDirection,
      totalPages = uiState.currentBook.totalPages,
      currentPage = if (isLandscape) uiState.currentSpreadIndex else uiState.currentSinglePageIndex,
      isPortrait = !isLandscape,
      portraitMode = uiState.portraitMode,
      paperTexture = uiState.paperTexture,
      savedSpreadIndex = uiState.savedSpreadIndex,
      isTranslationActive = uiState.isTranslationActive,
      isTranslating = uiState.isTranslating,
      sourceLanguage = uiState.sourceLanguage,
      onTogglePortraitMode = { viewModel.togglePortraitMode() },
      onToggleTranslate = { viewModel.toggleTranslate() },
      onSourceLanguageChanged = { viewModel.setSourceLanguage(it) },
      onContinueReading = { viewModel.resumeToSavedPosition() },
      onOpenArchiveClick = { archivePicker.launch(arrayOf("*/*")) },
      onOpenImagesClick = { imagesPicker.launch(arrayOf("image/*")) },
      onToggleDirection = { viewModel.toggleReadingDirection() },
      onSelectPaperTexture = { viewModel.setPaperTexture(it) },
      onCapturePanel = {
        val act = context as? android.app.Activity
        if (act != null) {
          viewModel.setHudVisible(false)
          scope.launch {
            kotlinx.coroutines.delay(250)
            com.example.util.PanelShareHelper.captureAndSharePanel(
              activity = act,
              comicTitle = uiState.currentBook.title,
              chapterTitle = uiState.currentBook.chapterTitle,
              onComplete = {
                viewModel.setHudVisible(true)
              }
            )
          }
        }
      },
      onBack = onBack,
      modifier = Modifier.align(Alignment.TopCenter),
    )



    // 6. Ghost Bottom HUD
    val pageLabel = if (isLandscape) {
      when {
        uiState.currentSpreadIndex == 0 -> "Cover Depan"
        uiState.currentSpreadIndex >= uiState.totalSpreads - 1 -> "Cover Belakang"
        else -> {
          val pLeft = leftPage?.pageNumber ?: 0
          val pRight = rightPage?.pageNumber ?: 0
          "Hal $pLeft - $pRight dari ${uiState.currentBook.totalPages}"
        }
      }
    } else {
      "Hal ${uiState.currentSinglePageIndex + 1} dari ${uiState.currentBook.totalPages}"
    }

    ReaderBottomHud(
      visible = uiState.isHudVisible,
      isPortrait = !isLandscape,
      portraitMode = uiState.portraitMode,
      currentPage = uiState.currentSinglePageIndex,
      totalPages = uiState.currentBook.totalPages,
      currentSpread = uiState.currentSpreadIndex,
      totalSpreads = uiState.totalSpreads,
      pageIndicatorText = pageLabel,
      isMagnifierActive = uiState.isMagnifierActive,
      isSaved = false,
      onPreviousClick = {
        if (isLandscape) viewModel.previousPage() else viewModel.previousSinglePage()
      },
      onNextClick = {
        if (isLandscape) viewModel.nextPage() else viewModel.nextSinglePage()
      },
      onSeek = { target ->
        if (isLandscape) viewModel.goToSpread(target) else viewModel.goToSinglePage(target)
      },
      onToggleMagnifier = { viewModel.toggleMagnifier() },
      modifier = Modifier.align(Alignment.BottomCenter),
    )

    // 7. Floating Continue Reading Notification
    AnimatedVisibility(
      visible = uiState.resumeToastMessage != null,
      enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
      exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .statusBarsPadding()
        .padding(top = 58.dp)
    ) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(16.dp))
          .background(Color(0xFF181C24).copy(alpha = 0.95f))
          .border(1.dp, LocalAppColors.current.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
          .padding(horizontal = 14.dp, vertical = 6.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.Bookmark,
            contentDescription = "Posisi Tersimpan",
            tint = LocalAppColors.current.primary,
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = uiState.resumeToastMessage ?: "",
            color = LocalAppColors.current.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          )
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(Color.White.copy(alpha = 0.12f))
              .clickable { viewModel.restartToBeginning() }
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "Mulai Awal",
              color = LocalAppColors.current.textSecondary,
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
          IconButton(
            onClick = { viewModel.dismissResumeToast() },
            modifier = Modifier.size(20.dp)
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "Tutup",
              tint = LocalAppColors.current.textMuted,
              modifier = Modifier.size(13.dp)
            )
          }
        }
      }
    }

    // 8. Floating Magnifier Exit Controller (visible whenever magnifier is active)
    AnimatedVisibility(
      visible = uiState.isMagnifierActive,
      enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
      exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .statusBarsPadding()
        .padding(top = 16.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.92f),
        border = BorderStroke(1.dp, LocalAppColors.current.primary.copy(alpha = 0.8f)),
        shadowElevation = 8.dp,
        modifier = Modifier.clickable { viewModel.exitMagnifier() }
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = LocalAppColors.current.primary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Kaca Pembesar Aktif",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.width(10.dp))
          Box(
            modifier = Modifier
              .size(22.dp)
              .clip(CircleShape)
              .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "Tutup Kaca Pembesar",
              tint = Color.White,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }
    }


  }
}

@Composable
private fun ReaderFastScroller(
  lazyListState: LazyListState,
  totalPages: Int,
  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()
  var isDragging by remember { mutableStateOf(false) }
  var dragFraction by remember { mutableFloatStateOf(0f) }
  var isVisible by remember { mutableStateOf(false) }
  var hideJob by remember { mutableStateOf<Job?>(null) }

  // Detect fast scrolling
  LaunchedEffect(lazyListState) {
    var lastIndex = lazyListState.firstVisibleItemIndex
    var lastOffset = lazyListState.firstVisibleItemScrollOffset
    var lastTime = System.currentTimeMillis()

    snapshotFlow {
      Triple(
        lazyListState.isScrollInProgress,
        lazyListState.firstVisibleItemIndex,
        lazyListState.firstVisibleItemScrollOffset
      )
    }.collect { (isScrolling, index, offset) ->
      val now = System.currentTimeMillis()
      val timeDiff = (now - lastTime).coerceAtLeast(1)

      if (isScrolling) {
        val indexDiff = kotlin.math.abs(index - lastIndex)
        val offsetDiff = kotlin.math.abs(offset - lastOffset)

        // Fast scroll detection:
        // When index changes (swiping across pages) or fast offset jump (> 50px within < 150ms)
        val isFast = indexDiff > 0 || (timeDiff < 150 && offsetDiff > 50)

        if (isFast) {
          isVisible = true
          hideJob?.cancel()
          hideJob = scope.launch {
            delay(2200)
            if (!isDragging) {
              isVisible = false
            }
          }
        }
      } else {
        if (!isDragging && isVisible) {
          hideJob?.cancel()
          hideJob = scope.launch {
            delay(1200)
            if (!isDragging) {
              isVisible = false
            }
          }
        }
      }

      lastIndex = index
      lastOffset = offset
      lastTime = now
    }
  }

  val alpha by animateFloatAsState(
    targetValue = if (isVisible || isDragging) 1f else 0f,
    animationSpec = tween(durationMillis = 250),
    label = "scroller_alpha"
  )

  if (alpha <= 0.005f) return

  val firstVisibleIndex = lazyListState.firstVisibleItemIndex
  val scrollFraction = if (totalPages > 1) {
    (firstVisibleIndex.toFloat() / (totalPages - 1)).coerceIn(0f, 1f)
  } else 0f

  val effectiveFraction = if (isDragging) dragFraction else scrollFraction
  val displayPage = (effectiveFraction * (totalPages - 1)).roundToInt().coerceIn(0, totalPages - 1)
  val colors = LocalAppColors.current

  BoxWithConstraints(
    modifier = modifier
      .fillMaxHeight()
      .width(56.dp)
      .padding(vertical = 60.dp)
      .statusBarsPadding()
      .navigationBarsPadding()
      .graphicsLayer { this.alpha = alpha },
    contentAlignment = Alignment.CenterEnd
  ) {
    val totalHeightPx = constraints.maxHeight.toFloat()
    val thumbHeightDp = 56.dp
    val thumbWidthDp = 28.dp
    val density = LocalDensity.current
    val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
    val maxScrollOffsetPx = (totalHeightPx - thumbHeightPx).coerceAtLeast(1f)
    val thumbOffsetDp = with(density) { (effectiveFraction * maxScrollOffsetPx).toDp() }

    // Floating Page Badge (visible while dragging)
    AnimatedVisibility(
      visible = isDragging,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.TopEnd)
        .offset(x = (-48).dp, y = (thumbOffsetDp - 4.dp).coerceAtLeast(0.dp))
    ) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF0F172A).copy(alpha = 0.85f))
          .border(1.dp, colors.primary.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
          .padding(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Text(
          text = "Hal. ${displayPage + 1} / $totalPages",
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    // Vertical Track line
    Box(
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 16.dp)
        .width(3.dp)
        .fillMaxHeight()
        .clip(RoundedCornerShape(1.5.dp))
        .background(Color.White.copy(alpha = if (isDragging) 0.25f else 0.12f))
    )

    // Touch & Drag Zone covering the whole track
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .width(48.dp)
        .align(Alignment.CenterEnd)
        .pointerInput(totalPages) {
          detectTapGestures(
            onPress = { offset ->
              val fraction = ((offset.y - thumbHeightPx / 2f) / maxScrollOffsetPx).coerceIn(0f, 1f)
              dragFraction = fraction
              isDragging = true
              hideJob?.cancel()
              val target = (fraction * (totalPages - 1)).roundToInt().coerceIn(0, totalPages - 1)
              scope.launch { lazyListState.scrollToItem(target) }
              tryAwaitRelease()
              isDragging = false
              hideJob = scope.launch {
                delay(1500)
                isVisible = false
              }
            }
          )
        }
        .pointerInput(totalPages) {
          detectVerticalDragGestures(
            onDragStart = { offset ->
              val fraction = ((offset.y - thumbHeightPx / 2f) / maxScrollOffsetPx).coerceIn(0f, 1f)
              dragFraction = fraction
              isDragging = true
              hideJob?.cancel()
              val target = (fraction * (totalPages - 1)).roundToInt().coerceIn(0, totalPages - 1)
              scope.launch { lazyListState.scrollToItem(target) }
            },
            onVerticalDrag = { change, _ ->
              change.consume()
              val fraction = ((change.position.y - thumbHeightPx / 2f) / maxScrollOffsetPx).coerceIn(0f, 1f)
              dragFraction = fraction
              val target = (fraction * (totalPages - 1)).roundToInt().coerceIn(0, totalPages - 1)
              scope.launch { lazyListState.scrollToItem(target) }
            },
            onDragEnd = {
              isDragging = false
              hideJob?.cancel()
              hideJob = scope.launch {
                delay(1500)
                isVisible = false
              }
            },
            onDragCancel = {
              isDragging = false
              hideJob?.cancel()
              hideJob = scope.launch {
                delay(1500)
                isVisible = false
              }
            }
          )
        }
    ) {
      // Draggable Thumb Knob (Slightly larger and semi-transparent)
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .offset(y = thumbOffsetDp)
          .padding(end = 4.dp)
          .width(thumbWidthDp)
          .height(thumbHeightDp)
          .clip(RoundedCornerShape(14.dp))
          .background(
            if (isDragging) colors.primary.copy(alpha = 0.75f)
            else Color(0xFF0F172A).copy(alpha = 0.40f)
          )
          .border(
            1.dp,
            if (isDragging) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.30f),
            RoundedCornerShape(14.dp)
          ),
        contentAlignment = Alignment.Center
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(3.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          repeat(3) {
            Box(
              modifier = Modifier
                .width(10.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isDragging) Color.White else Color.White.copy(alpha = 0.65f))
            )
          }
        }
      }
    }

    // Quick scroll to top button
    Box(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .offset(y = (-34).dp)
        .padding(end = 3.dp)
        .size(28.dp)
        .clip(CircleShape)
        .background(Color(0xFF0F172A).copy(alpha = 0.40f))
        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
        .clickable {
          scope.launch { lazyListState.scrollToItem(0) }
        },
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Rounded.KeyboardArrowUp,
        contentDescription = "Gulir ke atas",
        tint = Color.White.copy(alpha = 0.85f),
        modifier = Modifier.size(16.dp)
      )
    }

    // Quick scroll to bottom button
    Box(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .offset(y = 34.dp)
        .padding(end = 3.dp)
        .size(28.dp)
        .clip(CircleShape)
        .background(Color(0xFF0F172A).copy(alpha = 0.40f))
        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
        .clickable {
          scope.launch { lazyListState.scrollToItem((totalPages - 1).coerceAtLeast(0)) }
        },
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Rounded.KeyboardArrowDown,
        contentDescription = "Gulir ke bawah",
        tint = Color.White.copy(alpha = 0.85f),
        modifier = Modifier.size(16.dp)
      )
    }
  }
}

@Composable
private fun ComicPageContent(
  page: ComicPage,
  isRight: Boolean,
  isStrip: Boolean = false,
  translatedBlocks: List<com.example.translation.TranslatedBlock> = emptyList(),
  modifier: Modifier = Modifier,
  onBack: () -> Unit = {},
) {
  Box(
    modifier = modifier.background(Color(0xFF14171E)),
    contentAlignment = Alignment.Center
  ) {
    if (page.imageBytes != null || page.imageUrl != null) {
      val context = LocalContext.current
      val data = page.imageUrl ?: page.imageBytes
      val imageRequest = remember(data) {
        ImageRequest.Builder(context)
          .data(data)
          .addHeader("Referer", "https://web1.mgkomik.cc/")
          .crossfade(!isStrip)
          .build()
      }

      if (isStrip) {
        AsyncImage(
          model = imageRequest,
          contentDescription = page.label,
          contentScale = ContentScale.FillWidth,
          modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        )
      } else {
        AsyncImage(
          model = imageRequest,
          contentDescription = page.label,
          contentScale = ContentScale.Fit,
          alignment = Alignment.Center,
          modifier = Modifier.fillMaxSize(),
        )
      }
    } else {
      Box(
        modifier = if (isStrip) Modifier.fillMaxWidth().aspectRatio(0.707f).background(Color.White)
        else Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
      ) {
        Text(text = page.label, color = Color.Black)
      }
    }
    
    if (translatedBlocks.isNotEmpty()) {
      TranslateOverlay(
        blocks = translatedBlocks,
        modifier = Modifier.matchParentSize()
      )
    }
  }
}
