package com.pmshri.kvnda.taxcalc

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.view.View
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Encapsulates WebView configuration, security settings, WebChromeClient, and WebViewClient
 * for seamless and secure execution of the Google Apps Script web application.
 */
class WebViewManager(
    private val context: Context,
    private val webView: WebView,
    private val listener: WebViewEventListener
) {

    interface WebViewEventListener {
        fun onProgressUpdate(progress: Int)
        fun onPageLoadingStarted(url: String)
        fun onPageLoadingFinished(url: String)
        fun onErrorEncountered(errorCode: Int, description: String)
        fun onFileChoose(
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: WebChromeClient.FileChooserParams?
        ): Boolean
        fun onPrintRequested()
        fun onCalculationReady()
    }

    companion object {
        const val TARGET_URL =
            "https://script.google.com/macros/s/AKfycbwik4YejYW3GlAQYdwcDnI0Vlu2iwGXsnCBTNTiqck1oFgxbsj9HV6MRthd6rx_lmqZXQ/exec"

        private val ALLOWED_DOMAINS = listOf(
            "script.google.com",
            "script.googleusercontent.com",
            "accounts.google.com",
            "accounts.youtube.com",
            "apis.google.com",
            "google.com",
            "gstatic.com"
        )
    }

    init {
        configureSettings()
        setupClients()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureSettings() {
        val settings = webView.settings

        // 1. Core script & storage features required by Google Apps Script
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true

        // 2. Viewport and responsive layout
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false

        // 3. Security configurations
        // Never allow mixed HTTP/HTTPS content
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        // Disable file system access for enhanced security against cross-origin scripting
        settings.allowFileAccess = false
        settings.allowContentAccess = false

        // 4. Session & Cookie Persistence (Essential for GAS iframe cross-domain communication)
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        // 5. Caching & Performance
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // 6. User Agent Configuration
        // Ensure standard modern mobile Chrome UserAgent so Google does not block WebView login
        val defaultUserAgent = settings.userAgentString
        // Remove "wv" token from User Agent if present to prevent Google OAuth/Auth blocks
        val cleanedUserAgent = defaultUserAgent.replace("; wv", "")
        settings.userAgentString = cleanedUserAgent

        // 7. Input and scrolling optimizations
        webView.isScrollbarFadingEnabled = true
        webView.isVerticalScrollBarEnabled = true
        webView.isHorizontalScrollBarEnabled = true
        webView.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

        // 8. Register JavaScript Interface for native PDF printing and status bridge
        webView.addJavascriptInterface(
            WebAppInterface(
                onPrintRequested = { listener.onPrintRequested() },
                onCalculationRendered = { listener.onCalculationReady() }
            ),
            "AndroidBridge"
        )
    }

    private fun setupClients() {
        // WebChromeClient handles loading progress, file chooser, and window title
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                listener.onProgressUpdate(newProgress)
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                return listener.onFileChoose(filePathCallback, fileChooserParams)
            }
        }

        // WebViewClient handles navigation, redirects, SSL errors, and page life-cycle
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                val host = uri.host?.lowercase() ?: ""

                // Allow navigation within Google Apps Script & Google Auth domains
                val isAllowed = ALLOWED_DOMAINS.any { allowed ->
                    host == allowed || host.endsWith(".$allowed")
                }

                return if (isAllowed) {
                    false // Let WebView load it
                } else {
                    // Open external links (e.g. Terms, Help, Drive Abuse reports) in external browser
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    } catch (_: Exception) {
                    }
                    true
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let { listener.onPageLoadingStarted(it) }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Inject helper script to bridge window.print() and detect calculation table render
                injectPrintAndTableBridge()
                url?.let { listener.onPageLoadingFinished(it) }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                // Only show offline screen for the main frame failure
                if (request?.isForMainFrame == true) {
                    val errorCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        error?.errorCode ?: -1
                    } else {
                        -1
                    }
                    val description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        error?.description?.toString() ?: "Connection Error"
                    } else {
                        "Connection Error"
                    }
                    listener.onErrorEncountered(errorCode, description)
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 500) {
                    listener.onErrorEncountered(
                        errorResponse?.statusCode ?: 500,
                        "Server Error (${errorResponse?.statusCode})"
                    )
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                // Strict Security: NEVER bypass SSL certificate validation
                handler?.cancel()
                listener.onErrorEncountered(-100, "SSL Certificate Validation Failed")
            }
        }
    }

    /**
     * Injects a lightweight script into the page that bridges `window.print()` to native Android printing,
     * and monitors for table display to activate the native print shortcut.
     */
    private fun injectPrintAndTableBridge() {
        val bridgeScript = """
            (function() {
                // Bridge window.print() to native Android PrintManager
                window.print = function() {
                    if (window.AndroidBridge && window.AndroidBridge.printDocument) {
                        window.AndroidBridge.printDocument();
                    }
                };

                // Watch for result table rendering to trigger native quick-print action
                var observer = new MutationObserver(function(mutations) {
                    var resultEl = document.getElementById('result');
                    if (resultEl && resultEl.children && resultEl.children.length > 0) {
                        if (window.AndroidBridge && window.AndroidBridge.notifyCalculationReady) {
                            window.AndroidBridge.notifyCalculationReady();
                        }
                    }
                });
                var target = document.getElementById('result') || document.body;
                if (target) {
                    observer.observe(target, { childList: true, subtree: true });
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(bridgeScript, null)
    }

    fun loadApp() {
        webView.loadUrl(TARGET_URL)
    }

    fun reload() {
        webView.reload()
    }

    fun canGoBack(): Boolean = webView.canGoBack()

    fun goBack() {
        webView.goBack()
    }

    fun destroy() {
        webView.stopLoading()
        webView.clearHistory()
        webView.removeAllViews()
        webView.destroy()
    }
}
