package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PanelShareHelper {

    fun captureAndSharePanel(
        activity: Activity,
        comicTitle: String,
        chapterTitle: String,
        onComplete: () -> Unit = {}
    ) {
        val window = activity.window
        val view = window.decorView

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val location = IntArray(2)
            view.getLocationInWindow(location)

            PixelCopy.request(
                window,
                Rect(location[0], location[1], location[0] + view.width, location[1] + view.height),
                bitmap,
                { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        processAndShare(activity, bitmap, comicTitle, chapterTitle)
                    } else {
                        Toast.makeText(activity, "Gagal mengambil cuplikan panel", Toast.LENGTH_SHORT).show()
                    }
                    onComplete()
                },
                Handler(Looper.getMainLooper())
            )
        } else {
            try {
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                view.draw(canvas)
                processAndShare(activity, bitmap, comicTitle, chapterTitle)
            } catch (e: Exception) {
                Toast.makeText(activity, "Gagal mengambil cuplikan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            onComplete()
        }
    }

    private fun processAndShare(
        context: Context,
        sourceBitmap: Bitmap,
        comicTitle: String,
        chapterTitle: String
    ) {
        try {
            val watermarked = addWatermark(sourceBitmap, comicTitle, chapterTitle)
            val cacheFolder = File(context.cacheDir, "shared_panels")
            if (!cacheFolder.exists()) cacheFolder.mkdirs()

            val shareFile = File(cacheFolder, "flipz_panel_${System.currentTimeMillis()}.png")
            FileOutputStream(shareFile).use { out ->
                watermarked.compress(Bitmap.CompressFormat.PNG, 95, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                shareFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Cuplikan dari ${comicTitle.ifBlank { "Komik" }} ${if (chapterTitle.isNotBlank()) "• $chapterTitle" else ""}\nDibaca di Flipz Manga 📖✨"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Cuplikan Panel"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Terjadi kesalahan saat membagikan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addWatermark(
        source: Bitmap,
        comicTitle: String,
        chapterTitle: String
    ): Bitmap {
        val bannerHeight = (source.height * 0.07f).toInt().coerceIn(120, 200)
        val result = Bitmap.createBitmap(source.width, source.height + bannerHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw original screenshot
        canvas.drawBitmap(source, 0f, 0f, null)

        // Draw bottom bar background
        val bgPaint = Paint().apply {
            color = Color.parseColor("#121318")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(0f, source.height.toFloat(), source.width.toFloat(), (source.height + bannerHeight).toFloat(), bgPaint)

        // Top accent line of the watermark bar
        val accentPaint = Paint().apply {
            color = Color.parseColor("#E50914") // Manga red
            strokeWidth = 6f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawLine(0f, source.height.toFloat(), source.width.toFloat(), source.height.toFloat(), accentPaint)

        // Brand Text
        val brandPaint = Paint().apply {
            color = Color.WHITE
            textSize = bannerHeight * 0.32f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val textStartX = source.width * 0.05f
        val brandBaseline = source.height + (bannerHeight * 0.45f)
        canvas.drawText("FLIPZ MANGA", textStartX, brandBaseline, brandPaint)

        // Comic & Chapter Info Text
        val subPaint = Paint().apply {
            color = Color.parseColor("#9E9E9E")
            textSize = bannerHeight * 0.22f
            isAntiAlias = true
        }
        val info = if (chapterTitle.isNotBlank()) "$comicTitle • $chapterTitle" else comicTitle
        val safeInfo = if (info.length > 55) info.take(52) + "..." else info
        val subBaseline = source.height + (bannerHeight * 0.80f)
        canvas.drawText(safeInfo, textStartX, subBaseline, subPaint)

        return result
    }
}
