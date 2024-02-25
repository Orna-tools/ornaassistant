package com.lloir.ornaassistant

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment

class OrnaGuideFragment : Fragment() {

    companion object {
        private const val ORNA_HUB_URL = "https://orna.guide/"
        private const val ORNA_GUIDE_HOST = "www.orna.guide"
    }

    private lateinit var ornaGuideWebView: WebView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_ornahub, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ornaGuideWebView = view.findViewById(R.id.webview)
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

    fun refreshWebpage() {
        ornaGuideWebView.reload()
    }
}