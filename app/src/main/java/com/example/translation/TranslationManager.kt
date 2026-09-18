package com.example.translation

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.nl.translate.TranslateLanguage
import kotlinx.coroutines.tasks.await

data class TranslatedBlock(
    val boundingBox: android.graphics.Rect,
    val relativeX: Float = 0f,
    val relativeY: Float = 0f,
    val relativeWidth: Float = 0f,
    val relativeHeight: Float = 0f,
    val originalText: String,
    val translatedText: String,
    val backgroundColor: Int = android.graphics.Color.WHITE,
    val textColor: Int = android.graphics.Color.BLACK
)

class TranslationManager {
    private val TAG = "TranslationManager"
    
    // Recognizers
    private val japaneseRecognizer: TextRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    private val latinRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val chineseRecognizer: TextRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    
    val mlkitTranslator by lazy { MLKitTranslator() }

    suspend fun processImage(bitmap: Bitmap, sourceLang: String): List<TranslatedBlock> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val translatedBlocks = mutableListOf<TranslatedBlock>()

        try {
            kotlinx.coroutines.withTimeoutOrNull(20000) {
                val recognizer = when (sourceLang) {
                    TranslateLanguage.JAPANESE -> japaneseRecognizer
                    TranslateLanguage.ENGLISH -> latinRecognizer
                    TranslateLanguage.CHINESE -> chineseRecognizer
                    else -> latinRecognizer
                }
                
                val text = recognizer.process(image).await()
                val isAsian = sourceLang == TranslateLanguage.JAPANESE || sourceLang == TranslateLanguage.CHINESE
                processTextBlocksWithMLKit(text, translatedBlocks, bitmap, isAsian, sourceLang)
            }
        } catch (e: Throwable) {
            if (e !is kotlinx.coroutines.CancellationException) {
                Log.e(TAG, "Error processing image for translation", e)
                throw e
            }
        }

        return translatedBlocks
    }

    private fun rectDistance(r1: android.graphics.Rect, r2: android.graphics.Rect): Int {
        val left = maxOf(r1.left, r2.left)
        val right = minOf(r1.right, r2.right)
        val top = maxOf(r1.top, r2.top)
        val bottom = minOf(r1.bottom, r2.bottom)

        val dx = maxOf(0, left - right)
        val dy = maxOf(0, top - bottom)
        return kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toInt()
    }

    private class BlockCluster(
        var boundingBox: android.graphics.Rect,
        val blocks: MutableList<Text.TextBlock>
    )

    private suspend fun processTextBlocksWithMLKit(
        text: Text, 
        results: MutableList<TranslatedBlock>, 
        bitmap: Bitmap, 
        isAsian: Boolean,
        sourceLang: String
    ): Boolean {
        val imageWidth = bitmap.width
        val imageHeight = bitmap.height
        
        // Cluster blocks that are physically close to each other (belong to the same bubble)
        val clusters = text.textBlocks
            .filter { it.text.isNotBlank() && it.boundingBox != null }
            .map { BlockCluster(android.graphics.Rect(it.boundingBox!!), mutableListOf(it)) }
            .toMutableList()
            
        var changed = true
        val distanceThreshold = (imageWidth * 0.10).toInt().coerceAtLeast(60)
        
        while (changed) {
            changed = false
            for (i in 0 until clusters.size) {
                for (j in i + 1 until clusters.size) {
                    val c1 = clusters[i]
                    val c2 = clusters[j]
                    val isClose = c1.blocks.any { b1 ->
                        c2.blocks.any { b2 ->
                            rectDistance(b1.boundingBox!!, b2.boundingBox!!) < distanceThreshold
                        }
                    }
                    if (isClose) {
                        c1.blocks.addAll(c2.blocks)
                        c1.boundingBox.union(c2.boundingBox)
                        clusters.removeAt(j)
                        changed = true
                        break
                    }
                }
                if (changed) break
            }
        }
        
        // Extract original blocks and calculate colors
        val validClusters = mutableListOf<Triple<BlockCluster, Int, Int>>()
        val originalTexts = mutableListOf<String>()
        
        for (cluster in clusters) {
            val rect = cluster.boundingBox
            
            // Sort blocks inside cluster for proper reading order
            if (isAsian) {
                cluster.blocks.sortByDescending { it.boundingBox!!.right }
            } else {
                cluster.blocks.sortWith(compareBy({ it.boundingBox!!.top }, { it.boundingBox!!.left }))
            }
            
            val originalText = cluster.blocks.joinToString(" ") { it.text.replace("\n", " ") }.trim()
            if (originalText.isNotBlank()) {
                
                val padding = 5
                val sampleRect = android.graphics.Rect(
                    (rect.left - padding).coerceAtLeast(0),
                    (rect.top - padding).coerceAtLeast(0),
                    (rect.right + padding).coerceAtMost(imageWidth - 1),
                    (rect.bottom + padding).coerceAtMost(imageHeight - 1)
                )
                
                var lightPixels = 0
                var darkPixels = 0
                
                val points = listOf(
                    android.graphics.Point(sampleRect.left, sampleRect.top),
                    android.graphics.Point(sampleRect.right, sampleRect.top),
                    android.graphics.Point(sampleRect.left, sampleRect.bottom),
                    android.graphics.Point(sampleRect.right, sampleRect.bottom),
                    android.graphics.Point(sampleRect.centerX(), sampleRect.top),
                    android.graphics.Point(sampleRect.centerX(), sampleRect.bottom),
                    android.graphics.Point(sampleRect.left, sampleRect.centerY()),
                    android.graphics.Point(sampleRect.right, sampleRect.centerY())
                )
                
                for (pt in points) {
                    val pixel = bitmap.getPixel(pt.x, pt.y)
                    val r = android.graphics.Color.red(pixel)
                    val g = android.graphics.Color.green(pixel)
                    val b = android.graphics.Color.blue(pixel)
                    val luminance = 0.299 * r + 0.587 * g + 0.114 * b
                    if (luminance > 128) lightPixels++ else darkPixels++
                }
                
                val isDarkBubble = darkPixels > lightPixels
                val bgColor = if (isDarkBubble) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                val txtColor = if (isDarkBubble) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                validClusters.add(Triple(cluster, bgColor, txtColor))
                originalTexts.add(originalText)
            }
        }

        if (originalTexts.isNotEmpty()) {
            val translatedTexts = try {
                mlkitTranslator.translateBlocks(originalTexts, sourceLang)
            } catch (e: Throwable) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    Log.e(TAG, "ML Kit translation failed", e)
                    throw e
                }
                return false
            }
            
            if (translatedTexts.isEmpty()) {
                Log.e(TAG, "ML Kit returned empty translation")
                throw Exception("ML Kit returned empty translation")
            }
            
            validClusters.forEachIndexed { index, (cluster, bgColor, txtColor) ->
                val rect = cluster.boundingBox
                val translatedText = translatedTexts.getOrNull(index) ?: "Translation Error"
                
                // Keep the original originalText format just for reference, joined
                val combinedOriginalText = cluster.blocks.joinToString(" ") { it.text.replace("\n", " ") }
                
                results.add(
                    TranslatedBlock(
                        boundingBox = rect,
                        relativeX = rect.left.toFloat() / imageWidth,
                        relativeY = rect.top.toFloat() / imageHeight,
                        relativeWidth = rect.width().toFloat() / imageWidth,
                        relativeHeight = rect.height().toFloat() / imageHeight,
                        originalText = combinedOriginalText,
                        translatedText = translatedText,
                        backgroundColor = bgColor,
                        textColor = txtColor
                    )
                )
            }
        }
        return true
    }

    fun close() {
        japaneseRecognizer.close()
        latinRecognizer.close()
        chineseRecognizer.close()
        mlkitTranslator.close()
    }
}
