package com.hestudio.notifyforwarders.ui.webview

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun WebViewScreen(
    htmlContent: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            createWebView(context, htmlContent, isDarkTheme)
        },
        update = { webView ->
            webView.evaluateJavascript(
                "document.documentElement.setAttribute('data-theme', '${if (isDarkTheme) "dark" else "light"}');",
                null
            )
        }
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(
    context: android.content.Context,
    htmlContent: String,
    isDarkTheme: Boolean
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            loadWithOverviewMode = true
        }
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }
}
