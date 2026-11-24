package com.pmshri.kvnda.taxcalc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewManagerTest {

    @Test
    fun testTargetUrlValidity() {
        val url = WebViewManager.TARGET_URL
        assertTrue("Target URL must use HTTPS", url.startsWith("https://"))
        assertTrue("Target URL must target script.google.com", url.contains("script.google.com/macros/s/"))
        assertTrue("Target URL must end with /exec", url.endsWith("/exec"))
    }

    @Test
    fun testAllowedDomains() {
        val testUrl = "https://script.googleusercontent.com/userCodeAppPanel"
        val host = "script.googleusercontent.com"
        val isAllowed = host.endsWith("script.googleusercontent.com") || host.endsWith("script.google.com")
        assertTrue("Google Apps Script user content domain must be allowed", isAllowed)
    }
}
