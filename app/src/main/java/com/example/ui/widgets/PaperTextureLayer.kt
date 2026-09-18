package com.example.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.model.PaperTexture
import com.example.ui.theme.PaperClean
import com.example.ui.theme.PaperMangaPulp
import com.example.ui.theme.PaperMatte
import com.example.ui.theme.PaperVintage
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun PaperTextureLayer(
  texture: PaperTexture,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Box(modifier = modifier) {
    content()

    if (texture == PaperTexture.CLEAN) return@Box

    val tintColor: Color
    val grainAlpha: Float
    val isVintage: Boolean

    when (texture) {
      PaperTexture.MATTE -> {
        tintColor = PaperMatte.copy(alpha = 0.25f)
        grainAlpha = 0.04f
        isVintage = false
      }
      PaperTexture.MANGA_PULP -> {
        tintColor = PaperMangaPulp.copy(alpha = 0.35f)
        grainAlpha = 0.10f
        isVintage = false
      }
      PaperTexture.VINTAGE -> {
        tintColor = PaperVintage.copy(alpha = 0.45f)
        grainAlpha = 0.18f
        isVintage = true
      }
      PaperTexture.CLEAN -> return@Box
    }

    // Color Tint Overlay
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(tintColor)
    )

    // Tactile Fiber Grain Texture Canvas
    Canvas(modifier = Modifier.fillMaxSize()) {
      val random = Random(42) // Deterministic seed
      val step = 14f

      var x = 0f
      while (x < size.width) {
        var y = 0f
        while (y < size.height) {
          if (random.nextFloat() > 0.65f) {
            val len = 2f + random.nextFloat() * 4f
            val angle = random.nextFloat() * Math.PI.toFloat()
            val dx = cos(angle) * len
            val dy = sin(angle) * len
            drawLine(
              color = Color.Black.copy(alpha = grainAlpha),
              start = Offset(x, y),
              end = Offset(x + dx, y + dy),
              strokeWidth = 1f,
            )
          }
          y += step
        }
        x += step
      }

      if (isVintage) {
        // Aged corner discoloration vignette
        val vignette = Brush.radialGradient(
          colors = listOf(
            Color.Transparent,
            Color(0x224E3629),
            Color(0x553E2723),
          ),
          center = Offset(size.width / 2f, size.height / 2f),
          radius = (size.width.coerceAtLeast(size.height)) * 0.75f,
        )
        drawRect(
          brush = vignette,
          blendMode = BlendMode.Multiply,
        )
      }
    }
  }
}
