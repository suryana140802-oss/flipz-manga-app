package com.example.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class MLKitTranslator {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun isModelDownloaded(sourceLang: String): Boolean {
        // No offline model needed for Web API
        return true
    }

    suspend fun downloadModel(sourceLang: String) {
        // No offline model needed for Web API
    }

    suspend fun translateBlocks(blocks: List<String>, sourceLang: String): List<String> {
        return withContext(Dispatchers.IO) {
            val translatedBlocks = mutableListOf<String>()
            for (block in blocks) {
                if (block.isBlank()) {
                    translatedBlocks.add(block)
                    continue
                }
                
                try {
                    val encodedText = URLEncoder.encode(block, "UTF-8")
                    val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sourceLang&tl=id&dt=t&q=$encodedText"
                    
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0")
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                        // Response format: [[["Translated text","Original text",null,null,1]],null,"en"]
                        val jsonArray = JSONArray(responseBody)
                        val sentencesArray = jsonArray.optJSONArray(0)
                        
                        val translatedTextBuilder = StringBuilder()
                        if (sentencesArray != null) {
                            for (i in 0 until sentencesArray.length()) {
                                val sentenceParts = sentencesArray.optJSONArray(i)
                                if (sentenceParts != null) {
                                    val part = sentenceParts.optString(0, "")
                                    translatedTextBuilder.append(part)
                                }
                            }
                        }
                        
                        val translated = translatedTextBuilder.toString()
                        if (translated.isNotEmpty()) {
                            translatedBlocks.add(translated)
                        } else {
                            translatedBlocks.add("[Translate Error]")
                        }
                    } else {
                        translatedBlocks.add("[Failed: ${response.code}]")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    translatedBlocks.add("[Error]")
                }
            }
            translatedBlocks
        }
    }

    fun close() {
        // Cleanup if necessary
    }
}
