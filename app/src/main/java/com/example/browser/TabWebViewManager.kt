package com.example.browser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Environment
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.nativecore.VectraNativeEngine
import java.io.ByteArrayInputStream

class TabWebViewManager(
    private val context: Context,
    private val onPageStarted: (tabId: String, url: String) -> Unit,
    private val onPageFinished: (tabId: String, url: String, title: String, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    private val onProgressChanged: (tabId: String, progress: Int) -> Unit,
    private val onTitleChanged: (tabId: String, title: String) -> Unit,
    private val onTrackerBlocked: (tabId: String) -> Unit,
    private val onOpenNewTabUrl: (url: String, isIncognito: Boolean) -> Unit,
    private val onSslError: (SslErrorPrompt) -> Unit,
    private val onFindResult: (tabId: String, activeOrdinal: Int, totalMatches: Int) -> Unit,
    private val onFileChooserRequested: (ValueCallback<Array<Uri>>, Array<String>) -> Unit,
    private val onFullscreenChanged: (View?) -> Unit,
    private val onNotifyMessage: (String) -> Unit
) {
    private val webViews = mutableMapOf<String, WebView>()
    private var defaultUserAgent: String? = null

    @Volatile
    var adBlockEnabled: Boolean = true

    @Volatile
    var javascriptEnabled: Boolean = true
        set(value) {
            field = value
            webViews.values.forEach { it.settings.javaScriptEnabled = value }
        }

    fun getOrCreateWebView(tab: BrowserTab): WebView {
        return webViews[tab.id] ?: createWebView(tab).also { webViews[tab.id] = it }
    }

    fun getExistingWebView(tabId: String): WebView? = webViews[tabId]

    fun loadUrl(tab: BrowserTab, url: String) {
        if (url == HOME_URL || url.isBlank()) return
        val wv = getOrCreateWebView(tab)
        if (wv.url != url) {
            wv.loadUrl(url)
        }
    }

    fun setDesktopMode(tab: BrowserTab, desktop: Boolean) {
        val wv = webViews[tab.id] ?: return
        applyUserAgent(wv, desktop)
        wv.settings.useWideViewPort = true
        wv.settings.loadWithOverviewMode = true
        if (!tab.isHomePage) {
            wv.reload()
        }
    }

    fun goBack(tabId: String): Boolean {
        val wv = webViews[tabId] ?: return false
        return if (wv.canGoBack()) {
            wv.goBack()
            true
        } else {
            false
        }
    }

    fun goForward(tabId: String): Boolean {
        val wv = webViews[tabId] ?: return false
        return if (wv.canGoForward()) {
            wv.goForward()
            true
        } else {
            false
        }
    }

    fun reload(tabId: String) {
        webViews[tabId]?.reload()
    }

    fun stopLoading(tabId: String) {
        webViews[tabId]?.stopLoading()
    }

    fun findAllAsync(tabId: String, query: String) {
        val wv = webViews[tabId] ?: return
        if (query.isBlank()) {
            wv.clearMatches()
            onFindResult(tabId, 0, 0)
        } else {
            wv.findAllAsync(query)
        }
    }

    fun findNext(tabId: String, forward: Boolean) {
        webViews[tabId]?.findNext(forward)
    }

    fun clearFindMatches(tabId: String) {
        webViews[tabId]?.clearMatches()
    }

    fun destroyTab(tabId: String, wasIncognito: Boolean, anyIncognitoRemaining: Boolean) {
        val wv = webViews.remove(tabId) ?: return
        (wv.parent as? ViewGroup)?.removeView(wv)
        if (wasIncognito) {
            wv.clearHistory()
            wv.clearFormData()
            if (!anyIncognitoRemaining) {
                wv.clearCache(true)
            }
        }
        wv.stopLoading()
        wv.destroy()
    }

    fun destroyAll() {
        webViews.values.forEach { wv ->
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.stopLoading()
            wv.destroy()
        }
        webViews.clear()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(tab: BrowserTab): WebView {
        return WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            if (defaultUserAgent == null) {
                defaultUserAgent = settings.userAgentString
            }

            settings.apply {
                javaScriptEnabled = this@TabWebViewManager.javascriptEnabled
                domStorageEnabled = !tab.isIncognito
                databaseEnabled = !tab.isIncognito
                builtInZoomControls = true
                displayZoomControls = false
                useWideViewPort = true
                loadWithOverviewMode = true
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(true)
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                allowFileAccess = false
                allowContentAccess = true
                loadsImagesAutomatically = true
                offscreenPreRaster = true
                @Suppress("DEPRECATION")
                saveFormData = !tab.isIncognito
                cacheMode = if (tab.isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
            }

            applyUserAgent(this, tab.isDesktopMode)

            val webView = this
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(webView, !tab.isIncognito)
            }

            setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
                if (isDoneCounting) {
                    val current = if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0
                    onFindResult(tab.id, current, numberOfMatches)
                }
            }

            setDownloadListener(DownloadListener { downloadUrl, userAgent, contentDisposition, mimeType, _ ->
                enqueueSystemDownload(downloadUrl, userAgent, contentDisposition, mimeType)
            })

            webViewClient = object : WebViewClient() {
                @Volatile
                private var currentPageHost: String = ""

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val uri = request?.url ?: return null
                    if (adBlockEnabled && !request.isForMainFrame) {
                        val scheme = uri.scheme?.lowercase()
                        if (scheme == "http" || scheme == "https") {
                            if (VectraNativeEngine.shouldBlockRequest(uri.toString(), currentPageHost)) {
                                onTrackerBlocked(tab.id)
                                return WebResourceResponse(
                                    "text/plain",
                                    "utf-8",
                                    ByteArrayInputStream(ByteArray(0))
                                )
                            }
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val uri = request?.url ?: return false
                    val scheme = uri.scheme?.lowercase() ?: return false
                    if (scheme == "http" || scheme == "https" || scheme == "about") {
                        return false
                    }
                    // Handle external schemes like tel:, mailto:, geo:, intent:
                    return try {
                        val intent = if (scheme == "intent") {
                            Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                        } else {
                            Intent(Intent.ACTION_VIEW, uri)
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        true
                    } catch (_: Exception) {
                        onNotifyMessage("No hay aplicación disponible para abrir $scheme:")
                        true
                    }
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    val safeUrl = url ?: return
                    currentPageHost = VectraNativeEngine.extractHost(safeUrl)
                    onPageStarted(tab.id, safeUrl)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    val safeUrl = url ?: view?.url ?: return
                    val safeTitle = view?.title?.takeIf { it.isNotBlank() } ?: safeUrl
                    onPageFinished(
                        tab.id,
                        safeUrl,
                        safeTitle,
                        view?.canGoBack() == true,
                        view?.canGoForward() == true
                    )
                }

                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler?,
                    error: SslError?
                ) {
                    if (handler == null) return
                    val failingUrl = error?.url ?: view?.url.orEmpty()
                    val summary = when (error?.primaryError) {
                        SslError.SSL_EXPIRED -> "El certificado SSL ha expirado."
                        SslError.SSL_IDMISMATCH -> "El nombre del certificado no coincide con el dominio."
                        SslError.SSL_UNTRUSTED -> "La autoridad emisora del certificado no es de confianza."
                        SslError.SSL_DATE_INVALID -> "La fecha del certificado SSL no es válida."
                        else -> "Error de verificación en el certificado SSL/TLS."
                    }
                    onSslError(SslErrorPrompt(tab.id, failingUrl, summary, handler))
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    onProgressChanged(tab.id, newProgress)
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    if (!title.isNullOrBlank()) {
                        onTitleChanged(tab.id, title)
                    }
                }

                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    val hitTest = view?.hitTestResult
                    val targetUrl = hitTest?.extra
                    if (!targetUrl.isNullOrBlank() && (targetUrl.startsWith("http://") || targetUrl.startsWith("https://"))) {
                        onOpenNewTabUrl(targetUrl, tab.isIncognito)
                        return true
                    }
                    return false
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    if (filePathCallback == null) return false
                    val acceptTypes = fileChooserParams?.acceptTypes
                        ?.filter { it.isNotBlank() }
                        ?.toTypedArray()
                        ?: arrayOf("*/*")
                    onFileChooserRequested(filePathCallback, acceptTypes.ifEmpty { arrayOf("*/*") })
                    return true
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    onFullscreenChanged(view)
                }

                override fun onHideCustomView() {
                    onFullscreenChanged(null)
                }
            }
        }
    }

    private fun applyUserAgent(webView: WebView, desktopMode: Boolean) {
        val baseUa = defaultUserAgent ?: webView.settings.userAgentString
        webView.settings.userAgentString = if (desktopMode) {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        } else {
            baseUa
        }
    }

    private fun enqueueSystemDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ) {
        try {
            val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                val cookies = CookieManager.getInstance().getCookie(url)
                if (!cookies.isNullOrBlank()) addRequestHeader("cookie", cookies)
                if (!userAgent.isNullOrBlank()) addRequestHeader("User-Agent", userAgent)
                setTitle(filename)
                setDescription("Descargando con Vectra")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            onNotifyMessage("Descargando: $filename")
        } catch (_: ActivityNotFoundException) {
            onNotifyMessage("No se pudo iniciar la descarga")
        } catch (e: Exception) {
            onNotifyMessage("Error al descargar: ${e.message ?: "desconocido"}")
        }
    }
}
