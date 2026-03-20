package ru.runa.wfe

import android.app.Application
import ru.runa.wfe.data.PreferencesManager
import ru.runa.wfe.rest.ApiClient

class RunaWfeApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        val wfURL = PreferencesManager(this)
            .getValue(PreferencesManager.WEBVIEW_URL, "")
        ApiClient.setServerUrl(wfURL)
    }
}