package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.entity.BookmarkEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object BookmarkBackupHelper {

    fun exportBookmarksToJson(context: Context, bookmarks: List<BookmarkEntity>, uri: Uri): Result<Int> {
        return try {
            val jsonArray = JSONArray()
            bookmarks.forEach { b ->
                val obj = JSONObject().apply {
                    put("comicUrl", b.comicUrl)
                    put("title", b.title)
                    put("coverUrl", b.coverUrl)
                    put("latestChapter", b.latestChapter)
                    put("timestamp", b.timestamp)
                }
                jsonArray.put(obj)
            }

            val root = JSONObject().apply {
                put("app", "Flipz Manga")
                put("version", 1)
                put("exportedAt", System.currentTimeMillis())
                put("totalCount", bookmarks.size)
                put("bookmarks", jsonArray)
            }

            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                    writer.write(root.toString(2))
                }
            } ?: return Result.failure(Exception("Tidak dapat membuka file penyimpanan"))

            Result.success(bookmarks.size)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun importBookmarksFromJson(context: Context, uri: Uri): Result<List<BookmarkEntity>> {
        return try {
            val content = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { `is` ->
                BufferedReader(InputStreamReader(`is`, Charsets.UTF_8)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        content.append(line)
                        line = reader.readLine()
                    }
                }
            } ?: return Result.failure(Exception("Tidak dapat membaca file"))

            val root = JSONObject(content.toString())
            val array = if (root.has("bookmarks")) {
                root.getJSONArray("bookmarks")
            } else {
                JSONArray(content.toString())
            }

            val list = mutableListOf<BookmarkEntity>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val comicUrl = obj.optString("comicUrl", "")
                val title = obj.optString("title", "")
                val coverUrl = obj.optString("coverUrl", "")
                val latestChapter = obj.optString("latestChapter", "")
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())

                if (comicUrl.isNotBlank() && title.isNotBlank()) {
                    list.add(
                        BookmarkEntity(
                            comicUrl = comicUrl,
                            title = title,
                            coverUrl = coverUrl,
                            latestChapter = latestChapter,
                            timestamp = timestamp
                        )
                    )
                }
            }

            Result.success(list)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
