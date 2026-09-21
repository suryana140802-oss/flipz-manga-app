package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalAppColors

@Composable
fun HomeScreen(
    isDarkTheme: Boolean = true,
    onThemeToggle: () -> Unit = {},
    onNavigateToReaderOnline: () -> Unit,
    onImportArchiveClick: () -> Unit,
    onImportImagesClick: () -> Unit,
    onSupportClick: (() -> Unit)? = null,
    onResumeReading: ((com.example.data.entity.ReadingProgressEntity) -> Unit)? = null
) {
    val colors = LocalAppColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember(context) { com.example.data.ComicDatabase.getInstance(context) }
    val latestProgress by db.readingProgressDao().getLatestProgress().collectAsState(initial = null)

    var showImportGuide by remember { mutableStateOf(false) }

    if (showImportGuide) {
        AlertDialog(
            onDismissRequest = { showImportGuide = false },
            title = {
                Text(
                    text = "Import Komik Offline",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Pilih file arsip (ZIP, RAR, CBZ, CBR) atau pilih sekumpulan gambar chapter langsung dari memori perangkat untuk dibaca secara offline.",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportGuide = false
                    onImportArchiveClick()
                }) {
                    Text(
                        "Pilih Arsip (CBZ/ZIP)",
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportGuide = false
                    onImportImagesClick()
                }) {
                    Text(
                        "Pilih Gambar",
                        color = colors.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            containerColor = colors.cardBackground,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Adaptive canvas: Slate 50 in Light mode, Obsidian Navy in Dark mode
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.deskDark)
    ) {

        // ── Top: Manga collage hero image ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.TopCenter)
        ) {
            Image(
                painter = painterResource(id = R.drawable.manga_collage_bg),
                contentDescription = "Manga Collage Background",
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
            )

            // Smooth multi-stop gradient overlay that blends into canvas background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to colors.deskDark.copy(alpha = if (isDarkTheme) 0.50f else 0.25f),
                                0.25f to colors.deskDark.copy(alpha = if (isDarkTheme) 0.20f else 0.10f),
                                0.50f to colors.deskDark.copy(alpha = if (isDarkTheme) 0.60f else 0.45f),
                                0.78f to colors.deskDark.copy(alpha = 0.92f),
                                1.0f to colors.deskDark
                            )
                        )
                    )
            )

            // Top Right: Theme Toggle
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.cardBackground.copy(alpha = 0.85f))
                    .border(1.dp, colors.borderSubtle, CircleShape)
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
        }


        // ── Bottom Section: Brand, Headline & Action Buttons ───────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // App Branding Mark - Balanced Proportional Lockup
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0284C7), Color(0xFF0EA5E9), Color(0xFF38BDF8))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MenuBook,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Flipz",
                        color = colors.textPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeight = 28.sp,
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        )
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "MANGA",
                        color = colors.primary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.4.sp,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeight = 11.sp,
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Headline & Description (MangaPlus onboarding typography)
            Text(
                text = "Mulai Membaca",
                color = colors.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Jelajahi ribuan chapter manga terbaru secara online atau buka koleksi komik offline favoritmu.",
                color = colors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(20.dp))

            // Continue Reading Banner (if user has previously read an online comic)
            val resumeItem = latestProgress
            if (resumeItem != null && resumeItem.chapterUrl.isNotBlank() && onResumeReading != null) {
                com.example.ui.widgets.ContinueReadingCard(
                    progress = resumeItem,
                    onResume = { onResumeReading(it) },
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }

            // Primary CTA: Full-width Pill Button
            MangaPillButton(
                title = "Baca Online",
                icon = Icons.Rounded.CloudDownload,
                isPrimary = true,
                onClick = onNavigateToReaderOnline
            )

            Spacer(Modifier.height(12.dp))

            // Secondary CTA: Clean Surface Pill Button
            MangaPillButton(
                title = "Import Komik Offline",
                icon = Icons.Rounded.FolderOpen,
                isPrimary = false,
                onClick = { showImportGuide = true }
            )

            Spacer(Modifier.height(16.dp))
            
            // Saweria Banner
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.cardBackground)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                    .clickable { uriHandler.openUri("https://saweria.co/suryaaln") }
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CardGiftcard,
                        contentDescription = "Dukung Kreator",
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Dukung Pengembang",
                            color = colors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Bantu Flipz terus berkembang via Saweria",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (onSupportClick != null) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.pillTint)
                        .border(1.dp, colors.borderSubtle, CircleShape)
                        .clickable(onClick = onSupportClick)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Tonton Iklan Singkat",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Bottom Indicator (like MangaPlus)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(colors.borderSubtle)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "read more. scroll less.",
                color = colors.textMuted,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = "by Surya",
                color = colors.textMuted.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun MangaPillButton(
    title: String,
    icon: ImageVector,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "pill_scale"
    )

    val height = if (isPrimary) 54.dp else 48.dp
    val pillShape = CircleShape

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
            .clip(pillShape)
            .then(
                if (isPrimary) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
                        )
                    )
                } else {
                    Modifier
                        .background(colors.cardBackground)
                        .border(1.dp, colors.borderSubtle, pillShape)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPrimary) Color.White else colors.textPrimary,
                modifier = Modifier.size(if (isPrimary) 22.dp else 19.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                color = if (isPrimary) Color.White else colors.textPrimary,
                fontSize = if (isPrimary) 16.sp else 14.sp,
                fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            )
        }
    }
}
