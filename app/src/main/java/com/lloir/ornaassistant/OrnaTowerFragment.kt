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
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class OrnaTowerFragment : Fragment() {

    companion object {
        private const val ORNA_HUB_URL = "https://tower.fqegg.top/"
        private const val ORNA_HUB_HOST = "https://tower.fqegg.top/"
    }

    private lateinit var ornaTowerWebView: WebView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_ornahub, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ornaTowerWebView = view.findViewById(R.id.webview)
        setupornaTowerWebView()
        ornaTowerWebView.loadUrl(ORNA_HUB_URL)
    }

    private fun setupornaTowerWebView() {
        ornaTowerWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
        }
        ornaTowerWebView.webViewClient = createornaTowerWebViewClient()
    }

    private fun createornaTowerWebViewClient() = object : WebViewClient() {
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
    fun refreshWebpage() {
        ornaTowerWebView.reload()
    }
}