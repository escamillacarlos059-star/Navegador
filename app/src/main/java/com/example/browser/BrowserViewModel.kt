package com.example.browser

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.view.View
import android.webkit.ValueCallback
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BrowserPreferences
import com.example.data.BrowserRepository
import com.example.data.SavedTabEntity
import com.example.data.VectraDatabase
import com.example.nativecore.SecurityClassification
import com.example.nativecore.VectraNativeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BrowserRepository
    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private val _fullscreenView = MutableStateFlow<View?>(null)
    val fullscreenView: StateFlow<View?> = _fullscreenView.asStateFlow()

    var fileChooserCallback: ((ValueCallback<Array<Uri>>, Array<String>) -> Unit)? = null
    var openExePickerCallback: (() -> Unit)? = null

    val webViewManager: TabWebViewManager

    init {
        val db = VectraDatabase.getInstance(application)
        val prefs = BrowserPreferences(application)
        repository = BrowserRepository(db.browserDao(), prefs)

        webViewManager = TabWebViewManager(
            context = application,
            onPageStarted = { tabId, url ->
                val sec = repository.analyzeSecurity(url)
                updateTab(tabId) {
                    it.copy(
                        url = url,
                        isLoading = true,
                        progress = 10,
                        blockedCountOnPage = 0,
                        security = sec
                    )
                }
                syncOmniboxIfActive(tabId, url)
                refreshDiagnostics()
            },
            onPageFinished = { tabId, url, title, canGoBack, canGoForward ->
                val sec = repository.analyzeSecurity(url)
                var isIncognito = false
                updateTab(tabId) {
                    isIncognito = it.isIncognito
                    it.copy(
                        url = url,
                        title = title,
                        isLoading = false,
                        progress = 100,
                        canGoBack = canGoBack,
                        canGoForward = canGoForward,
                        security = sec
                    )
                }
                syncOmniboxIfActive(tabId, url)
                refreshDiagnostics()
                if (!isIncognito && !url.startsWith("vectra://")) {
                    viewModelScope.launch {
                        repository.recordHistory(title, url)
                        persistTabs()
                    }
                }
            },
            onProgressChanged = { tabId, progress ->
                updateTab(tabId) {
                    it.copy(
                        progress = progress,
                        isLoading = progress in 1..99
                    )
                }
            },
            onTitleChanged = { tabId, title ->
                updateTab(tabId) { it.copy(title = title) }
            },
            onTrackerBlocked = { tabId ->
                updateTab(tabId) { it.copy(blockedCountOnPage = it.blockedCountOnPage + 1) }
            },
            onOpenNewTabUrl = { url, isIncognito ->
                openNewTab(initialUrl = url, isIncognito = isIncognito)
            },
            onSslError = { prompt ->
                _uiState.update { it.copy(sslErrorPrompt = prompt) }
            },
            onFindResult = { tabId, current, total ->
                if (_uiState.value.activeTabId == tabId) {
                    _uiState.update {
                        it.copy(findMatchesCurrent = current, findMatchesTotal = total)
                    }
                }
            },
            onFileChooserRequested = { callback, acceptTypes ->
                val handler = fileChooserCallback
                if (handler != null) {
                    handler(callback, acceptTypes)
                } else {
                    callback.onReceiveValue(null)
                }
            },
            onFullscreenChanged = { view ->
                _fullscreenView.value = view
            },
            onNotifyMessage = { msg ->
                showMessage(msg)
            }
        )

        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            repository.initializeDefaultsIfEmpty()
            val savedTabs = repository.loadSavedTabs()
            if (savedTabs.isNotEmpty()) {
                val restored = savedTabs.map { saved ->
                    BrowserTab(
                        id = saved.id,
                        url = saved.url,
                        title = saved.title.ifBlank { "Vectra" },
                        security = repository.analyzeSecurity(saved.url)
                    )
                }
                _uiState.update { state ->
                    val active = restored.first()
                    state.copy(
                        tabs = restored,
                        activeTabId = active.id,
                        omniboxText = if (active.isHomePage) "" else active.url
                    )
                }
                restored.firstOrNull()?.let { firstTab ->
                    if (!firstTab.isHomePage) {
                        webViewManager.loadUrl(firstTab, firstTab.url)
                    }
                }
            }
            refreshDiagnostics()
        }

        viewModelScope.launch {
            repository.bookmarks.collect { list ->
                _uiState.update { it.copy(bookmarks = list) }
            }
        }

        viewModelScope.launch {
            repository.history.collect { list ->
                _uiState.update { it.copy(history = list) }
            }
        }

        viewModelScope.launch {
            repository.settings.collect { settings ->
                webViewManager.adBlockEnabled = settings.adBlockEnabled
                webViewManager.javascriptEnabled = settings.javascriptEnabled
                _uiState.update {
                    it.copy(
                        settings = settings,
                        customBlockedDomains = repository.getCustomBlockedDomains()
                    )
                }
            }
        }
    }

    fun handleIncomingSystemIntent(url: String?) {
        if (url.isNullOrBlank()) return
        if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
            openNewTab(initialUrl = url, isIncognito = false)
        } else {
            submitOmniboxInput(url)
        }
    }

    fun onOmniboxChanged(text: String) {
        _uiState.update { it.copy(omniboxText = text) }
    }

    fun onOmniboxFocusChanged(focused: Boolean) {
        _uiState.update { state ->
            val tab = state.activeTab
            val nextText = if (!focused && !tab.isHomePage) tab.url else state.omniboxText
            state.copy(isOmniboxFocused = focused, omniboxText = nextText)
        }
    }

    fun submitOmniboxInput(rawInput: String = _uiState.value.omniboxText) {
        val currentSettings = _uiState.value.settings
        val normalized = repository.normalizeInput(
            rawInput = rawInput,
            searchEngineId = currentSettings.searchEngineId,
            stripParams = currentSettings.stripTrackingParams
        )
        val tab = _uiState.value.activeTab
        val sec = repository.analyzeSecurity(normalized.finalUrl)

        updateTab(tab.id) {
            it.copy(
                url = normalized.finalUrl,
                title = if (normalized.finalUrl == HOME_URL) "Inicio" else normalized.displayHost,
                security = sec,
                blockedCountOnPage = 0
            )
        }

        _uiState.update {
            it.copy(
                omniboxText = if (normalized.finalUrl == HOME_URL) "" else normalized.finalUrl,
                isOmniboxFocused = false,
                activeSheet = ActiveSheet.None
            )
        }

        if (normalized.strippedParamsCount > 0) {
            showMessage("C++20 limpió ${normalized.strippedParamsCount} parámetro(s) de rastreo")
        }

        if (normalized.finalUrl != HOME_URL) {
            webViewManager.loadUrl(tab, normalized.finalUrl)
        }
        refreshDiagnostics()
        viewModelScope.launch { persistTabs() }
    }

    fun navigateToUrl(url: String) {
        submitOmniboxInput(url)
    }

    fun goHome() {
        val tab = _uiState.value.activeTab
        updateTab(tab.id) {
            it.copy(
                url = HOME_URL,
                title = "Inicio",
                isLoading = false,
                progress = 0,
                security = SecurityClassification.INTERNAL
            )
        }
        _uiState.update {
            it.copy(
                omniboxText = "",
                isOmniboxFocused = false,
                findInPageVisible = false,
                activeSheet = ActiveSheet.None
            )
        }
        viewModelScope.launch { persistTabs() }
    }

    fun openNewTab(initialUrl: String = HOME_URL, isIncognito: Boolean = false) {
        val sec = repository.analyzeSecurity(initialUrl)
        val newTab = BrowserTab(
            url = initialUrl,
            title = if (initialUrl == HOME_URL) {
                if (isIncognito) "Pestaña privada" else "Inicio"
            } else {
                initialUrl
            },
            isIncognito = isIncognito,
            security = sec
        )
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs + newTab,
                activeTabId = newTab.id,
                omniboxText = if (newTab.isHomePage) "" else newTab.url,
                isOmniboxFocused = false,
                findInPageVisible = false,
                activeSheet = ActiveSheet.None
            )
        }
        if (!newTab.isHomePage) {
            webViewManager.loadUrl(newTab, initialUrl)
        }
        viewModelScope.launch { persistTabs() }
    }

    fun selectTab(tabId: String) {
        val target = _uiState.value.tabs.firstOrNull { it.id == tabId } ?: return
        _uiState.update {
            it.copy(
                activeTabId = target.id,
                omniboxText = if (target.isHomePage) "" else target.url,
                isOmniboxFocused = false,
                findInPageVisible = false,
                activeSheet = ActiveSheet.None
            )
        }
        if (!target.isHomePage && webViewManager.getExistingWebView(target.id) == null) {
            webViewManager.loadUrl(target, target.url)
        }
    }

    fun closeTab(tabId: String) {
        val currentTabs = _uiState.value.tabs
        val closing = currentTabs.firstOrNull { it.id == tabId } ?: return
        val remaining = currentTabs.filterNot { it.id == tabId }

        if (remaining.isEmpty()) {
            val fresh = BrowserTab(isIncognito = false)
            webViewManager.destroyTab(tabId, closing.isIncognito, anyIncognitoRemaining = false)
            _uiState.update {
                it.copy(
                    tabs = listOf(fresh),
                    activeTabId = fresh.id,
                    omniboxText = "",
                    activeSheet = ActiveSheet.None
                )
            }
        } else {
            val anyIncognito = remaining.any { it.isIncognito }
            webViewManager.destroyTab(tabId, closing.isIncognito, anyIncognito)
            val nextActive = if (_uiState.value.activeTabId == tabId) remaining.last() else _uiState.value.activeTab
            _uiState.update {
                it.copy(
                    tabs = remaining,
                    activeTabId = nextActive.id,
                    omniboxText = if (nextActive.isHomePage) "" else nextActive.url
                )
            }
            if (!nextActive.isHomePage && webViewManager.getExistingWebView(nextActive.id) == null) {
                webViewManager.loadUrl(nextActive, nextActive.url)
            }
        }
        viewModelScope.launch { persistTabs() }
    }

    fun navigateBack(): Boolean {
        val state = _uiState.value
        if (state.activeSheet != ActiveSheet.None) {
            setActiveSheet(ActiveSheet.None)
            return true
        }
        if (state.findInPageVisible) {
            closeFindInPage()
            return true
        }
        val tab = state.activeTab
        if (!tab.isHomePage && webViewManager.goBack(tab.id)) {
            return true
        }
        if (!tab.isHomePage) {
            goHome()
            return true
        }
        return false
    }

    fun navigateForward() {
        webViewManager.goForward(_uiState.value.activeTabId)
    }

    fun reloadOrStop() {
        val tab = _uiState.value.activeTab
        if (tab.isHomePage) return
        if (tab.isLoading) {
            webViewManager.stopLoading(tab.id)
        } else {
            webViewManager.reload(tab.id)
        }
    }

    fun toggleDesktopMode() {
        val tab = _uiState.value.activeTab
        val next = !tab.isDesktopMode
        updateTab(tab.id) { it.copy(isDesktopMode = next) }
        webViewManager.setDesktopMode(tab.copy(isDesktopMode = next), next)
        _uiState.update { it.copy(activeSheet = ActiveSheet.None) }
        showMessage(if (next) "Modo escritorio activado" else "Modo móvil activado")
    }

    fun toggleCurrentBookmark() {
        val tab = _uiState.value.activeTab
        if (tab.isHomePage) return
        val already = _uiState.value.bookmarks.any { it.url == tab.url }
        viewModelScope.launch {
            repository.toggleBookmark(tab.title, tab.url, already)
            showMessage(if (already) "Marcador eliminado" else "Guardado en marcadores")
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch { repository.deleteBookmark(id) }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch { repository.deleteHistoryItem(id) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            showMessage("Historial borrado")
        }
    }

    fun setActiveSheet(sheet: ActiveSheet, libraryTab: Int = 0) {
        if (sheet == ActiveSheet.EngineInspector) {
            refreshDiagnostics()
        }
        _uiState.update {
            it.copy(
                activeSheet = sheet,
                libraryInitialTab = libraryTab
            )
        }
    }

    fun openFindInPage() {
        _uiState.update {
            it.copy(
                activeSheet = ActiveSheet.None,
                findInPageVisible = true,
                findQuery = "",
                findMatchesCurrent = 0,
                findMatchesTotal = 0
            )
        }
    }

    fun updateFindQuery(query: String) {
        _uiState.update { it.copy(findQuery = query) }
        webViewManager.findAllAsync(_uiState.value.activeTabId, query)
    }

    fun findNextMatch(forward: Boolean) {
        webViewManager.findNext(_uiState.value.activeTabId, forward)
    }

    fun closeFindInPage() {
        webViewManager.clearFindMatches(_uiState.value.activeTabId)
        _uiState.update {
            it.copy(
                findInPageVisible = false,
                findQuery = "",
                findMatchesCurrent = 0,
                findMatchesTotal = 0
            )
        }
    }

    fun setAdBlockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAdBlockEnabled(enabled)
            refreshDiagnostics()
        }
    }

    fun setStripTrackingParams(enabled: Boolean) {
        viewModelScope.launch {
            repository.setStripTrackingParams(enabled)
            refreshDiagnostics()
        }
    }

    fun setJavascriptEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setJavascriptEnabled(enabled)
        }
    }

    fun setSearchEngine(option: SearchEngineOption) {
        viewModelScope.launch {
            repository.setSearchEngineId(option.id)
        }
    }

    fun addCustomBlockedDomain(domain: String) {
        viewModelScope.launch {
            val added = repository.addCustomBlockedDomain(domain)
            if (added) {
                _uiState.update { it.copy(customBlockedDomains = repository.getCustomBlockedDomains()) }
                refreshDiagnostics()
                showMessage("Dominio añadido al escudo C++20")
            } else {
                showMessage("Ingresa un dominio válido (ej. ads.ejemplo.com)")
            }
        }
    }

    fun removeCustomBlockedDomain(domain: String) {
        viewModelScope.launch {
            repository.removeCustomBlockedDomain(domain)
            _uiState.update { it.copy(customBlockedDomains = repository.getCustomBlockedDomains()) }
            refreshDiagnostics()
        }
    }

    fun resetNativeStats() {
        repository.resetNativeStats()
        refreshDiagnostics()
        showMessage("Contadores nativos reiniciados")
    }

    fun resolveSslError(proceed: Boolean) {
        val prompt = _uiState.value.sslErrorPrompt ?: return
        if (proceed) {
            prompt.handler.proceed()
        } else {
            prompt.handler.cancel()
        }
        _uiState.update { it.copy(sslErrorPrompt = null) }
    }

    fun openInExternalSystemBrowser(context: Context) {
        val tab = _uiState.value.activeTab
        val targetUrl = if (tab.isHomePage) "https://www.google.com" else tab.url
        val baseIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val pm = context.packageManager
        val resolvedList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                baseIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(baseIntent, PackageManager.MATCH_ALL)
        }

        val candidates = resolvedList
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedWith(
                compareByDescending<android.content.pm.ResolveInfo> { info ->
                    val pkg = info.activityInfo.packageName
                    pkg == "com.android.chrome" || pkg == "com.android.browser"
                }.thenByDescending { info ->
                    (info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                }
            )

        if (candidates.isEmpty()) {
            showMessage("No se encontró otro navegador del sistema instalado")
            _uiState.update { it.copy(activeSheet = ActiveSheet.None) }
            return
        }

        val targetInfo = candidates.first()
        val targetPackage = targetInfo.activityInfo.packageName
        val appLabel = targetInfo.loadLabel(pm).toString()

        val launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            setPackage(targetPackage)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        try {
            context.startActivity(launchIntent)
            showMessage("Abriendo en $appLabel")
        } catch (_: Exception) {
            showMessage("No se pudo abrir el navegador del sistema")
        }
        _uiState.update { it.copy(activeSheet = ActiveSheet.None) }
    }

    fun shareCurrentUrl(context: Context) {
        val tab = _uiState.value.activeTab
        if (tab.isHomePage) return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, tab.title)
            putExtra(Intent.EXTRA_TEXT, tab.url)
        }
        val chooser = Intent.createChooser(shareIntent, "Compartir enlace").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        _uiState.update { it.copy(activeSheet = ActiveSheet.None) }
    }

    fun triggerExePicker() {
        _uiState.update { it.copy(activeSheet = ActiveSheet.None) }
        openExePickerCallback?.invoke()
    }

    fun inspectExeUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                var name = uri.lastPathSegment ?: "archivo.exe"
                var sizeBytes = 0L
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                        if (sizeIdx >= 0) sizeBytes = cursor.getLong(sizeIdx)
                    }
                }
                val headerBytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                    val buf = ByteArray(4096)
                    var total = 0
                    while (total < buf.size) {
                        val read = stream.read(buf, total, buf.size - total)
                        if (read <= 0) break
                        total += read
                    }
                    buf.copyOf(total)
                } ?: ByteArray(0)

                val inspection = VectraNativeEngine.inspectPeBytes(headerBytes)
                ExeInspectionPrompt(
                    uri = uri,
                    fileName = name,
                    fileSizeBytes = sizeBytes,
                    inspection = inspection
                )
            }
            _uiState.update { it.copy(exeInspectionPrompt = result) }
        }
    }

    fun openExeInExternalApp(context: Context, uri: Uri) {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.microsoft.portable-executable")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        val chooser = Intent.createChooser(viewIntent, "Abrir archivo .exe con…").apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            showMessage("No hay un entorno compatible (ej. Wine/Winlator) instalado en Android")
        }
    }

    fun dismissExeInspection() {
        _uiState.update { it.copy(exeInspectionPrompt = null) }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun refreshDiagnostics() {
        _uiState.update { it.copy(diagnostics = repository.getNativeDiagnostics()) }
    }

    private fun showMessage(msg: String) {
        _uiState.update { it.copy(snackbarMessage = msg) }
    }

    private fun updateTab(tabId: String, transform: (BrowserTab) -> BrowserTab) {
        _uiState.update { state ->
            state.copy(tabs = state.tabs.map { if (it.id == tabId) transform(it) else it })
        }
    }

    private fun syncOmniboxIfActive(tabId: String, url: String) {
        _uiState.update { state ->
            if (state.activeTabId == tabId && !state.isOmniboxFocused) {
                state.copy(omniboxText = if (url == HOME_URL) "" else url)
            } else {
                state
            }
        }
    }

    private suspend fun persistTabs() {
        val normalTabs = _uiState.value.tabs
            .filter { !it.isIncognito }
            .mapIndexed { index, tab ->
                SavedTabEntity(
                    id = tab.id,
                    url = tab.url,
                    title = tab.title,
                    position = index
                )
            }
        repository.persistNormalTabs(normalTabs)
    }

    override fun onCleared() {
        super.onCleared()
        webViewManager.destroyAll()
    }
}
