package com.example.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppColors

/**
 * Komponen Empty State modern minimalis sesuai referensi visual gambar:
 * Ilustrasi tumpukan kartu pastel berlapis, floating badge +, headline tebal, dan tombol pill aksi.
 */
@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- Vector Illustration Layer ---
        Box(
            modifier = Modifier.size(190.dp, 160.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(170.dp, 140.dp)) {
                val w = size.width
                val h = size.height

                // Spark / doodle lines (top right)
                drawLine(
                    color = Color(0xFF1E293B).copy(alpha = 0.7f),
                    start = Offset(w * 0.85f, h * 0.08f),
                    end = Offset(w * 0.90f, h * 0.03f),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color(0xFF1E293B).copy(alpha = 0.7f),
                    start = Offset(w * 0.88f, h * 0.14f),
                    end = Offset(w * 0.95f, h * 0.11f),
                    strokeWidth = 3f
                )

                // Doodle curved arrow (top left)
                val arrowPath = Path().apply {
                    moveTo(w * 0.12f, h * 0.10f)
                    cubicTo(w * 0.16f, h * 0.02f, w * 0.24f, h * 0.03f, w * 0.26f, h * 0.12f)
                }
                drawPath(
                    path = arrowPath,
                    color = Color(0xFF1E293B).copy(alpha = 0.7f),
                    style = Stroke(width = 3f)
                )
                // Arrow tip
                drawLine(
                    color = Color(0xFF1E293B).copy(alpha = 0.7f),
                    start = Offset(w * 0.26f, h * 0.12f),
                    end = Offset(w * 0.23f, h * 0.09f),
                    strokeWidth = 3f
                )

                // 1. Back Layer: Soft Cyan Card
                drawRoundRect(
                    color = Color(0xFF5EEAD4), // Mint/Cyan
                    topLeft = Offset(w * 0.22f, h * 0.16f),
                    size = Size(w * 0.65f, h * 0.55f),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )

                // 2. Middle Layer: Warm Yellow / Orange Card
                drawRoundRect(
                    color = Color(0xFFFDE047), // Pastel Yellow
                    topLeft = Offset(w * 0.14f, h * 0.22f),
                    size = Size(w * 0.68f, h * 0.55f),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )

                // 3. Middle Layer: Bright Pink Card
                drawRoundRect(
                    color = Color(0xFFF472B6), // Pastel Pink
                    topLeft = Offset(w * 0.20f, h * 0.28f),
                    size = Size(w * 0.65f, h * 0.55f),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )

                // 4. Front Layer: Main Blue Folder with Wave Texture
                val folderCorner = 18.dp.toPx()
                drawRoundRect(
                    color = Color(0xFF2563EB), // Rich Sky Blue
                    topLeft = Offset(w * 0.12f, h * 0.40f),
                    size = Size(w * 0.76f, h * 0.56f),
                    cornerRadius = CornerRadius(folderCorner)
                )

                // Gentle wave highlight on the front folder
                val wavePath = Path().apply {
                    moveTo(w * 0.12f, h * 0.58f)
                    cubicTo(w * 0.35f, h * 0.52f, w * 0.55f, h * 0.68f, w * 0.88f, h * 0.60f)
                    lineTo(w * 0.88f, h * 0.96f)
                    lineTo(w * 0.12f, h * 0.96f)
                    close()
                }
                drawPath(
                    path = wavePath,
                    color = Color(0xFF1D4ED8).copy(alpha = 0.5f)
                )
            }

            // Top-right floating black circular badge with '+' icon
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 26.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Headline
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.3).sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Subtitle
        Text(
            text = subtitle,
            color = colors.textSecondary,
            fontSize = 13.5.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        // Action Pill Button (Matching the "+ Create Project" button in reference image)
        if (!actionButtonText.isNullOrBlank() && onActionClick != null) {
            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.pillTint)
                    .clickable { onActionClick() }
                    .padding(horizontal = 22.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = actionButtonText,
                        color = colors.primary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
