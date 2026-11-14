package com.example.ui

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.browser.ActiveSheet
import com.example.browser.BrowserViewModel
import com.example.browser.SearchEngineOption
import com.example.ui.theme.VectraTheme

@Composable
fun VectraBrowserApp(viewModel: BrowserViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fullscreenView by viewModel.fullscreenView.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val activeTab = state.activeTab
    val isBookmarked = remember(state.bookmarks, activeTab.url) {
        !activeTab.isHomePage && state.bookmarks.any { it.url == activeTab.url }
    }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    val shouldInterceptBack = state.activeSheet != ActiveSheet.None ||
        state.findInPageVisible ||
        !activeTab.isHomePage ||
        activeTab.canGoBack

    BackHandler(enabled = shouldInterceptBack) {
        viewModel.navigateBack()
    }

    VectraTheme(isIncognito = activeTab.isIncognito) {
        if (fullscreenView != null) {
            FullscreenVideoOverlay(view = fullscreenView!!)
            return@VectraTheme
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 840.dp

            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.imePadding()
                    )
                },
                bottomBar = {
                    if (!isExpandedScreen) {
                        BrowserBottomBar(
                            canGoBack = activeTab.canGoBack || !activeTab.isHomePage,
                            canGoForward = activeTab.canGoForward,
                            tabCount = state.tabs.size,
                            isIncognito = activeTab.isIncognito,
                            onBack = { viewModel.navigateBack() },
                            onForward = { viewModel.navigateForward() },
                            onHome = { viewModel.goHome() },
                            onOpenTabs = { viewModel.setActiveSheet(ActiveSheet.Tabs) },
                            onOpenMenu = { viewModel.setActiveSheet(ActiveSheet.Menu) }
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedScreen) {
                        BrowserSideRail(
                            canGoBack = activeTab.canGoBack || !activeTab.isHomePage,
                            canGoForward = activeTab.canGoForward,
                            tabCount = state.tabs.size,
                            onBack = { viewModel.navigateBack() },
                            onForward = { viewModel.navigateForward() },
                            onHome = { viewModel.goHome() },
                            onOpenTabs = { viewModel.setActiveSheet(ActiveSheet.Tabs) },
                            onOpenMenu = { viewModel.setActiveSheet(ActiveSheet.Menu) }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        BrowserOmniboxHeader(
                            activeTab = activeTab,
                            omniboxText = state.omniboxText,
                            isOmniboxFocused = state.isOmniboxFocused,
                            isBookmarked = isBookmarked,
                            adBlockEnabled = state.settings.adBlockEnabled,
                            findInPageVisible = state.findInPageVisible,
                            findQuery = state.findQuery,
                            findMatchesCurrent = state.findMatchesCurrent,
                            findMatchesTotal = state.findMatchesTotal,
                            bookmarks = state.bookmarks,
                            history = state.history,
                            onOmniboxChanged = viewModel::onOmniboxChanged,
                            onOmniboxFocusChanged = viewModel::onOmniboxFocusChanged,
                            onSubmitOmnibox = { viewModel.submitOmniboxInput() },
                            onReloadOrStop = viewModel::reloadOrStop,
                            onToggleBookmark = viewModel::toggleCurrentBookmark,
                            onOpenInSystemBrowser = { viewModel.openInExternalSystemBrowser(context) },
                            onOpenSecurityInfo = { viewModel.setActiveSheet(ActiveSheet.SecurityInfo) },
                            onOpenSuggestion = { url -> viewModel.navigateToUrl(url) },
                            onFindQueryChanged = viewModel::updateFindQuery,
                            onFindNext = viewModel::findNextMatch,
                            onCloseFind = viewModel::closeFindInPage
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            if (activeTab.isHomePage) {
                                BrowserStartPage(
                                    isIncognito = activeTab.isIncognito,
                                    searchEngine = SearchEngineOption.fromId(state.settings.searchEngineId),
                                    adBlockEnabled = state.settings.adBlockEnabled,
                                    diagnostics = state.diagnostics,
                                    bookmarks = state.bookmarks,
                                    onSubmitSearch = { query -> viewModel.submitOmniboxInput(query) },
                                    onOpenBookmark = { url -> viewModel.navigateToUrl(url) },
                                    onOpenIncognitoTab = { viewModel.openNewTab(isIncognito = true) },
                                    onOpenEngineInspector = { viewModel.setActiveSheet(ActiveSheet.EngineInspector) },
                                    onOpenLibrary = { tabIdx -> viewModel.setActiveSheet(ActiveSheet.Library, tabIdx) },
                                    onOpenInSystemBrowser = { viewModel.openInExternalSystemBrowser(context) }
                                )
                            } else {
                                AndroidView(
                                    factory = { ctx ->
                                        FrameLayout(ctx).apply {
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                        }
                                    },
                                    update = { container ->
                                        val webView = viewModel.webViewManager.getOrCreateWebView(activeTab)
                                        if (container.getChildAt(0) !== webView) {
                                            container.removeAllViews()
                                            (webView.parent as? ViewGroup)?.removeView(webView)
                                            container.addView(
                                                webView,
                                                FrameLayout.LayoutParams(
                                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                                    ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Modal Sheets & Dialogs
        when (state.activeSheet) {
            ActiveSheet.Tabs -> {
                TabsModalSheet(
                    tabs = state.tabs,
                    activeTabId = state.activeTabId,
                    onSelectTab = viewModel::selectTab,
                    onCloseTab = viewModel::closeTab,
                    onNewTab = { incognito -> viewModel.openNewTab(isIncognito = incognito) },
                    onDismiss = { viewModel.setActiveSheet(ActiveSheet.None) }
                )
            }

            ActiveSheet.Menu -> {
                BrowserMenuSheet(
                    activeTab = activeTab,
                    isBookmarked = isBookmarked,
                    onToggleBookmark = viewModel::toggleCurrentBookmark,
                    onToggleDesktopMode = viewModel::toggleDesktopMode,
                    onOpenFindInPage = viewModel::openFindInPage,
                    onShareUrl = { viewModel.shareCurrentUrl(context) },
                    onOpenInSystemBrowser = { viewModel.openInExternalSystemBrowser(context) },
                    onOpenExeInspector = viewModel::triggerExePicker,
                    onOpenLibrary = { idx -> viewModel.setActiveSheet(ActiveSheet.Library, idx) },
                    onOpenEngineInspector = { viewModel.setActiveSheet(ActiveSheet.EngineInspector) },
                    onNewIncognitoTab = { viewModel.openNewTab(isIncognito = true) },
                    onDismiss = { viewModel.setActiveSheet(ActiveSheet.None) }
                )
            }

            ActiveSheet.Library -> {
                LibraryModalSheet(
                    initialTab = state.libraryInitialTab,
                    bookmarks = state.bookmarks,
                    history = state.history,
                    onOpenUrl = { url -> viewModel.navigateToUrl(url) },
                    onDeleteBookmark = viewModel::deleteBookmark,
                    onDeleteHistoryItem = viewModel::deleteHistoryItem,
                    onClearHistory = viewModel::clearHistory,
                    onDismiss = { viewModel.setActiveSheet(ActiveSheet.None) }
                )
            }

            ActiveSheet.EngineInspector -> {
                EngineInspectorSheet(
                    diagnostics = state.diagnostics,
                    settings = state.settings,
                    customBlockedDomains = state.customBlockedDomains,
                    onToggleAdBlock = viewModel::setAdBlockEnabled,
                    onToggleStripTrackers = viewModel::setStripTrackingParams,
                    onToggleJavascript = viewModel::setJavascriptEnabled,
                    onSelectSearchEngine = viewModel::setSearchEngine,
                    onAddCustomDomain = viewModel::addCustomBlockedDomain,
                    onRemoveCustomDomain = viewModel::removeCustomBlockedDomain,
                    onResetStats = viewModel::resetNativeStats,
                    onDismiss = { viewModel.setActiveSheet(ActiveSheet.None) }
                )
            }

            ActiveSheet.SecurityInfo -> {
                SecurityInfoDialog(
                    tab = activeTab,
                    adBlockEnabled = state.settings.adBlockEnabled,
                    onOpenEngineInspector = { viewModel.setActiveSheet(ActiveSheet.EngineInspector) },
                    onDismiss = { viewModel.setActiveSheet(ActiveSheet.None) }
                )
            }

            ActiveSheet.None -> Unit
        }

        state.sslErrorPrompt?.let { prompt ->
            SslErrorDialog(
                prompt = prompt,
                onGoBackSafe = { viewModel.resolveSslError(proceed = false) },
                onProceedAnyway = { viewModel.resolveSslError(proceed = true) }
            )
        }

        state.exeInspectionPrompt?.let { prompt ->
            ExeInspectorDialog(
                prompt = prompt,
                onOpenExternal = { viewModel.openExeInExternalApp(context, prompt.uri) },
                onDismiss = viewModel::dismissExeInspection
            )
        }
    }
}

@Composable
private fun FullscreenVideoOverlay(view: View) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    (view.parent as? ViewGroup)?.removeView(view)
                    addView(
                        view,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
