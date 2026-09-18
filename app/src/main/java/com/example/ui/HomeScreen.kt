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
import androidx.compose.material.icons.rounded.Favorite
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
    onNavigateToReaderOnline: () -> Unit,
    onImportArchiveClick: () -> Unit,
    onImportImagesClick: () -> Unit,
    onSupportClick: (() -> Unit)? = null,
    onResumeReading: ((com.example.data.entity.ReadingProgressEntity) -> Unit)? = null
) {
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
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Pilih file arsip (ZIP, RAR, CBZ, CBR) atau pilih sekumpulan gambar chapter langsung dari memori perangkat untuk dibaca secara offline.",
                    color = Color(0xFF94A3B8),
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
                        color = Color(0xFF38BDF8),
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
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color(0xFF94A3B8),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Deep cinematic pure black canvas (MangaPlus onboarding style)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // ── Top: Manga collage hero image ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.60f)
                .align(Alignment.TopCenter)
        ) {
            Image(
                painter = painterResource(id = R.drawable.manga_collage_bg),
                contentDescription = "Manga Collage Background",
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
            )

            // Multi-stop cinematic dark gradient overlay
            // Seamlessly blends from top cover illumination down into pure pitch black
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.40f),  // Status bar readability
                                0.20f to Color.Black.copy(alpha = 0.15f), // Clear cover visibility
                                0.48f to Color.Black.copy(alpha = 0.45f), // Gentle transition begins
                                0.72f to Color.Black.copy(alpha = 0.85f), // Strong fading
                                0.92f to Color.Black,                     // Pure solid black
                                1.0f to Color.Black
                            )
                        )
                    )
            )
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
                        color = Color.White,
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
                        color = Color(0xFF38BDF8),
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
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Jelajahi ribuan chapter manga terbaru secara online atau buka koleksi komik offline favoritmu.",
                color = Color(0xFF94A3B8),
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

            // Secondary CTA: Translucent Glass Pill Button
            MangaPillButton(
                title = "Import Komik Offline",
                icon = Icons.Rounded.FolderOpen,
                isPrimary = false,
                onClick = { showImportGuide = true }
            )

            if (onSupportClick != null) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                        .clickable(onClick = onSupportClick)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFF43F5E),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Dukung Flipz (Tonton Iklan Singkat)",
                        color = Color.White.copy(alpha = 0.7f),
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
                        .background(Color(0xFF38BDF8))
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "read more. scroll less.",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = "by Surya",
                color = Color.White.copy(alpha = 0.18f),
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
                            listOf(Color(0xFF0284C7), Color(0xFF0EA5E9))
                        )
                    )
                } else {
                    Modifier
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), pillShape)
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
                tint = if (isPrimary) Color.White else Color(0xFFE2E8F0),
                modifier = Modifier.size(if (isPrimary) 22.dp else 19.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                color = if (isPrimary) Color.White else Color(0xFFE2E8F0),
                fontSize = if (isPrimary) 16.sp else 14.sp,
                fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            )
        }
    }
}
