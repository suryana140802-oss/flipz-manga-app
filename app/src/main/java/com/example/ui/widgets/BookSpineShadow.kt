package com.example.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BookSpineShadow(
  modifier: Modifier = Modifier,
  width: Dp = 44.dp,
) {
  Canvas(
    modifier = modifier
      .width(width)
      .fillMaxHeight()
  ) {
    val midX = size.width / 2f
    val halfWidth = size.width / 2f

    // Left gutter shadow (fade to black at center)
    val leftBrush = Brush.horizontalGradient(
      colors = listOf(Color.Transparent, Color(0x33000000), Color(0x77000000)),
      startX = 0f,
      endX = midX,
    )
    drawRect(
      brush = leftBrush,
      topLeft = Offset.Zero,
      size = Size(halfWidth, size.height),
    )

    // Right gutter shadow (fade from black at center)
    val rightBrush = Brush.horizontalGradient(
      colors = listOf(Color(0x77000000), Color(0x33000000), Color.Transparent),
      startX = midX,
      endX = size.width,
    )
    drawRect(
      brush = rightBrush,
      topLeft = Offset(midX, 0f),
      size = Size(halfWidth, size.height),
    )

    // Sharp center spine crease groove
    val centerLineBrush = Brush.verticalGradient(
      colors = listOf(Color(0x99000000), Color(0xDD000000), Color(0x99000000)),
      startY = 0f,
      endY = size.height,
    )
    drawRect(
      brush = centerLineBrush,
      topLeft = Offset(midX - 1.5f, 0f),
      size = Size(3f, size.height),
    )
  }
}
