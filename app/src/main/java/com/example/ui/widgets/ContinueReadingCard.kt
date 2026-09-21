package com.example.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.entity.ReadingProgressEntity
import com.example.ui.theme.LocalAppColors

@Composable
fun ContinueReadingCard(
    progress: ReadingProgressEntity,
    onResume: (ReadingProgressEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val total = progress.totalPages.coerceAtLeast(1)
    val current = (progress.pageIndex + 1).coerceIn(1, total)
    val progressRatio = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardBackground)
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
            .clickable { onResume(progress) }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Thumbnail
            if (progress.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = progress.coverUrl,
                    contentDescription = progress.comicTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 46.dp, height = 62.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.deskMedium)
                )
                Spacer(Modifier.width(12.dp))
            }

            // Info Column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "LANJUTKAN MEMBACA",
                        color = colors.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = progress.comicTitle.ifBlank { "Komik Terakhir" },
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(2.dp))

                val chapterTxt = progress.chapterTitle.ifBlank { "Chapter Aktif" }
                Text(
                    text = "$chapterTxt • Hal. $current/$total",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progressRatio },
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = colors.primary,
                    trackColor = colors.borderSubtle
                )
            }

            Spacer(Modifier.width(8.dp))

            // Play / Resume Circular Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Lanjut Baca",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
