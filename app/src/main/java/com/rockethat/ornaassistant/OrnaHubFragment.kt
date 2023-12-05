package com.rockethat.ornaassistant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment

class OrnaHubFragment : Fragment() {

    companion object {
        private const val ORNA_HUB_URL = "https://ornahub.co.uk"
    }

    private lateinit var webView: WebView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ornahub, container, false)
        webView = view.findViewById(R.id.webview)
        setupWebView()
        webView.loadUrl(ORNA_HUB_URL)
        return view
    }

    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = getWebViewClient()
    }

    private fun getWebViewClient() = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            return url?.startsWith(ORNA_HUB_URL) == false
        }
    }
}
