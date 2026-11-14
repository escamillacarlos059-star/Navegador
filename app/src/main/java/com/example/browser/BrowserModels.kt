package com.example.browser

import android.net.Uri
import android.webkit.SslErrorHandler
import com.example.data.BookmarkEntity
import com.example.data.BrowserSettings
import com.example.data.HistoryEntity
import com.example.nativecore.NativeEngineDiagnostics
import com.example.nativecore.PeBinaryInspection
import com.example.nativecore.SecurityClassification
import java.util.UUID

const val HOME_URL = "vectra://home"

enum class SearchEngineOption(val id: Int, val displayName: String, val domainHint: String) {
    GOOGLE(0, "Google", "google.com"),
    DUCKDUCKGO(1, "DuckDuckGo", "duckduckgo.com"),
    BRAVE(2, "Brave Search", "search.brave.com"),
    WIKIPEDIA(3, "Wikipedia ES", "es.wikipedia.org");

    companion object {
        fun fromId(id: Int): SearchEngineOption = entries.firstOrNull { it.id == id } ?: GOOGLE
    }
}

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = HOME_URL,
    val title: String = "Inicio",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isIncognito: Boolean = false,
    val isDesktopMode: Boolean = false,
    val blockedCountOnPage: Int = 0,
    val security: SecurityClassification = SecurityClassification.INTERNAL
) {
    val isHomePage: Boolean get() = url == HOME_URL || url.isBlank()
}

data class SslErrorPrompt(
    val tabId: String,
    val url: String,
    val errorSummary: String,
    val handler: SslErrorHandler
)

data class ExeInspectionPrompt(
    val uri: Uri,
    val fileName: String,
    val fileSizeBytes: Long,
    val inspection: PeBinaryInspection
)

sealed interface ActiveSheet {
    data object None : ActiveSheet
    data object Tabs : ActiveSheet
    data object Library : ActiveSheet // Bookmarks & History
    data object Menu : ActiveSheet
    data object EngineInspector : ActiveSheet
    data object SecurityInfo : ActiveSheet
}

data class BrowserUiState(
    val tabs: List<BrowserTab> = listOf(BrowserTab()),
    val activeTabId: String = tabs.first().id,
    val omniboxText: String = "",
    val isOmniboxFocused: Boolean = false,
    val findInPageVisible: Boolean = false,
    val findQuery: String = "",
    val findMatchesCurrent: Int = 0,
    val findMatchesTotal: Int = 0,
    val activeSheet: ActiveSheet = ActiveSheet.None,
    val libraryInitialTab: Int = 0, // 0 = Bookmarks, 1 = History
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val history: List<HistoryEntity> = emptyList(),
    val settings: BrowserSettings = BrowserSettings(),
    val diagnostics: NativeEngineDiagnostics? = null,
    val customBlockedDomains: List<String> = emptyList(),
    val sslErrorPrompt: SslErrorPrompt? = null,
    val exeInspectionPrompt: ExeInspectionPrompt? = null,
    val snackbarMessage: String? = null
) {
    val activeTab: BrowserTab
        get() = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()
}
