package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ComicDatabase
import com.example.data.repository.ReadingProgressRepository
import com.example.model.ComicBook
import com.example.model.ComicPage
import com.example.model.PaperTexture
import com.example.model.ReadingDirection
import com.example.service.ComicArchiveParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlin.math.ceil

data class ReaderUiState(
  val currentBook: ComicBook = ComicArchiveParser.getEmptyComic(),
  val currentSpreadIndex: Int = 0,
  val currentSinglePageIndex: Int = 0,
  val portraitMode: com.example.model.PortraitReadingMode = com.example.model.PortraitReadingMode.LONG_STRIP,
  val readingDirection: ReadingDirection = ReadingDirection.RTL,
  val paperTexture: PaperTexture = PaperTexture.CLEAN,
  val curlProgress: Float = 0f,
  val isDragging: Boolean = false,
  val showCornerHint: Boolean = true,
  val isMagnifierActive: Boolean = false,
  val magnifierPosition: Offset = Offset(300f, 300f),
  val isHudVisible: Boolean = true,
  val isLoading: Boolean = false,
  val savedSpreadIndex: Int? = null,
  val resumeToastMessage: String? = null,
  val isTranslationActive: Boolean = false,
  val isTranslating: Boolean = false,
  val translationError: String? = null,
  val translatedBlocks: List<com.example.translation.TranslatedBlock> = emptyList(),
  val sourceLanguage: String = com.google.mlkit.nl.translate.TranslateLanguage.JAPANESE,
  val showDownloadDialog: Boolean = false,
  val downloadTargetLanguage: String? = null,
  val isDownloadingModel: Boolean = false
) {
  val hasSavedProgress: Boolean
    get() = savedSpreadIndex != null && savedSpreadIndex > 0

  val totalSpreads: Int
    get() {
      val totalPages = currentBook.pages.size
      if (totalPages <= 1) return 1
      val innerPages = (totalPages - 2).coerceAtLeast(0)
      val innerSpreads = ceil(innerPages / 2.0).toInt()
      return 1 + innerSpreads + 1 // Front Cover + Inners + Back Cover
    }

  fun getCurrentSpreadPages(): Pair<ComicPage?, ComicPage?> {
    val pages = currentBook.pages
    if (pages.isEmpty()) return Pair(null, null)
    val total = pages.size

    // Front Cover Spread
    if (currentSpreadIndex == 0) {
      return Pair(null, pages.first())
    }

    // Back Cover Spread
    if (currentSpreadIndex >= totalSpreads - 1) {
      return Pair(pages.last(), null)
    }

    val pairIndex = currentSpreadIndex - 1
    val pageAIndex = 1 + (pairIndex * 2)
    val pageBIndex = pageAIndex + 1

    val pageA = pages.getOrNull(pageAIndex)
    val pageB = pages.getOrNull(pageBIndex)

    return if (readingDirection == ReadingDirection.RTL) {
      // Manga mode: Right page is first (pageA), Left page is next (pageB)
      Pair(pageB, pageA)
    } else {
      // Western mode: Left page is first (pageA), Right page is next (pageB)
      Pair(pageA, pageB)
    }
  }
}

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(ReaderUiState())
  val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

  private val database = ComicDatabase.getInstance(application)
  private val repository = ReadingProgressRepository(database.readingProgressDao())

  private val translationManager = com.example.translation.TranslationManager()
  private var translationJob: Job? = null

  private var hudTimerJob: Job? = null
  private var resumeToastJob: Job? = null

  init {
    resetHudTimer()
    loadSavedProgressForBook(_uiState.value.currentBook, autoResume = true)
    
    viewModelScope.launch {
        _uiState.map { it.currentSinglePageIndex }
            .distinctUntilChanged()
            .collect {
                if (_uiState.value.isTranslationActive) {
                    triggerTranslation()
                }
            }
    }
  }

  fun spreadToPage(spreadIndex: Int, totalPages: Int, totalSpreads: Int): Int {
    if (totalPages <= 0) return 0
    if (spreadIndex <= 0) return 0
    if (spreadIndex >= totalSpreads - 1) return (totalPages - 1).coerceAtLeast(0)
    val target = 1 + (spreadIndex - 1) * 2
    return target.coerceIn(0, totalPages - 1)
  }

  fun pageToSpread(pageIndex: Int, totalPages: Int, totalSpreads: Int): Int {
    if (totalPages <= 0 || pageIndex <= 0) return 0
    if (pageIndex >= totalPages - 1) return (totalSpreads - 1).coerceAtLeast(0)
    return (1 + (pageIndex - 1) / 2).coerceIn(0, (totalSpreads - 1).coerceAtLeast(0))
  }

  fun loadComicBook(book: ComicBook) {
      _uiState.update { it.copy(currentBook = book) }
      loadSavedProgressForBook(book, autoResume = true)
  }

  private fun loadSavedProgressForBook(book: ComicBook, autoResume: Boolean = true) {
    viewModelScope.launch {
      val saved = repository.getProgressDirect(book.id)
      if (saved != null) {
        val maxSpread = (book.totalPages / 2) + 1
        val targetSpread = saved.spreadIndex.coerceIn(0, maxSpread)
        val savedDirection = runCatching { ReadingDirection.valueOf(saved.readingDirection) }.getOrNull()
          ?: book.defaultDirection

        val targetPage = if (saved.pageIndex in 0 until book.totalPages) saved.pageIndex
                         else spreadToPage(targetSpread, book.totalPages, (book.totalPages / 2) + 1)

        if (autoResume && (targetPage > 0 || targetSpread > 0)) {
          _uiState.update {
            it.copy(
              currentBook = book,
              currentSpreadIndex = targetSpread,
              currentSinglePageIndex = targetPage,
              readingDirection = savedDirection,
              savedSpreadIndex = targetSpread,
              showCornerHint = false,
              resumeToastMessage = "Melanjutkan bacaan: Halaman ${targetPage + 1} dari ${book.totalPages}",
            )
          }
          triggerToastDismiss()
        } else {
          _uiState.update {
            it.copy(
              currentBook = book,
              savedSpreadIndex = targetSpread,
              currentSinglePageIndex = targetPage,
              readingDirection = savedDirection,
            )
          }
        }
      } else {
        _uiState.update {
          it.copy(
            currentBook = book,
            savedSpreadIndex = null,
            resumeToastMessage = null,
          )
        }
      }
    }
  }

  private fun triggerToastDismiss() {
    resumeToastJob?.cancel()
    resumeToastJob = viewModelScope.launch {
      delay(4000)
      _uiState.update { it.copy(resumeToastMessage = null) }
    }
  }

  fun dismissResumeToast() {
    resumeToastJob?.cancel()
    _uiState.update { it.copy(resumeToastMessage = null) }
  }

  fun updatePageIndex(index: Int) {
    if (index == _uiState.value.currentSinglePageIndex) return
    val totalSpreads = _uiState.value.totalSpreads
    val spread = pageToSpread(index, _uiState.value.currentBook.totalPages, totalSpreads)
    _uiState.update { it.copy(currentSinglePageIndex = index, currentSpreadIndex = spread) }
    persistProgress()
  }

  private fun persistProgress() {
    val state = _uiState.value
    val book = state.currentBook
    if (book.id.isBlank() || book.pages.isEmpty()) return
    viewModelScope.launch {
      repository.saveProgress(
        comicId = book.id,
        comicTitle = book.title,
        coverUrl = book.coverUrl,
        chapterUrl = book.chapterUrl,
        chapterTitle = book.chapterTitle,
        pageIndex = state.currentSinglePageIndex,
        spreadIndex = state.currentSpreadIndex,
        totalPages = book.totalPages,
        readingDirection = state.readingDirection,
      )
      _uiState.update { it.copy(savedSpreadIndex = state.currentSpreadIndex) }
    }
  }

  fun resumeToSavedPosition() {
    val target = _uiState.value.savedSpreadIndex ?: return
    goToSpread(target)
  }

  fun restartToBeginning() {
    goToSpread(0)
    dismissResumeToast()
  }

  fun resetHudTimer() {
    hudTimerJob?.cancel()
    hudTimerJob = viewModelScope.launch {
      delay(4000)
      _uiState.update { it.copy(isHudVisible = false) }
    }
  }

  fun toggleHud() {
    _uiState.update { current ->
      val newVisible = !current.isHudVisible
      if (newVisible) resetHudTimer() else hudTimerJob?.cancel()
      current.copy(isHudVisible = newVisible)
    }
  }

  fun setHudVisible(visible: Boolean) {
    hudTimerJob?.cancel()
    _uiState.update { it.copy(isHudVisible = visible) }
    if (visible) resetHudTimer()
  }

  fun togglePortraitMode() {
    _uiState.update {
      it.copy(
        portraitMode = if (it.portraitMode == com.example.model.PortraitReadingMode.SINGLE_PAGE_SLIDE) {
          com.example.model.PortraitReadingMode.LONG_STRIP
        } else {
          com.example.model.PortraitReadingMode.SINGLE_PAGE_SLIDE
        }
      )
    }
    resetHudTimer()
  }

  fun setPortraitMode(mode: com.example.model.PortraitReadingMode) {
    _uiState.update { it.copy(portraitMode = mode) }
    resetHudTimer()
  }

  fun nextSinglePage() {
    val state = _uiState.value
    if (state.currentSinglePageIndex < state.currentBook.totalPages - 1) {
      val newPage = state.currentSinglePageIndex + 1
      val newSpread = pageToSpread(newPage, state.currentBook.totalPages, state.totalSpreads)
      
      _uiState.update {
        it.copy(
          currentSinglePageIndex = newPage,
          currentSpreadIndex = newSpread,
          showCornerHint = false,
          curlProgress = 0f,
        )
      }
      persistProgress()
      resetHudTimer()
    }
  }

  fun previousSinglePage() {
    val state = _uiState.value
    if (state.currentSinglePageIndex > 0) {
      val newPage = state.currentSinglePageIndex - 1
      val newSpread = pageToSpread(newPage, state.currentBook.totalPages, state.totalSpreads)
      
      _uiState.update {
        it.copy(
          currentSinglePageIndex = newPage,
          currentSpreadIndex = newSpread,
          showCornerHint = false,
          curlProgress = 0f,
        )
      }
      persistProgress()
      resetHudTimer()
    }
  }

  fun goToSinglePage(index: Int) {
    val state = _uiState.value
    val clamped = index.coerceIn(0, (state.currentBook.totalPages - 1).coerceAtLeast(0))
    if (clamped != state.currentSinglePageIndex) {
      val newSpread = pageToSpread(clamped, state.currentBook.totalPages, state.totalSpreads)
      
      _uiState.update {
        it.copy(
          currentSinglePageIndex = clamped,
          currentSpreadIndex = newSpread,
          showCornerHint = false,
          curlProgress = 0f,
        )
      }
      persistProgress()
      resetHudTimer()
    }
  }

  private var scrollPersistJob: Job? = null

  fun onScrollToPage(index: Int) {
    val state = _uiState.value
    val clamped = index.coerceIn(0, (state.currentBook.totalPages - 1).coerceAtLeast(0))
    if (clamped != state.currentSinglePageIndex) {
      val newSpread = pageToSpread(clamped, state.currentBook.totalPages, state.totalSpreads)
      _uiState.update {
        it.copy(
          currentSinglePageIndex = clamped,
          currentSpreadIndex = newSpread,
        )
      }
      // Debounce saving progress to avoid database writes during fast scrolling
      scrollPersistJob?.cancel()
      scrollPersistJob = viewModelScope.launch {
        delay(600)
        persistProgress()
      }
    }
  }

  fun nextPage() {
    val state = _uiState.value
    if (state.currentSpreadIndex < state.totalSpreads - 1) {
      val newSpread = state.currentSpreadIndex + 1
      val newPage = spreadToPage(newSpread, state.currentBook.totalPages, state.totalSpreads)
      
      _uiState.update {
        it.copy(
          currentSpreadIndex = newSpread,
          currentSinglePageIndex = newPage,
          showCornerHint = false,
          curlProgress = 0f,
        )
      }
      persistProgress()
      resetHudTimer()
    }
  }

  fun previousPage() {
    val state = _uiState.value
    if (state.currentSpreadIndex > 0) {
      val newSpread = state.currentSpreadIndex - 1
      val newPage = spreadToPage(newSpread, state.currentBook.totalPages, state.totalSpreads)
      
      _uiState.update {
        it.copy(
          currentSpreadIndex = newSpread,
          currentSinglePageIndex = newPage,
          showCornerHint = false,
          curlProgress = 0f,
        )
      }
      persistProgress()
      resetHudTimer()
    }
  }

  fun goToSpread(index: Int) {
    val state = _uiState.value
    val clamped = index.coerceIn(0, state.totalSpreads - 1)
    if (clamped != state.currentSpreadIndex) {
      val newPage = spreadToPage(clamped, state.currentBook.totalPages, state.totalSpreads)
      
      _uiState.update {
        it.copy(
          currentSpreadIndex = clamped,
          currentSinglePageIndex = newPage,
          showCornerHint = false,
          curlProgress = 0f,
        )
      }
      persistProgress()
      resetHudTimer()
    }
  }

  fun onDragStart() {
    _uiState.update { 
        it.copy(
            isDragging = true, 
            showCornerHint = false, 
            isHudVisible = true
        ) 
    }
    resetHudTimer()
  }

  fun onDragUpdate(deltaX: Float, screenWidth: Float) {
    if (!_uiState.value.isDragging) return
    val sensitivity = 1.8f / screenWidth
    val state = _uiState.value
    val deltaProgress = -deltaX * sensitivity

    _uiState.update {
      it.copy(curlProgress = (it.curlProgress + deltaProgress).coerceIn(-1f, 1f))
    }
  }

  fun onDragEnd() {
    val state = _uiState.value
    if (state.curlProgress > 0.25f) {
      nextPage()
    } else if (state.curlProgress < -0.25f) {
      previousPage()
    } else {
      _uiState.update { it.copy(curlProgress = 0f, isDragging = false) }
    }
    _uiState.update { it.copy(isDragging = false) }
    resetHudTimer()
  }

  fun setSourceLanguage(lang: String) {
    _uiState.update { it.copy(sourceLanguage = lang) }
    // If translation is already active, we should trigger it again for the new language
    if (_uiState.value.isTranslationActive) {
        checkAndTriggerTranslation()
    }
  }

  fun toggleTranslate() {
    val isActive = !_uiState.value.isTranslationActive
    
    if (isActive) {
        checkAndTriggerTranslation()
    } else {
        _uiState.update { it.copy(
            isTranslationActive = false,
            translatedBlocks = emptyList(), 
            isTranslating = false, 
            translationError = null,
            showDownloadDialog = false,
            isDownloadingModel = false
        ) }
        translationJob?.cancel()
    }
  }

  private fun checkAndTriggerTranslation() {
    val lang = _uiState.value.sourceLanguage
    viewModelScope.launch {
        val isDownloaded = translationManager.mlkitTranslator.isModelDownloaded(lang)
        if (isDownloaded) {
            _uiState.update { it.copy(isTranslationActive = true) }
            triggerTranslation()
        } else {
            _uiState.update { it.copy(
                showDownloadDialog = true, 
                downloadTargetLanguage = lang
            ) }
        }
    }
  }

  fun confirmDownloadModel() {
    val lang = _uiState.value.downloadTargetLanguage ?: return
    _uiState.update { it.copy(showDownloadDialog = false, isDownloadingModel = true) }
    
    viewModelScope.launch(Dispatchers.IO) {
        try {
            translationManager.mlkitTranslator.downloadModel(lang)
            _uiState.update { it.copy(isDownloadingModel = false, isTranslationActive = true) }
            triggerTranslation()
        } catch (e: Exception) {
            _uiState.update { it.copy(
                isDownloadingModel = false, 
                translationError = "Gagal mendownload model bahasa."
            ) }
        }
    }
  }

  fun cancelDownloadModel() {
    _uiState.update { it.copy(showDownloadDialog = false, downloadTargetLanguage = null) }
  }

  private fun triggerTranslation() {
    translationJob?.cancel()
    translationJob = viewModelScope.launch(Dispatchers.IO) {
        _uiState.update { it.copy(isTranslating = true, translatedBlocks = emptyList(), translationError = null) }
        
        // Let UI show the translating state
        delay(300)
        
        val state = _uiState.value
        val page = state.currentBook.pages.getOrNull(state.currentSinglePageIndex)
        
        if (page == null) {
            _uiState.update { it.copy(isTranslating = false, translatedBlocks = emptyList()) }
            return@launch
        }
        
        try {
            if (page.imageBytes != null) {
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(page.imageBytes, 0, page.imageBytes.size)
                if (bitmap != null) {
                    val blocks = translationManager.processImage(bitmap, state.sourceLanguage)
                    _uiState.update { it.copy(translatedBlocks = blocks, isTranslating = false) }
                } else {
                    _uiState.update { it.copy(isTranslating = false, translatedBlocks = emptyList()) }
                }
            } else {
                _uiState.update { it.copy(isTranslating = false, translatedBlocks = emptyList()) }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Ignore cancellation
        } catch (e: Throwable) {
            _uiState.update { it.copy(isTranslating = false, translatedBlocks = emptyList(), translationError = e.message ?: "Unknown error") }
        }
    }
  }

  fun setTranslating(translating: Boolean) {
    _uiState.update { it.copy(isTranslating = translating) }
  }

  fun setTranslatedBlocks(blocks: List<com.example.translation.TranslatedBlock>) {
    _uiState.update { it.copy(translatedBlocks = blocks, isTranslating = false) }
  }

  fun toggleReadingDirection() {
    _uiState.update {
      it.copy(
        readingDirection = if (it.readingDirection == ReadingDirection.RTL) ReadingDirection.LTR else ReadingDirection.RTL
      )
    }
    persistProgress()
    resetHudTimer()
  }

  fun setPaperTexture(texture: PaperTexture) {
    _uiState.update { it.copy(paperTexture = texture) }
    resetHudTimer()
  }

  fun toggleMagnifier() {
    val willBeActive = !_uiState.value.isMagnifierActive
    _uiState.update { it.copy(isMagnifierActive = willBeActive) }
    if (willBeActive) {
      hudTimerJob?.cancel()
    } else {
      resetHudTimer()
    }
  }

  fun exitMagnifier() {
    _uiState.update { it.copy(isMagnifierActive = false) }
    resetHudTimer()
  }

  fun updateMagnifierPosition(offset: Offset) {
    _uiState.update { it.copy(magnifierPosition = offset) }
  }



  fun loadFromArchiveUri(uri: Uri) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      try {
        val filename = runCatching {
          var name: String? = null
          getApplication<Application>().contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
          )?.use { cursor ->
            if (cursor.moveToFirst()) {
              val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
              if (idx != -1) name = cursor.getString(idx)
            }
          }
          name
        }.getOrNull() ?: uri.lastPathSegment ?: "Comic.cbz"

        val book = ComicArchiveParser.parseArchiveFromUri(getApplication(), uri, filename)
        if (book.pages.isNotEmpty()) {
          _uiState.update {
            it.copy(
              currentBook = book,
              currentSpreadIndex = 0,
              currentSinglePageIndex = 0,
              readingDirection = book.defaultDirection,
              showCornerHint = true,
              isLoading = false,
            )
          }
          loadSavedProgressForBook(book, autoResume = true)
        } else {
          _uiState.update { it.copy(isLoading = false) }
        }
      } catch (_: Exception) {
        _uiState.update { it.copy(isLoading = false) }
      }
      resetHudTimer()
    }
  }

  fun loadFromImageUris(uris: List<Uri>) {
    if (uris.isEmpty()) return
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      try {
        val book = ComicArchiveParser.createFromUris(getApplication(), uris)
        if (book.pages.isNotEmpty()) {
          _uiState.update {
            it.copy(
              currentBook = book,
              currentSpreadIndex = 0,
              currentSinglePageIndex = 0,
              readingDirection = book.defaultDirection,
              showCornerHint = true,
              isLoading = false,
            )
          }
          loadSavedProgressForBook(book, autoResume = true)
        } else {
          _uiState.update { it.copy(isLoading = false) }
        }
      } catch (_: Exception) {
        _uiState.update { it.copy(isLoading = false) }
      }
      resetHudTimer()
    }
  }
}
