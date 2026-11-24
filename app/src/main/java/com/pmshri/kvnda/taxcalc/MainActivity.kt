package com.pmshri.kvnda.taxcalc

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pmshri.kvnda.taxcalc.databinding.ActivityMainBinding

/**
 * Main Activity for PM SHRI KV NDA Pune Income Tax Calculator application.
 * Manages the native UI shell, WebView integration, network monitoring,
 * and Android system print service for PDF export.
 */
class MainActivity : AppCompatActivity(), WebViewManager.WebViewEventListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var webViewManager: WebViewManager
    private lateinit var networkManager: NetworkManager

    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    private var isFirstLoad = true

    // File chooser launcher for WebView file uploads
    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val results: Array<Uri>? = if (result.resultCode == RESULT_OK) {
                result.data?.let { data ->
                    val singleUri = data.data
                    val clipData = data.clipData
                    when {
                        clipData != null -> {
                            Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                        }
                        singleUri != null -> {
                            arrayOf(singleUri)
                        }
                        else -> null
                    }
                }
            } else {
                null
            }
            fileUploadCallback?.onReceiveValue(results)
            fileUploadCallback = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupManagers()
        setupUI()
        setupBackNavigation()

        loadApplication()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topToolbar.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    private fun setupManagers() {
        networkManager = NetworkManager(this)
        webViewManager = WebViewManager(this, binding.webView, this)

        // Monitor real-time network state transitions
        networkManager.registerNetworkListener(
            onAvailable = {
                runOnUiThread {
                    // If currently displaying the offline screen, automatically retry
                    if (binding.noInternetView.visibility == View.VISIBLE) {
                        retryConnection()
                    }
                }
            },
            onLost = {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        getString(R.string.no_internet_title),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun setupUI() {
        // Toolbar refresh button
        binding.btnToolbarRefresh.setOnClickListener {
            if (networkManager.isNetworkAvailable()) {
                webViewManager.reload()
            } else {
                showOfflineScreen()
            }
        }

        // Toolbar quick print button
        binding.btnQuickPrint.setOnClickListener {
            onPrintRequested()
        }

        // Pull to refresh
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.primary,
            R.color.accent,
            R.color.success_green
        )
        binding.swipeRefreshLayout.setOnRefreshListener {
            if (networkManager.isNetworkAvailable()) {
                webViewManager.reload()
            } else {
                binding.swipeRefreshLayout.isRefreshing = false
                showOfflineScreen()
            }
        }

        // Retry button on offline screen
        binding.btnRetry.setOnClickListener {
            retryConnection()
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webViewManager.canGoBack()) {
                    webViewManager.goBack()
                } else {
                    showExitDialog()
                }
            }
        })
    }

    private fun loadApplication() {
        if (networkManager.isNetworkAvailable()) {
            showLoadingView()
            webViewManager.loadApp()
        } else {
            showOfflineScreen()
        }
    }

    private fun retryConnection() {
        if (networkManager.isNetworkAvailable()) {
            hideOfflineScreen()
            showLoadingView()
            webViewManager.loadApp()
        } else {
            Toast.makeText(
                this,
                getString(R.string.no_internet_msg),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showLoadingView() {
        binding.initialLoadingView.visibility = View.VISIBLE
        binding.noInternetView.visibility = View.GONE
        binding.swipeRefreshLayout.visibility = View.GONE
    }

    private fun hideLoadingView() {
        binding.initialLoadingView.visibility = View.GONE
        binding.swipeRefreshLayout.visibility = View.VISIBLE
    }

    private fun showOfflineScreen() {
        binding.swipeRefreshLayout.isRefreshing = false
        binding.initialLoadingView.visibility = View.GONE
        binding.swipeRefreshLayout.visibility = View.GONE
        binding.noInternetView.visibility = View.VISIBLE
    }

    private fun hideOfflineScreen() {
        binding.noInternetView.visibility = View.GONE
        binding.swipeRefreshLayout.visibility = View.VISIBLE
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.exit_confirm_title)
            .setMessage(R.string.exit_confirm_msg)
            .setPositiveButton(R.string.exit_yes) { _, _ -> finish() }
            .setNegativeButton(R.string.exit_cancel, null)
            .show()
    }

    // --- WebViewEventListener Callbacks ---

    override fun onProgressUpdate(progress: Int) {
        if (progress in 1..99) {
            binding.progressBar.visibility = View.VISIBLE
            binding.progressBar.progress = progress
        } else {
            binding.progressBar.visibility = View.GONE
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onPageLoadingStarted(url: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.progress = 10
    }

    override fun onPageLoadingFinished(url: String) {
        binding.progressBar.visibility = View.GONE
        binding.swipeRefreshLayout.isRefreshing = false
        if (isFirstLoad) {
            isFirstLoad = false
            hideLoadingView()
        }
        hideOfflineScreen()
    }

    override fun onErrorEncountered(errorCode: Int, description: String) {
        showOfflineScreen()
    }

    override fun onFileChoose(
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Boolean {
        fileUploadCallback?.onReceiveValue(null)
        fileUploadCallback = filePathCallback

        val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        return try {
            fileChooserLauncher.launch(intent)
            true
        } catch (_: Exception) {
            fileUploadCallback = null
            false
        }
    }

    override fun onPrintRequested() {
        runOnUiThread {
            try {
                Toast.makeText(
                    this,
                    "Preparing PDF / Print preview...",
                    Toast.LENGTH_SHORT
                ).show()
                val printManager = getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (printManager != null) {
                    val jobName = "Income_Tax_Calculation_2026_27_KV_NDA"
                    val printAdapter = binding.webView.createPrintDocumentAdapter(jobName)
                    val attributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
                        .build()
                    printManager.print(jobName, printAdapter, attributes)
                } else {
                    Toast.makeText(
                        this,
                        "Printing service is unavailable on this device",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Print error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCalculationReady() {
        runOnUiThread {
            // Reveal quick print button in the toolbar
            binding.btnQuickPrint.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        networkManager.unregisterNetworkListener()
        webViewManager.destroy()
        super.onDestroy()
    }
}
