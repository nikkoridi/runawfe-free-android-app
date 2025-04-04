@file:Suppress("DEPRECATION")

package ru.runa.wfe

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView


class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var urlField: TextView
    private lateinit var prefs: SharedPreferences
    private lateinit var topBar: LinearLayout
    private lateinit var settingsButton: ImageButton

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val wfURL = prefs.getString("urlQuery", "https://wf.processtech.ru/spa/").toString()
        urlField = findViewById(R.id.urlField)
        webView = findViewById(R.id.webview)
        topBar = findViewById(R.id.topBar)
        settingsButton = findViewById(R.id.settingsButton)
        val lastVersion = prefs.getString("last_version", "")
        val currentVersion: String = BuildConfig.VERSION_NAME
        if (lastVersion != currentVersion) {
            webView.clearCache(true)
            webView.reload()
            val editor = prefs.edit()
            editor.putString("last_version", currentVersion)
            editor.apply()
        }
        settingsButton.setOnClickListener {
            prefs.edit().putString("urlQuery", webView.getUrl()).apply()
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        webView.webViewClient = object : WebViewClient() {
            @SuppressLint("WebViewClientOnReceivedSslError", "ObsoleteSdkInt")
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
                    handler.proceed()
                } else {
                    handler.cancel()
                }
            }
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                urlField.text = webView.getUrl()
                prefs.edit().putString("urlQuery", webView.getUrl()).apply()
                if (webView.url.isNullOrBlank() || webView.url == "about:blank") {
                    val emptyURLDialogFragment = EmptyURLDialogFragment()
                    emptyURLDialogFragment.activityOfMessage = this@MainActivity
                    emptyURLDialogFragment.show(fragmentManager, "emptyURLDialog")
                }

            }
        }
        val isShowUrl = prefs.getBoolean("showUrl", false)
        toggleUrlVisibility(isShowUrl)

        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = true
        webView.loadUrl(wfURL)
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
        val settingButtonLayoutParams = settingsButton.layoutParams as RelativeLayout.LayoutParams
        if (isVisible) {
            topBar.visibility = View.VISIBLE
            layoutParams.addRule(RelativeLayout.BELOW, topBar.id)
            settingButtonLayoutParams.topMargin = 96
            settingsButton.layoutParams = settingButtonLayoutParams
        } else {
            topBar.visibility = View.INVISIBLE
            layoutParams.removeRule(RelativeLayout.BELOW)
            settingButtonLayoutParams.topMargin = 0
            settingsButton.layoutParams = settingButtonLayoutParams
        }
    }
}
