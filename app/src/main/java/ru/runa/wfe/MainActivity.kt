@file:Suppress("DEPRECATION")

package ru.runa.wfe

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout


class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var urlField: EditText
    private lateinit var prefs: SharedPreferences
    private lateinit var topBar: LinearLayout
    private lateinit var settingsButton: ImageButton

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val wfurl = "https://wf.processtech.ru/spa/"
        urlField = findViewById(R.id.urlField)
        webView = findViewById(R.id.webview)
        topBar = findViewById(R.id.topBar)
        settingsButton = findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
                return true
            }
            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
                    // Если версия Android 7 (Nougat) или ниже, продолжаем загрузку страницы
                    handler.proceed()
                } else {
                    handler.cancel()
                }
            }
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                urlField.setText(webView.getUrl())
            }
        }
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isShowUrl = prefs.getBoolean("showUrl", true)
        toggleUrlVisibility(isShowUrl)

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        webView.loadUrl(wfurl)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
    private fun toggleUrlVisibility(isVisible: Boolean) {
        val layoutParams = webView.layoutParams as RelativeLayout.LayoutParams
        if (isVisible) {
            topBar.visibility = View.VISIBLE
            layoutParams.addRule(RelativeLayout.BELOW, topBar.id)
        } else {
            topBar.visibility = View.INVISIBLE
            layoutParams.removeRule(RelativeLayout.BELOW)
        }
        webView.layoutParams = layoutParams
    }
}
