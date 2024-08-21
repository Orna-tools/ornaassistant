package com.lloir.ornaassistant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment

class OrnaTowerFragment : Fragment() {

    companion object {
        private const val ORNA_HUB_URL = "https://tower.fqegg.top/"
        private const val ORNA_HUB_HOST = "https://tower.fqegg.top/"
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
            return false
        }
    }

    fun refreshWebpage() {
        ornaGuideWebView.reload()
    }
}