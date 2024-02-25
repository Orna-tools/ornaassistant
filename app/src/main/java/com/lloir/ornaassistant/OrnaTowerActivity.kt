package com.lloir.ornaassistant

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class OrnaTowerActivity : AppCompatActivity() {
    companion object {
        private const val ORNA_HUB_URL = "https://tower.fqegg.top/"
        private const val ORNA_HUB_HOST = "https://tower.fqegg.top/"
    }
    private lateinit var ornaGuideWebView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ornahub)
        ornaGuideWebView = findViewById(R.id.webview)
        setupOrnaGuideWebView()
        ornaGuideWebView.loadUrl(ORNA_HUB_URL)
    }

    private fun setupOrnaGuideWebView() {
        ornaGuideWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
        }
        ornaGuideWebView.webViewClient = createOrnaGuideWebViewClient()
    }

    private fun createOrnaGuideWebViewClient() = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            if (isRequestHostSameAsOrnaHubHost(url)) {
                return false
            }
            launchExternalBrowser(url)
            return true
        }
    }

    private fun isRequestHostSameAsOrnaHubHost(url: String): Boolean {
        return Uri.parse(url).host == ORNA_HUB_HOST
    }

    private fun launchExternalBrowser(url: String) {
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            startActivity(this)
        }
    }
}