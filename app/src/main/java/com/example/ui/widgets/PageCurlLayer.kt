package com.example.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun PageCurlLayer(
  curlProgress: Float, // -1.0 to 1.0
  isRightPage: Boolean,
  showCornerHint: Boolean,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val isCurlingThisPage = (isRightPage && curlProgress > 0f) || (!isRightPage && curlProgress < 0f)
  val progress = if (isCurlingThisPage) abs(curlProgress) else 0f
  val hintRotation = if (showCornerHint && isRightPage && progress == 0f) 3.5f else 0f

  val maxRotation = 85f
  val angle = (progress * maxRotation) + hintRotation
  val rotationY = if (isRightPage) -angle else angle

  Box(modifier = modifier) {
    // Drop Shadow on underlying page
    if (progress > 0.02f) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .shadow(
            elevation = (progress * 28f).dp,
            spotColor = Color.Black.copy(alpha = (progress * 0.7f).coerceIn(0f, 0.85f)),
          )
      )
    }

    // 3D Curling Sheet
    Box(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          this.rotationY = rotationY
          this.cameraDistance = 12f * density
          this.transformOrigin = TransformOrigin(if (isRightPage) 0f else 1f, 0.5f)
        }
    ) {
      content()

      // Dynamic Shading based on curl progress
      if (progress > 0.02f) {
        val shadeBrush = Brush.horizontalGradient(
          colors = if (isRightPage) {
            listOf(
              Color.Transparent,
              Color.Black.copy(alpha = progress * 0.3f),
              Color.Black.copy(alpha = progress * 0.55f),
            )
          } else {
            listOf(
              Color.Black.copy(alpha = progress * 0.55f),
              Color.Black.copy(alpha = progress * 0.3f),
              Color.Transparent,
            )
          }
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(shadeBrush)
        )
      }

      // Subtle folded corner dog-ear hint
      if (showCornerHint && isRightPage && progress == 0f) {
        Canvas(
          modifier = Modifier
            .size(36.dp)
            .align(Alignment.TopEnd)
        ) {
          val foldPath = Path().apply {
            moveTo(size.width - 24.dp.toPx(), 0f)
            lineTo(size.width, 24.dp.toPx())
            lineTo(size.width, 0f)
            close()
          }
          drawPath(foldPath, Color(0x55000000))

          val flapPath = Path().apply {
            moveTo(size.width - 24.dp.toPx(), 0f)
            lineTo(size.width - 24.dp.toPx(), 24.dp.toPx())
            lineTo(size.width, 24.dp.toPx())
            close()
          }
          drawPath(flapPath, Color(0xFFE0E0E0))
          drawPath(flapPath, Color(0xFF9E9E9E), style = Stroke(width = 1.dp.toPx()))
        }
      }
    }
  }
}
