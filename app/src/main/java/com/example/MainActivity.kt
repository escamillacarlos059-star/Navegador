package com.example

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.browser.BrowserViewModel
import com.example.ui.VectraBrowserApp

class MainActivity : ComponentActivity() {

    private val browserViewModel: BrowserViewModel by viewModels()
    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val result = if (uris.isNullOrEmpty()) null else uris.toTypedArray()
        pendingFileCallback?.onReceiveValue(result)
        pendingFileCallback = null
    }

    private val exePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            browserViewModel.inspectExeUri(this, uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        browserViewModel.fileChooserCallback = { callback, acceptTypes ->
            pendingFileCallback?.onReceiveValue(null)
            pendingFileCallback = callback
            fileChooserLauncher.launch(acceptTypes)
        }

        browserViewModel.openExePickerCallback = {
            exePickerLauncher.launch(
                arrayOf(
                    "application/x-msdownload",
                    "application/vnd.microsoft.portable-executable",
                    "application/octet-stream",
                    "*/*"
                )
            )
        }

        if (savedInstanceState == null) {
            dispatchIncomingIntent(intent)
        }

        setContent {
            VectraBrowserApp(viewModel = browserViewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dispatchIncomingIntent(intent)
    }

    private fun dispatchIncomingIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val url = intent.dataString
                val uri = intent.data
                if (uri != null && (uri.scheme == "content" || uri.scheme == "file")) {
                    browserViewModel.inspectExeUri(this, uri)
                } else if (!url.isNullOrBlank()) {
                    browserViewModel.handleIncomingSystemIntent(url)
                }
            }

            Intent.ACTION_WEB_SEARCH -> {
                val query = intent.getStringExtra(SearchManager.QUERY)
                if (!query.isNullOrBlank()) {
                    browserViewModel.handleIncomingSystemIntent(query)
                }
            }
        }
    }
}
