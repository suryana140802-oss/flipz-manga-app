package com.example.network

import com.example.domain.model.Chapter
import com.example.domain.model.Comic
import com.example.domain.model.ComicDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class MgkomikScraper(val okHttpClient: OkHttpClient) {

    private val baseUrl = "https://web1.mgkomik.cc"

    suspend fun getLatestComics(
        page: Int = 1,
        status: String = "",   // "ongoing", "completed", ""
        type: String = "",     // "manga", "manhua", "manhwa", ""
        order: String = "latest", // "latest", "trending"
        genre: String = ""
    ): List<Comic> = withContext(Dispatchers.IO) {
        if (genre.isNotBlank()) {
            return@withContext getComicsByGenre(genre, page, type)
        }

        val url = when {
            status == "ongoing" -> "$baseUrl/search/?status=on-going&page=$page"
            status == "completed" -> "$baseUrl/komik/?completed=1&order_by=$order&page=$page"
            type.isNotBlank() -> "$baseUrl/komik/?filter=$type&order_by=$order&page=$page"
            else -> "$baseUrl/komik/?filter=&order_by=$order&page=$page"
        }
        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Failed to load: ${response.code}")
        val html = response.body?.string() ?: throw Exception("Empty body")
        parseComicList(html)
    }

    suspend fun getComicsByGenre(
        genre: String,
        page: Int = 1,
        type: String = ""
    ): List<Comic> = withContext(Dispatchers.IO) {
        val slug = genre.trim().lowercase()
            .replace("&", "")
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

        val typeParam = if (type.isNotBlank()) "&filter=$type" else ""
        val candidateUrls = mutableListOf<String>()

        // 1. Direct genre taxonomy archive endpoints (/genres/$slug/ or /genre/$slug/)
        val pathGenres = if (page == 1) "$baseUrl/genres/$slug/" else "$baseUrl/genres/$slug/page/$page/"
        val pathGenre = if (page == 1) "$baseUrl/genre/$slug/" else "$baseUrl/genre/$slug/page/$page/"
        
        // 2. Query filter parameters on /komik/ or /search/
        val paramKomikBracket = "$baseUrl/komik/?genre%5B%5D=$slug&page=$page$typeParam"
        val paramKomikPlain = "$baseUrl/komik/?genre=$slug&page=$page$typeParam"
        val paramKomikPlural = "$baseUrl/komik/?genres%5B%5D=$slug&page=$page$typeParam"
        val paramSearchBracket = "$baseUrl/search/?genre%5B%5D=$slug&page=$page$typeParam"

        // Put previously successful pattern first for speed
        when (workingGenrePattern) {
            "GENRES_PATH" -> candidateUrls.add(pathGenres)
            "GENRE_PATH" -> candidateUrls.add(pathGenre)
            "KOMIK_BRACKET" -> candidateUrls.add(paramKomikBracket)
            "KOMIK_PLAIN" -> candidateUrls.add(paramKomikPlain)
            "KOMIK_PLURAL" -> candidateUrls.add(paramKomikPlural)
            "SEARCH_BRACKET" -> candidateUrls.add(paramSearchBracket)
        }

        // Add remaining candidate endpoints without duplicates
        listOf(
            pathGenres to "GENRES_PATH",
            pathGenre to "GENRE_PATH",
            paramKomikBracket to "KOMIK_BRACKET",
            paramKomikPlain to "KOMIK_PLAIN",
            paramKomikPlural to "KOMIK_PLURAL",
            paramSearchBracket to "SEARCH_BRACKET"
        ).forEach { (url, _) ->
            if (!candidateUrls.contains(url)) {
                candidateUrls.add(url)
            }
        }

        // Last-resort fallback: plain text query
        val encodedGenre = java.net.URLEncoder.encode(genre, "UTF-8")
        val fallbackSearchUrl = if (page == 1) {
            "$baseUrl/search/?q=$encodedGenre$typeParam"
        } else {
            "$baseUrl/search/?q=$encodedGenre$typeParam&page=$page"
        }
        candidateUrls.add(fallbackSearchUrl)

        for (targetUrl in candidateUrls) {
            try {
                val request = Request.Builder().url(targetUrl).build()
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: continue
                    val comics = parseComicList(html)
                    if (comics.isNotEmpty()) {
                        when (targetUrl) {
                            pathGenres -> workingGenrePattern = "GENRES_PATH"
                            pathGenre -> workingGenrePattern = "GENRE_PATH"
                            paramKomikBracket -> workingGenrePattern = "KOMIK_BRACKET"
                            paramKomikPlain -> workingGenrePattern = "KOMIK_PLAIN"
                            paramKomikPlural -> workingGenrePattern = "KOMIK_PLURAL"
                            paramSearchBracket -> workingGenrePattern = "SEARCH_BRACKET"
                        }
                        android.util.Log.d("MGKOMIK_SCRAPER", "Scraped ${comics.size} comics for genre '$genre' from $targetUrl")
                        return@withContext comics
                    }
                }
            } catch (e: CloudflareException) {
                throw e
            } catch (e: Exception) {
                // Try next candidate endpoint
            }
        }
        emptyList()
    }

    suspend fun searchComics(query: String, page: Int = 1): List<Comic> = withContext(Dispatchers.IO) {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = if (page == 1) "$baseUrl/search/?q=$encodedQuery" else "$baseUrl/search/?q=$encodedQuery&page=$page"
        val request = Request.Builder()
            .url(url)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) return@withContext emptyList()
        val html = response.body?.string() ?: return@withContext emptyList()

        parseComicList(html)
    }

    private fun parseComicList(html: String): List<Comic> {
        val doc = Jsoup.parse(html)
        val comics = mutableListOf<Comic>()

        val items = doc.select(".manga-card, .project-card, .page-item-detail, .c-tabs-item__content, .listupd .bs, .listupd .bsx")
        for (item in items) {
            // Search page: item itself is <a class="manga-card">, so href is on item
            // Komik/Archive page: item is <div class="manga-card">, href is on nested <a>
            val rawUrl = if (item.tagName() == "a") item.attr("href")
                         else item.selectFirst("a[href*='/komik/']")?.attr("href")
                             ?: item.selectFirst("a[href*='/manga/']")?.attr("href")
                             ?: item.selectFirst("a")?.attr("href")
                             ?: continue
            if (rawUrl.isBlank()) continue
            val url = if (rawUrl.startsWith("/")) "$baseUrl$rawUrl" else rawUrl

            val title = item.selectFirst(".manga-title, .post-title, .tt, h3, h4")?.text()?.ifBlank { null }
                ?: item.selectFirst("img")?.attr("alt")?.ifBlank { null }
                ?: continue

            val thumb = item.selectFirst(".manga-cover, img")?.let { img ->
                val src = img.attr("src")
                if (src.isNotBlank() && !src.startsWith("data:")) src
                else img.attr("data-src").ifBlank { img.attr("data-lazy-src") }
            }?.let { if (it.startsWith("/")) "$baseUrl$it" else it } ?: ""
            val ep = item.selectFirst(".chapter-capsule, .epxs, .chapter, .font-meta")?.text() ?: ""

            comics.add(Comic(title, url, thumb, ep))
        }

        // Fallback to old structure
        if (comics.isEmpty()) {
            val oldItems = doc.select(".bs, .bsx, .animepost, .badge-pos-1")
            for (item in oldItems) {
                val a = item.selectFirst("a") ?: continue
                val rawUrl = a.attr("href")
                val url = if (rawUrl.startsWith("/")) "$baseUrl$rawUrl" else rawUrl
                val title = item.selectFirst(".tt, .post-title, h3, h4")?.text() ?: a.attr("title")
                val img = item.selectFirst("img")
                val thumb = img?.let { el ->
                    val src = el.attr("src")
                    if (src.isNotBlank() && !src.startsWith("data:")) src
                    else el.attr("data-src").ifBlank { el.attr("data-lazy-src") }
                }?.let {
                    if (it.startsWith("/")) "$baseUrl$it" else it
                } ?: ""
                val ep = item.selectFirst(".epxs, .chapter")?.text() ?: ""
                comics.add(Comic(title, url, thumb, ep))
            }
        }

        return comics
    }

    suspend fun getComicDetail(comicUrl: String): ComicDetail = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(comicUrl)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Failed to load")
        val html = response.body?.string() ?: throw Exception("Empty body")

        val doc = Jsoup.parse(html, comicUrl)

        val title = doc.selectFirst(".manga-title, .comic-title, .entry-title, h1")?.text() ?: ""

        // Cover image - use the manga-cover-large class or fallback selectors
        val thumb = doc.selectFirst("img.manga-cover-large")?.absUrl("src")
            ?: doc.selectFirst(".manga-cover-wrapper img")?.absUrl("src")
            ?: doc.selectFirst(".comic-cover img, .thumb img, .poster img")?.absUrl("src")
            ?: ""

        // Synopsis
        val synopsis = doc.selectFirst("#sinopsisContainer p")?.text()
            ?: doc.selectFirst("#sinopsisContainer")?.text()
            ?: doc.selectFirst(".sinopsis-preview")?.text()
            ?: doc.selectFirst(".comic-synopsis, .entry-content")?.text()
            ?: ""

        // ── Genres Extraction (3-tier bulletproof extraction) ─────────────
        val genreSet = linkedSetOf<String>()

        // 1. Selector-based search (classes containing genre, tag, category)
        val genreClassElements = doc.select(
            ".genres-content a, .genres-content span, " +
            ".genres a, .genres span, .genres li, " +
            ".genre a, .genre span, .genre li, " +
            ".manga-genres a, .manga-genres span, .manga-genres li, " +
            ".manga-genre a, .manga-genre span, .manga-genre li, " +
            ".genre-info a, .genre-info span, " +
            ".mgen a, .seriestugenre a, " +
            "[class*='genre'] a, [class*='genre'] span, [class*='genre'] li, " +
            "[class*='tag'] a, [class*='tag'] span, " +
            "a[href*='/genre/'], a[href*='/genres/'], a[href*='genre='], " +
            "a[href*='/tag/'], a[href*='/tags/']"
        )
        for (el in genreClassElements) {
            val t = el.text().trim()
            if (t.isNotBlank() &&
                !t.equals("genre", ignoreCase = true) &&
                !t.equals("genres", ignoreCase = true) &&
                !t.equals("genre(s)", ignoreCase = true) &&
                !t.equals("daftar genre", ignoreCase = true) &&
                !t.equals("all genres", ignoreCase = true) &&
                !t.contains("filter", ignoreCase = true) &&
                t.length in 2..28 &&
                !t.contains(":") && !t.contains("\n")
            ) {
                genreSet.add(t)
            }
        }

        // 2. Text-based search: Find "Genre:" or "Genre(s):" in any text container
        if (genreSet.isEmpty()) {
            val genreContainers = doc.select("*:matchesOwn((?i)^\\s*genres?\\s*\\(?s?\\)?\\s*[:\\-])")
            for (container in genreContainers) {
                // Check if there are child links or spans
                val children = container.parent()?.select("a, span, li") ?: emptyList()
                for (child in children) {
                    val t = child.text().trim()
                    if (t.isNotBlank() && !t.contains("genre", ignoreCase = true) && t.length in 2..28) {
                        genreSet.add(t)
                    }
                }
                // Also check plain text after "Genre:"
                val fullText = container.parent()?.text() ?: container.text()
                val match = Regex("(?i)genres?\\s*\\(?s?\\)?\\s*[:\\-]?\\s*([^|\\n]+)").find(fullText)
                if (match != null) {
                    val rawList = match.groupValues[1].split(",", "•", "/", ";")
                    for (item in rawList) {
                        val clean = item.trim()
                        if (clean.isNotBlank() && clean.length in 2..28 && !clean.contains("genre", ignoreCase = true)) {
                            genreSet.add(clean)
                        }
                    }
                }
                if (genreSet.isNotEmpty()) break
            }
        }

        // 3. Fallback: Scan metadata / header area for known comic genres
        if (genreSet.isEmpty()) {
            val knownGenres = listOf(
                "Action", "Adventure", "Comedy", "Drama", "Fantasy",
                "Isekai", "Magic", "Manhua", "Manhwa", "Martial Arts",
                "Mystery", "Romance", "School Life", "Sci-Fi", "Shounen",
                "Supernatural", "Seinen", "Shoujo", "Slice of Life",
                "Historical", "Psychological", "Tragedy", "Harem", "Ecchi",
                "Horror", "Game", "Sports", "Time Travel", "Reincarnation",
                "Villainess", "Murim", "Cultivation", "System", "Josei",
                "Mecha", "Military", "Music", "Police", "Post-Apocalyptic",
                "Rebirth", "Survival", "Thriller", "Vampire", "Webtoon", "Webtoons"
            )
            val candidateElements = doc.select(".manga-info *, .series-info *, .info-desc *, .entry-content *, .post-content *, .bixbox *, .thumb *, .summary *")
                .ifEmpty { doc.select("a, span, li, b, strong, button") }

            for (el in candidateElements) {
                val t = el.ownText().trim().ifBlank { el.text().trim() }
                for (kg in knownGenres) {
                    if (t.equals(kg, ignoreCase = true)) {
                        genreSet.add(kg)
                        break
                    }
                }
            }
        }

        val genres = genreSet.toList()
        android.util.Log.e("MGKOMIK_GENRE", "Scraped ${genres.size} genres for $title: $genres")

        val chapters = mutableListOf<Chapter>()

        // New structure: #chapterList li.chapter-list-item
        val chapterElements = doc.select("#chapterList .chapter-list-item, #chapterlist li, .chapter-list li")
        for (el in chapterElements) {
            val a = el.selectFirst("a.chapter-link, a") ?: continue
            val cRawUrl = a.attr("href")
            val cUrl = if (cRawUrl.startsWith("/")) "$baseUrl$cRawUrl" else cRawUrl
            val cTitle = el.selectFirst(".chapter-number, .chapternum, .chapter-num")?.text()
                ?: a.text()
            val cDate = el.selectFirst(".chapter-date, .chapterdate")?.text() ?: ""
            if (cUrl.isNotBlank()) {
                chapters.add(Chapter(cTitle, cUrl, cDate))
            }
        }

        ComicDetail(title, comicUrl, thumb, synopsis, chapters, genres)
    }

    suspend fun getChapterPages(chapterUrl: String): List<String> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(chapterUrl)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Failed to load")
        val html = response.body?.string() ?: throw Exception("Empty body")

        val doc = Jsoup.parse(html)
        val pages = mutableListOf<String>()

        val imgs = doc.select("#readingContent img, .reading-content img, #readerarea img, .reader-area img, .chapter-image img")
        for (img in imgs) {
            val src = img.attr("src")
            if (src.isNotEmpty()) pages.add(src)
        }
        pages
    }

    companion object {
        @Volatile
        private var workingGenrePattern: String? = null
    }
}
