package com.example.ui.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.roundToInt
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.example.model.PaperTexture
import com.example.model.PortraitReadingMode
import com.example.model.ReadingDirection
import com.example.ui.theme.LocalAppColors

@Composable
fun ReaderTopHud(
  visible: Boolean,
  title: String,
  readingDirection: ReadingDirection,
  totalPages: Int,
  currentPage: Int,
  isPortrait: Boolean,
  portraitMode: PortraitReadingMode,
  paperTexture: PaperTexture,
  savedSpreadIndex: Int? = null,
  isTranslationActive: Boolean = false,
  isTranslating: Boolean = false,
  sourceLanguage: String = com.google.mlkit.nl.translate.TranslateLanguage.JAPANESE,
  onTogglePortraitMode: () -> Unit,
  onToggleTranslate: () -> Unit = {},
  onSourceLanguageChanged: (String) -> Unit = {},
  onContinueReading: (() -> Unit)? = null,
  onOpenArchiveClick: () -> Unit,
  onOpenImagesClick: () -> Unit,
  onToggleDirection: () -> Unit,
  onSelectPaperTexture: (PaperTexture) -> Unit,
  onCapturePanel: () -> Unit = {},
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showOverflowMenu by remember { mutableStateOf(false) }
  var showLanguageDialog by remember { mutableStateOf(false) }
  val colors = LocalAppColors.current

  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(),
    exit = fadeOut(),
    modifier = modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(colors.hudGlass)
        .border(1.dp, colors.hudBorder, RoundedCornerShape(16.dp))
        .padding(horizontal = 12.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        // Left: Compact Brand & Info
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f, fill = false)
        ) {
          IconButton(
            onClick = onBack,
            modifier = Modifier.size(36.dp).padding(end = 4.dp)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "Kembali",
              tint = colors.textPrimary,
              modifier = Modifier.size(22.dp)
            )
          }

          Box(
            modifier = Modifier
              .size(28.dp)
              .background(colors.primary, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.MenuBook,
              contentDescription = "Flipz Manga",
              tint = Color.White,
              modifier = Modifier.size(15.dp),
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f, fill = false)
          ) {
            Text(
              text = title,
              color = colors.textPrimary,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(
                text = if (isPortrait) {
                  if (portraitMode == PortraitReadingMode.SINGLE_PAGE_SLIDE) "SLIDE 1 HAL" else "LONG STRIP"
                } else {
                  if (readingDirection == ReadingDirection.RTL) "RTL MANGA" else "LTR BARAT"
                },
                color = colors.primary,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
              )
              Text(
                text = "• Hal ${currentPage + 1}/$totalPages",
                color = colors.textMuted,
                fontSize = 8.sp,
                maxLines = 1,
              )
              if (savedSpreadIndex != null && savedSpreadIndex > 0) {
                Icon(
                  imageVector = Icons.Rounded.Bookmark,
                  contentDescription = "Posisi Tersimpan",
                  tint = colors.primary,
                  modifier = Modifier.size(9.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Right Action Toolbar
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          // Portrait Mode Switcher Chip (Only visible in Portrait mode)
          if (isPortrait) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(colors.primary.copy(alpha = 0.18f))
                .border(1.dp, colors.primary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .clickable { onTogglePortraitMode() }
                .padding(horizontal = 7.dp, vertical = 4.dp),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = if (portraitMode == PortraitReadingMode.SINGLE_PAGE_SLIDE) {
                    Icons.Rounded.ViewCarousel
                  } else {
                    Icons.Rounded.ViewStream
                  },
                  contentDescription = "Ganti Mode Baca",
                  tint = colors.primary,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = if (portraitMode == PortraitReadingMode.SINGLE_PAGE_SLIDE) "Slide" else "Strip",
                  color = colors.textPrimary,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
            Spacer(modifier = Modifier.width(2.dp))
          }

          // Cuplik Panel Button (Langsung terlihat di UI atas)
          IconButton(
            onClick = onCapturePanel,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Rounded.Crop,
              contentDescription = "Cuplik & Bagikan Panel",
              tint = colors.textPrimary,
              modifier = Modifier.size(20.dp),
            )
          }


          // Titik Tiga (Overflow Menu - Menampung semua fitur lainnya)
          Box {
            IconButton(
              onClick = { showOverflowMenu = true },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Fitur Lainnya",
                tint = colors.textPrimary,
                modifier = Modifier.size(22.dp),
              )
            }

            DropdownMenu(
              expanded = showOverflowMenu,
              onDismissRequest = { showOverflowMenu = false },
              modifier = Modifier.background(colors.hudGlass)
            ) {
              // ── 1. Fitur Terjemahan (Translate) ──────────────────────────
              DropdownMenuItem(
                text = {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = if (isTranslationActive) "Matikan Terjemahan" else "Aktifkan Terjemahan",
                      color = if (isTranslationActive) colors.primary else colors.textPrimary,
                      fontSize = 12.sp,
                      fontWeight = if (isTranslationActive) FontWeight.Bold else FontWeight.Normal
                    )
                    Box(
                      modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isTranslationActive) colors.primary else colors.deskMedium)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                      Text(
                        text = when (sourceLanguage) {
                          com.google.mlkit.nl.translate.TranslateLanguage.JAPANESE -> "JP"
                          com.google.mlkit.nl.translate.TranslateLanguage.ENGLISH -> "EN"
                          com.google.mlkit.nl.translate.TranslateLanguage.CHINESE -> "ZH"
                          else -> "JP"
                        },
                        color = if (isTranslationActive) Color.White else colors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                },
                leadingIcon = {
                  Icon(
                    imageVector = if (isTranslationActive) Icons.Rounded.CheckCircle else Icons.Rounded.Translate,
                    contentDescription = null,
                    tint = if (isTranslationActive) colors.primary else colors.textMuted,
                    modifier = Modifier.size(16.dp)
                  )
                },
                onClick = {
                  showOverflowMenu = false
                  onToggleTranslate()
                }
              )

              DropdownMenuItem(
                text = { Text("Pilih Bahasa Terjemahan...", color = colors.textPrimary, fontSize = 12.sp) },
                leadingIcon = {
                  Icon(Icons.Rounded.Language, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                },
                onClick = {
                  showOverflowMenu = false
                  showLanguageDialog = true
                }
              )

              HorizontalDivider(color = colors.hudBorder.copy(alpha = 0.5f))

              // ── 2. Fitur Impor / Buka File Komik ─────────────────────────
              DropdownMenuItem(
                text = { Text("Buka Arsip (ZIP, CBZ, RAR, CBR)", color = colors.textPrimary, fontSize = 12.sp) },
                leadingIcon = {
                  Icon(Icons.Rounded.Archive, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                },
                onClick = {
                  showOverflowMenu = false
                  onOpenArchiveClick()
                }
              )
              DropdownMenuItem(
                text = { Text("Pilih Gambar (JPG, PNG, WEBP)", color = colors.textPrimary, fontSize = 12.sp) },
                leadingIcon = {
                  Icon(Icons.Rounded.Collections, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                },
                onClick = {
                  showOverflowMenu = false
                  onOpenImagesClick()
                }
              )

              HorizontalDivider(color = colors.hudBorder.copy(alpha = 0.5f))

              // ── 3. Pengaturan Tampilan & Pembaca ─────────────────────────
              DropdownMenuItem(
                text = {
                  Text(
                    text = if (readingDirection == ReadingDirection.RTL) "Arah: Manga (RTL)" else "Arah: Barat (LTR)",
                    color = colors.textPrimary,
                    fontSize = 12.sp
                  )
                },
                leadingIcon = {
                  Icon(Icons.AutoMirrored.Rounded.CompareArrows, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                },
                onClick = {
                  onToggleDirection()
                  showOverflowMenu = false
                }
              )

              DropdownMenuItem(
                text = { Text("Tekstur: Clean (Digital)", color = colors.textPrimary, fontSize = 12.sp) },
                leadingIcon = {
                  if (paperTexture == PaperTexture.CLEAN) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                  } else {
                    Icon(Icons.Rounded.Style, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                  }
                },
                onClick = {
                  onSelectPaperTexture(PaperTexture.CLEAN)
                  showOverflowMenu = false
                }
              )
              DropdownMenuItem(
                text = { Text("Tekstur: Matte (Buku)", color = colors.textPrimary, fontSize = 12.sp) },
                leadingIcon = {
                  if (paperTexture == PaperTexture.MATTE) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                  } else {
                    Icon(Icons.Rounded.Style, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                  }
                },
                onClick = {
                  onSelectPaperTexture(PaperTexture.MATTE)
                  showOverflowMenu = false
                }
              )
              DropdownMenuItem(
                text = { Text("Tekstur: Manga Pulp (Tankobon)", color = colors.textPrimary, fontSize = 12.sp) },
                leadingIcon = {
                  if (paperTexture == PaperTexture.MANGA_PULP) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                  } else {
                    Icon(Icons.Rounded.Style, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                  }
                },
                onClick = {
                  onSelectPaperTexture(PaperTexture.MANGA_PULP)
                  showOverflowMenu = false
                }
              )
              DropdownMenuItem(
                text = { Text("Tekstur: Vintage Aged (Retro)", color = colors.textPrimary, fontSize = 12.sp) },
                leadingIcon = {
                  if (paperTexture == PaperTexture.VINTAGE) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                  } else {
                    Icon(Icons.Rounded.Style, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                  }
                },
                onClick = {
                  onSelectPaperTexture(PaperTexture.VINTAGE)
                  showOverflowMenu = false
                }
              )

              if (savedSpreadIndex != null && savedSpreadIndex > 0) {
                DropdownMenuItem(
                  text = { Text("Lanjut Bacaan Terakhir", color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                  leadingIcon = {
                    Icon(Icons.Rounded.Bookmark, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                  },
                  onClick = {
                    onContinueReading?.invoke()
                    showOverflowMenu = false
                  }
                )
              }
            }
          }
        }
      }
    }

    // Modal Dialog Pilih Bahasa Terjemahan
    if (showLanguageDialog) {
      AlertDialog(
        onDismissRequest = { showLanguageDialog = false },
        title = {
          Text(
            text = "Bahasa Sumber Terjemahan",
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val languages = listOf(
              com.google.mlkit.nl.translate.TranslateLanguage.JAPANESE to "Jepang (JP)",
              com.google.mlkit.nl.translate.TranslateLanguage.ENGLISH to "Inggris (EN)",
              com.google.mlkit.nl.translate.TranslateLanguage.CHINESE to "Mandarin (ZH)"
            )
            languages.forEach { (langCode, langName) ->
              val isSelected = sourceLanguage == langCode
              androidx.compose.material3.Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable {
                    onSourceLanguageChanged(langCode)
                    showLanguageDialog = false
                  },
                color = if (isSelected) colors.primary.copy(alpha = 0.15f) else Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (isSelected) colors.primary else colors.hudBorder
                ),
                shape = RoundedCornerShape(10.dp)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = langName,
                    color = if (isSelected) colors.primary else colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                  if (isSelected) {
                    Icon(
                      imageVector = Icons.Rounded.CheckCircle,
                      contentDescription = null,
                      tint = colors.primary,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { showLanguageDialog = false }) {
            Text("Tutup", color = colors.primary)
          }
        },
        containerColor = colors.deskDark,
        shape = RoundedCornerShape(16.dp)
      )
    }
  }
}

@Composable
fun ReaderBottomHud(
  visible: Boolean,
  isPortrait: Boolean,
  portraitMode: PortraitReadingMode,
  currentPage: Int,
  totalPages: Int,
  currentSpread: Int,
  totalSpreads: Int,
  pageIndicatorText: String,
  isMagnifierActive: Boolean,
  isSaved: Boolean = false,
  onPreviousClick: () -> Unit,
  onNextClick: () -> Unit,
  onSeek: (Int) -> Unit,
  onToggleMagnifier: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = LocalAppColors.current
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(),
    exit = fadeOut(),
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(colors.hudGlass)
        .border(1.dp, colors.hudBorder, RoundedCornerShape(16.dp))
        .padding(horizontal = 12.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val hasPrev = if (isPortrait) currentPage > 0 else currentSpread > 0
        val hasNext = if (isPortrait) currentPage < totalPages - 1 else currentSpread < totalSpreads - 1

        // Prev Button
        IconButton(
          onClick = onPreviousClick,
          enabled = hasPrev,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.NavigateBefore,
            contentDescription = "Sebelumnya",
            tint = if (hasPrev) colors.textPrimary else colors.textMuted,
            modifier = Modifier.size(24.dp),
          )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Scrubbing Slider with 1 dot per comic image
        val currentVal = if (isPortrait) currentPage else currentSpread
        val totalCount = if (isPortrait) totalPages else totalSpreads

        ComicPageSlider(
          currentPage = currentVal,
          totalCount = totalCount,
          onSeek = onSeek,
          modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Page Indicator Badge (Harmonized, fixed styling without layout shift)
        Box(
          modifier = Modifier
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = if (isPortrait) "Hal ${currentPage + 1}/$totalPages" else pageIndicatorText,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
          )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Next Button
        IconButton(
          onClick = onNextClick,
          enabled = hasNext,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
            contentDescription = "Selanjutnya",
            tint = if (hasNext) colors.textPrimary else colors.textMuted,
            modifier = Modifier.size(24.dp),
          )
        }

        Box(
          modifier = Modifier
            .padding(horizontal = 3.dp)
            .width(1.dp)
            .height(16.dp)
            .background(Color.White.copy(alpha = 0.2f))
        )

        // Magnifier Loupe Toggle
        IconButton(
          onClick = onToggleMagnifier,
          modifier = Modifier
            .size(32.dp)
            .background(
              if (isMagnifierActive) colors.primary.copy(alpha = 0.25f) else Color.Transparent,
              CircleShape
            )
        ) {
          Icon(
            imageVector = if (isMagnifierActive) Icons.Rounded.ZoomIn else Icons.Rounded.Search,
            contentDescription = "Kaca Pembesar",
            tint = if (isMagnifierActive) colors.primary else colors.textPrimary,
            modifier = Modifier.size(17.dp),
          )
        }
      }
    }
  }
}

@Composable
fun ComicPageSlider(
  currentPage: Int,
  totalCount: Int,
  onSeek: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = LocalAppColors.current
  val maxVal = (totalCount - 1).coerceAtLeast(1)
  val clampedPage = currentPage.coerceIn(0, maxVal)

  BoxWithConstraints(
    modifier = modifier
      .height(36.dp)
      .pointerInput(maxVal) {
        detectTapGestures(
          onTap = { offset ->
            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
            val target = (fraction * maxVal).roundToInt().coerceIn(0, maxVal)
            onSeek(target)
          }
        )
      }
      .pointerInput(maxVal) {
        detectHorizontalDragGestures(
          onDragStart = { offset ->
            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
            val target = (fraction * maxVal).roundToInt().coerceIn(0, maxVal)
            onSeek(target)
          },
          onHorizontalDrag = { change, _ ->
            change.consume()
            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
            val target = (fraction * maxVal).roundToInt().coerceIn(0, maxVal)
            if (target != clampedPage) {
              onSeek(target)
            }
          }
        )
      },
    contentAlignment = Alignment.CenterStart
  ) {
    Canvas(modifier = Modifier.fillMaxWidth().height(24.dp)) {
      val centerY = size.height / 2f
      val trackHeight = 6.dp.toPx()
      val cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)

      // 1. Inactive Track Background
      drawRoundRect(
        color = Color.White.copy(alpha = 0.2f),
        topLeft = Offset(0f, centerY - trackHeight / 2f),
        size = Size(size.width, trackHeight),
        cornerRadius = cornerRadius
      )

      val progressFraction = clampedPage.toFloat() / maxVal.toFloat()
      val thumbWidth = 4.dp.toPx()
      val thumbHeight = 18.dp.toPx()
      val activeWidth = (progressFraction * size.width).coerceIn(0f, size.width)

      // 2. Active Track
      if (activeWidth > 0f) {
        drawRoundRect(
          color = colors.primary,
          topLeft = Offset(0f, centerY - trackHeight / 2f),
          size = Size(activeWidth, trackHeight),
          cornerRadius = cornerRadius
        )
      }

      // 3. Discrete Dots (1 dot per comic page/image, scaled dynamically to prevent cramping)
      if (totalCount in 2..80) {
        val spacingPx = size.width / maxVal.toFloat()
        val dotRadiusPx = when {
          spacingPx >= 12.dp.toPx() -> 2.2.dp.toPx()
          spacingPx >= 7.dp.toPx() -> 1.6.dp.toPx()
          spacingPx >= 4.dp.toPx() -> 1.1.dp.toPx()
          else -> 0.8.dp.toPx()
        }

        for (i in 0 until totalCount) {
          val dotX = (i.toFloat() / maxVal.toFloat()) * size.width
          val isPassed = i <= clampedPage
          drawCircle(
            color = if (isPassed) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.35f),
            radius = dotRadiusPx,
            center = Offset(dotX, centerY)
          )
        }
      }

      // 4. Sleek Vertical Pill Thumb Handle matching reference
      val thumbX = (progressFraction * size.width).coerceIn(thumbWidth / 2f, size.width - thumbWidth / 2f)
      drawRoundRect(
        color = Color.White,
        topLeft = Offset(thumbX - thumbWidth / 2f, centerY - thumbHeight / 2f),
        size = Size(thumbWidth, thumbHeight),
        cornerRadius = CornerRadius(thumbWidth / 2f, thumbWidth / 2f)
      )
    }
  }
}

