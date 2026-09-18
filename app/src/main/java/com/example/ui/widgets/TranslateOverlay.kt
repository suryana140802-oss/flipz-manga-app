package com.example.ui.widgets

import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.translation.TranslatedBlock
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.R

@Composable
fun TranslateOverlay(
    blocks: List<TranslatedBlock>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val comicFont = FontFamily(Font(R.font.comic_neue_bold))
    
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()
        
        blocks.forEach { block ->
            // Use relative coordinates if available, fallback to absolute with 1f scale
            val useRelative = block.relativeWidth > 0f
            
            val origWidth = if (useRelative) block.relativeWidth * containerWidth else block.boundingBox.width().toFloat()
            val origHeight = if (useRelative) block.relativeHeight * containerHeight else block.boundingBox.height().toFloat()
            
            val origLeft = if (useRelative) block.relativeX * containerWidth else block.boundingBox.left.toFloat()
            val origTop = if (useRelative) block.relativeY * containerHeight else block.boundingBox.top.toFloat()
            
            // Calculate center of the original bounding box
            val centerX = origLeft + origWidth / 2f
            val centerY = origTop + origHeight / 2f
            
            val finalWidth = origWidth
            val finalHeight = origHeight
            
            var x = (centerX - finalWidth / 2f).toInt()
            var y = (centerY - finalHeight / 2f).toInt()
            
            val maxX = maxOf(4, (containerWidth - finalWidth - 4).toInt())
            val maxY = maxOf(4, (containerHeight - finalHeight - 4).toInt())
            x = maxOf(4, minOf(x, maxX))
            y = maxOf(4, minOf(y, maxY))
            
            Box(
                modifier = Modifier
                    .offset { IntOffset(x, y) }
                    .size(
                        width = with(density) { finalWidth.toDp() },
                        height = with(density) { finalHeight.toDp() }
                    )
                    .background(Color(block.backgroundColor).copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                val textLength = block.translatedText.length.coerceAtLeast(1)
                val estimatedArea = finalWidth * finalHeight
                val fontSizePx = kotlin.math.sqrt(estimatedArea / (textLength * 0.6f))
                
                val rawSp = with(density) { ((fontSizePx * 0.7f).toSp()) }
                val minSp = 4.sp
                val maxSp = 16.sp
                val fontSize = if (rawSp < minSp) minSp else if (rawSp > maxSp) maxSp else rawSp
                
                Text(
                    text = block.translatedText,
                    color = Color(block.textColor),
                    fontSize = fontSize,
                    fontFamily = comicFont,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = fontSize * 1.25f,
                )
            }
        }
    }
}
