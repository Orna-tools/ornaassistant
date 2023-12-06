package com.rockethat.ornaassistant

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.Fragment
import android.content.Intent
import android.net.Uri

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
        // Enable JavaScript but disable other possibly insecure settings
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
        }

        webView.webViewClient = getWebViewClient()
    }

    private fun getWebViewClient() = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            if (Uri.parse(url).host == "www.ornahub.co.uk") {
                // This is your web site, so do not override; let the WebView to load the page
                return false
            }

            // Otherwise, if the link is from an unknown source, open the URL in a Browser Activity
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                startActivity(this)
            }
            return true
        }
    }
}