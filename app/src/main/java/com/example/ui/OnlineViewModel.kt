package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Comic
import com.example.network.CloudflareException
import com.example.network.CloudflareSolver
import com.example.network.CookieCache
import com.example.network.MgkomikScraper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.example.data.dao.BookmarkDao
import kotlinx.coroutines.flow.first

sealed class ComicFilter(val label: String, val status: String = "", val type: String = "") {
    object Latest    : ComicFilter("Terbaru")
    object Popular   : ComicFilter("Populer")
    object Ongoing   : ComicFilter("Ongoing",   status = "ongoing")
    object Completed : ComicFilter("Completed", status = "completed")
    object Bookmark  : ComicFilter("Favorit")
    object Manga     : ComicFilter("Manga",     type = "manga")
    object Manhua    : ComicFilter("Manhua",    type = "manhua")
    object Manhwa    : ComicFilter("Manhwa",    type = "manhwa")
}

val statusFilters = listOf(
    ComicFilter.Latest,
    ComicFilter.Popular,
    ComicFilter.Ongoing,
    ComicFilter.Completed,
    ComicFilter.Bookmark,
)

val typeFilters = listOf(
    ComicFilter.Manga,
    ComicFilter.Manhua,
    ComicFilter.Manhwa,
)

class OnlineViewModel(
    private val scraper: MgkomikScraper,
    private val bookmarkDao: BookmarkDao? = null
) : ViewModel() {

    private val _comics = MutableStateFlow<List<Comic>>(emptyList())
    val comics: StateFlow<List<Comic>> = _comics.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _needsCloudflareBypass = MutableStateFlow(false)
    val needsCloudflareBypass: StateFlow<Boolean> = _needsCloudflareBypass.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _activeStatusFilter = MutableStateFlow<ComicFilter>(ComicFilter.Latest)
    val activeStatusFilter: StateFlow<ComicFilter> = _activeStatusFilter.asStateFlow()

    private val _activeTypeFilter = MutableStateFlow<ComicFilter?>(null)
    val activeTypeFilter: StateFlow<ComicFilter?> = _activeTypeFilter.asStateFlow()

    private val _activeGenreFilter = MutableStateFlow<String?>(null)
    val activeGenreFilter: StateFlow<String?> = _activeGenreFilter.asStateFlow()

    private var currentPage = 1
    private var isLastPage = false
    private var loadJob: Job? = null   // ponytail: single job ref for cancellation
    private var searchJob: Job? = null

    init {
        loadLatestComics()
        viewModelScope.launch {
            bookmarkDao?.getAllBookmarks()?.collect { bookmarks ->
                if (_activeStatusFilter.value == ComicFilter.Bookmark) {
                    _comics.value = bookmarks.map { b ->
                        Comic(
                            title = b.title,
                            url = b.comicUrl,
                            thumbnailUrl = b.coverUrl,
                            latestChapter = b.latestChapter.ifBlank { "Tersimpan" }
                        )
                    }
                    isLastPage = true
                    _isLoading.value = false
                }
            }
        }
    }

    fun setStatusFilter(filter: ComicFilter) {
        _activeStatusFilter.value = filter
        loadLatestComics()
    }

    fun setTypeFilter(filter: ComicFilter?) {
        _activeTypeFilter.value = filter
        loadLatestComics()
    }

    fun setGenreFilter(genre: String?) {
        _activeGenreFilter.value = genre
        loadLatestComics()
    }

    fun loadLatestComics() {
        loadJob?.cancel()   // cancel any in-flight load
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _needsCloudflareBypass.value = false
            _isSearchActive.value = false
            _searchQuery.value = ""
            currentPage = 1
            isLastPage = false

            if (_activeStatusFilter.value == ComicFilter.Bookmark) {
                try {
                    val bookmarks = bookmarkDao?.getAllBookmarks()?.first() ?: emptyList()
                    _comics.value = bookmarks.map { b ->
                        Comic(
                            title = b.title,
                            url = b.comicUrl,
                            thumbnailUrl = b.coverUrl,
                            latestChapter = b.latestChapter.ifBlank { "Tersimpan" }
                        )
                    }
                    isLastPage = true
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    _isLoading.value = false
                }
                return@launch
            }

            val cachedCookie = CookieCache.getCachedCookie()
            val cachedUA = CookieCache.getCachedUserAgent()
            if (!cachedCookie.isNullOrBlank()) {
                CloudflareSolver.cfClearanceCookie = cachedCookie
                if (!cachedUA.isNullOrBlank()) CloudflareSolver.userAgent = cachedUA
            }

            try {
                val sf = _activeStatusFilter.value
                val tf = _activeTypeFilter.value
                val genre = _activeGenreFilter.value ?: ""
                val order  = if (sf == ComicFilter.Popular) "trending" else "latest"
                val status = if (sf == ComicFilter.Popular || sf == ComicFilter.Latest) "" else sf.status
                val type   = tf?.type ?: ""

                val result = scraper.getLatestComics(currentPage, status, type, order, genre)
                val uniqueResult = result.distinctBy { it.url }
                _comics.value = uniqueResult
                if (uniqueResult.isEmpty()) isLastPage = true
            } catch (e: CloudflareException) {
                CookieCache.clear()
                _needsCloudflareBypass.value = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreComics() {
        if (_isLoading.value || isLastPage || _isSearchActive.value || _activeStatusFilter.value == ComicFilter.Bookmark) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                currentPage++
                val sf = _activeStatusFilter.value
                val tf = _activeTypeFilter.value
                val genre = _activeGenreFilter.value ?: ""
                val order  = if (sf == ComicFilter.Popular) "popular" else "update"
                val status = if (sf == ComicFilter.Popular || sf == ComicFilter.Latest) "" else sf.status
                val type   = tf?.type ?: ""

                val more = scraper.getLatestComics(currentPage, status, type, order, genre)
                if (more.isEmpty()) {
                    isLastPage = true
                } else {
                    val currentUrls = _comics.value.map { it.url }.toSet()
                    val newItems = more.distinctBy { it.url }.filter { it.url !in currentUrls }
                    if (newItems.isEmpty()) {
                        isLastPage = true
                    } else {
                        _comics.value = _comics.value + newItems
                    }
                }
            } catch (e: CloudflareException) {
                _needsCloudflareBypass.value = true; currentPage--
            } catch (e: Exception) {
                currentPage--; e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) { loadLatestComics(); return }
        searchJob = viewModelScope.launch {
            delay(600)
            performSearch(query)
        }
    }

    private suspend fun performSearch(query: String) {
        _isLoading.value = true
        _isSearchActive.value = true
        try {
            _comics.value = scraper.searchComics(query)
        } catch (e: CloudflareException) {
            _needsCloudflareBypass.value = true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isLoading.value = false
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        loadLatestComics()
    }

    fun onCloudflareBypassSuccess() {
        _needsCloudflareBypass.value = false
        if (_isSearchActive.value && _searchQuery.value.isNotBlank()) {
            viewModelScope.launch { performSearch(_searchQuery.value) }
        } else {
            loadLatestComics()
        }
    }

    fun exportBookmarks(context: Context, uri: Uri, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val bookmarks = bookmarkDao?.getAllBookmarksList() ?: emptyList()
                if (bookmarks.isEmpty()) {
                    onError("Tidak ada komik di daftar Favorit untuk dicadangkan")
                    return@launch
                }
                val result = com.example.util.BookmarkBackupHelper.exportBookmarksToJson(context, bookmarks, uri)
                result.fold(
                    onSuccess = { count -> onSuccess(count) },
                    onFailure = { e -> onError(e.message ?: "Gagal mencadangkan favorit") }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun importBookmarks(context: Context, uri: Uri, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = com.example.util.BookmarkBackupHelper.importBookmarksFromJson(context, uri)
                result.fold(
                    onSuccess = { importedList ->
                        if (importedList.isEmpty()) {
                            onError("File tidak berisi data komik yang valid")
                            return@fold
                        }
                        bookmarkDao?.insertBookmarks(importedList)
                        onSuccess(importedList.size)
                        if (_activeStatusFilter.value == ComicFilter.Bookmark) {
                            loadLatestComics()
                        }
                    },
                    onFailure = { e -> onError(e.message ?: "Gagal membaca file cadangan") }
                )
            } catch (e: Exception) {
                onError(e.message ?: "Terjadi kesalahan")
            }
        }
    }
}
