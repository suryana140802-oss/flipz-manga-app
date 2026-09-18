package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

class AudioHapticService(private val context: Context) {

  private val coroutineScope = CoroutineScope(Dispatchers.Default)
  var isSoundEnabled = true

  private val vibrator: Vibrator? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      manager?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
  }

  fun playPageFlipFeedback() {
    // 1. Tactile Haptic Tick
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(18)
      }
    } catch (_: Exception) {}

    // 2. Synthesized Physical Paper Rustle Audio
    if (!isSoundEnabled) return

    coroutineScope.launch {
      synthesizePaperRustle()
    }
  }

  private fun synthesizePaperRustle() {
    try {
      val sampleRate = 22050
      val durationMs = 120
      val numSamples = (sampleRate * durationMs) / 1000
      val buffer = ShortArray(numSamples)
      val random = Random(42)

      // Generate shaped paper slide noise with exponential decay envelope
      for (i in 0 until numSamples) {
        val t = i.toFloat() / numSamples
        // Envelope: quick attack, smooth friction decay
        val envelope = (1f - t) * (1f - t) * (if (t < 0.15f) t / 0.15f else 1f)
        // High-frequency friction noise mixed with paper resonance
        val noise = (random.nextFloat() * 2f - 1f) * 0.7f
        val resonance = sin(2.0 * Math.PI * 850.0 * (i.toDouble() / sampleRate)).toFloat() * 0.3f
        val sampleVal = ((noise + resonance) * envelope * Short.MAX_VALUE * 0.35f).toInt()
        buffer[i] = sampleVal.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
      }

      val track = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(buffer.size * 2)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

      track.write(buffer, 0, buffer.size)
      track.play()
      // Release after playback completes
      Thread.sleep(durationMs.toLong() + 30)
      track.release()
    } catch (_: Exception) {}
  }
}
