@file:Suppress("DEPRECATION")
package ru.runa.wfe.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity.DOWNLOAD_SERVICE
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.view.View
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import ru.runa.wfe.BuildConfig
import ru.runa.wfe.EmptyURLDialogFragment
import ru.runa.wfe.R
import ru.runa.wfe.notification.NotificationService
import kotlin.math.abs

class WebFragment : Fragment(R.layout.web_fragment) {

    private lateinit var webView: WebView
    private lateinit var urlField: TextView
    private lateinit var prefs: SharedPreferences
    private lateinit var topBar: LinearLayout
    private lateinit var settingsButton: ImageButton

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity()
            .onBackPressedDispatcher
            .addCallback(this) {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else if (parentFragmentManager.backStackEntryCount >= 1) {
                        parentFragmentManager.popBackStack()
                    }
                else {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(view.context)

        val notificationService = Intent(requireContext(), NotificationService::class.java)
        requireContext().startService(notificationService)
        prefs.edit().remove("isLogged").apply() // Remove when credentials will be stored in App

        val wfURL = prefs.getString("urlQuery", "").toString()
        urlField = view.findViewById(R.id.urlField)
        webView = view.findViewById(R.id.webview)
        topBar = view.findViewById(R.id.topBar)
        settingsButton = view.findViewById(R.id.settingsButton)
        val lastVersion = prefs.getString("last_version", "")
        val currentVersion: String = BuildConfig.VERSION_NAME
        if (lastVersion != currentVersion) {
            webView.clearCache(true)
            webView.reload()
            prefs.edit {
                putString("last_version", currentVersion)
            }
        }

        settingsButton.setOnClickListener {
            prefs.edit { putString("urlQuery", webView.url) }
            findNavController().navigate(R.id.main_to_settings)
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
                urlField.text = webView.url
                prefs.edit { putString("urlQuery", webView.url) }
                if (webView.url.isNullOrBlank() || webView.url == "about:blank") {
                    val emptyURLDialogFragment = EmptyURLDialogFragment()
                    emptyURLDialogFragment.activityOfMessage = requireActivity()
                    emptyURLDialogFragment.show(parentFragmentManager, "emptyURLDialog")
                }

            }
        }
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val request = DownloadManager.Request(url.toUri()).apply {
                setMimeType(mimeType)

                val cookies = CookieManager.getInstance().getCookie(url)
                if (!cookies.isNullOrEmpty()) {
                    addRequestHeader("cookie", cookies)
                }
                addRequestHeader("User-Agent", userAgent)
                setDescription("Downloading file...")
                setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                allowScanningByMediaScanner()
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    setDestinationInExternalFilesDir(
                        requireActivity(),
                        Environment.DIRECTORY_DOWNLOADS,
                        URLUtil.guessFileName(url, contentDisposition, mimeType)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        URLUtil.guessFileName(url, contentDisposition, mimeType))
                }
            }

            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE)
            val downloadManager = requireContext().getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            Handler(Looper.getMainLooper()).postDelayed({
                val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(abs(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)))
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        val uri = downloadManager.getUriForDownloadedFile(downloadId)
                        try {
                            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            startActivity(openIntent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(requireActivity(),
                                "No app found to open this file",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                cursor.close()
            }, 3000)
            Toast.makeText(
                requireActivity(),
                "Downloading File",
                Toast.LENGTH_LONG
            ).show()
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