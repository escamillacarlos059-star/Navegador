package com.example.data

import com.example.nativecore.NativeEngineDiagnostics
import com.example.nativecore.NormalizedInputResult
import com.example.nativecore.SecurityClassification
import com.example.nativecore.VectraNativeEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class BrowserRepository(
    private val dao: BrowserDao,
    private val preferences: BrowserPreferences
) {
    val bookmarks: Flow<List<BookmarkEntity>> = dao.observeBookmarks()
    val history: Flow<List<HistoryEntity>> = dao.observeHistory()
    val settings: Flow<BrowserSettings> = preferences.settingsFlow

    suspend fun initializeDefaultsIfEmpty() {
        val currentSettings = preferences.settingsFlow.first()
        currentSettings.customBlockedDomains.forEach { domain ->
            VectraNativeEngine.addCustomBlockedDomain(domain)
        }

        if (dao.countBookmarks() == 0) {
            dao.insertBookmarks(
                listOf(
                    BookmarkEntity(
                        title = "Wikipedia",
                        url = "https://es.wikipedia.org/",
                        accentColorIndex = 0
                    ),
                    BookmarkEntity(
                        title = "DuckDuckGo",
                        url = "https://duckduckgo.com/",
                        accentColorIndex = 1
                    ),
                    BookmarkEntity(
                        title = "GitHub",
                        url = "https://github.com/",
                        accentColorIndex = 2
                    ),
                    BookmarkEntity(
                        title = "MDN Web Docs",
                        url = "https://developer.mozilla.org/es/",
                        accentColorIndex = 3
                    ),
                    BookmarkEntity(
                        title = "isocpp.org",
                        url = "https://isocpp.org/",
                        accentColorIndex = 4
                    ),
                    BookmarkEntity(
                        title = "BBC Mundo",
                        url = "https://www.bbc.com/mundo",
                        accentColorIndex = 5
                    )
                )
            )
        }
    }

    suspend fun toggleBookmark(title: String, url: String, isCurrentlyBookmarked: Boolean) {
        if (isCurrentlyBookmarked) {
            dao.deleteBookmarkByUrl(url)
        } else {
            val cleanTitle = title.ifBlank { VectraNativeEngine.extractHost(url) }
            val colorSeed = kotlin.math.abs(url.hashCode()) % 6
            dao.insertBookmark(
                BookmarkEntity(
                    title = cleanTitle,
                    url = url,
                    accentColorIndex = colorSeed
                )
            )
        }
    }

    suspend fun deleteBookmark(id: Long) = dao.deleteBookmarkById(id)

    suspend fun recordHistory(title: String, url: String) {
        if (url.isBlank() || url.startsWith("vectra://")) return
        val cleanTitle = title.ifBlank { VectraNativeEngine.extractHost(url) }
        dao.insertHistory(HistoryEntity(title = cleanTitle, url = url))
    }

    suspend fun deleteHistoryItem(id: Long) = dao.deleteHistoryById(id)

    suspend fun clearHistory() = dao.clearHistory()

    suspend fun loadSavedTabs(): List<SavedTabEntity> = dao.getSavedTabsOnce()

    suspend fun persistNormalTabs(tabs: List<SavedTabEntity>) {
        dao.clearSavedTabs()
        if (tabs.isNotEmpty()) {
            dao.upsertSavedTabs(tabs)
        }
    }

    suspend fun setAdBlockEnabled(enabled: Boolean) = preferences.setAdBlockEnabled(enabled)
    suspend fun setStripTrackingParams(enabled: Boolean) = preferences.setStripTrackingParams(enabled)
    suspend fun setJavascriptEnabled(enabled: Boolean) = preferences.setJavascriptEnabled(enabled)
    suspend fun setSearchEngineId(engineId: Int) = preferences.setSearchEngineId(engineId)

    suspend fun addCustomBlockedDomain(rawDomain: String): Boolean {
        val added = VectraNativeEngine.addCustomBlockedDomain(rawDomain)
        if (added) {
            val host = VectraNativeEngine.extractHost(rawDomain)
            preferences.addCustomBlockedDomain(host)
        }
        return added
    }

    suspend fun removeCustomBlockedDomain(domain: String) {
        VectraNativeEngine.removeCustomBlockedDomain(domain)
        preferences.removeCustomBlockedDomain(domain)
    }

    fun normalizeInput(rawInput: String, searchEngineId: Int, stripParams: Boolean): NormalizedInputResult =
        VectraNativeEngine.normalizeInput(rawInput, searchEngineId, stripParams)

    fun analyzeSecurity(url: String): SecurityClassification =
        VectraNativeEngine.analyzeSecurity(url)

    fun getNativeDiagnostics(): NativeEngineDiagnostics =
        VectraNativeEngine.getDiagnostics()

    fun getCustomBlockedDomains(): List<String> =
        VectraNativeEngine.getCustomBlockedDomains()

    fun resetNativeStats() = VectraNativeEngine.resetStats()
}
