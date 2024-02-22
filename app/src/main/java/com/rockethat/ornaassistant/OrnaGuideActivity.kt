package com.rockethat.ornaassistant

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class OrnaGuideActivity : AppCompatActivity() {
    companion object {
        private const val ORNA_HUB_URL = "https://orna.guide"
        private const val ORNA_GUIDE_HOST = "www.orna.guide"
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
            if (Uri.parse(url).host == ORNA_GUIDE_HOST) {
                return false
            }
            launchExternalBrowser(url)
            return true
        }
    }

    private fun launchExternalBrowser(url: String) {
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            startActivity(this)
        }
    }
}