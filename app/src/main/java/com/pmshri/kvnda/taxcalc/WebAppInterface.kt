package com.pmshri.kvnda.taxcalc

import android.webkit.JavascriptInterface

/**
 * JavaScript interface that bridges Web events (such as window.print) to native Android features.
 */
class WebAppInterface(
    private val onPrintRequested: () -> Unit,
    private val onCalculationRendered: () -> Unit
) {

    /**
     * Triggered by window.print() or downloadPDF() in the web app.
     */
    @JavascriptInterface
    fun printDocument() {
        onPrintRequested()
    }

    /**
     * Triggered when calculation table is populated.
     */
    @JavascriptInterface
    fun notifyCalculationReady() {
        onCalculationRendered()
    }
}
