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
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F1218))
            .border(1.dp, Color(0xFF1E232E), RoundedCornerShape(12.dp))
            .clickable { onResume(progress) }
            .padding(10.dp)
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
                        .size(width = 44.dp, height = 60.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1C202A))
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
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(2.dp))

                val chapterTxt = progress.chapterTitle.ifBlank { "Chapter Aktif" }
                Text(
                    text = "$chapterTxt • Hal. $current/$total",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(5.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progressRatio },
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = colors.primary,
                    trackColor = Color(0xFF1E232E)
                )
            }

            Spacer(Modifier.width(8.dp))

            // Play / Resume Circular Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Lanjut Baca",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
