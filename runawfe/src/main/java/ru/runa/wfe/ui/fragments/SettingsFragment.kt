package ru.runa.wfe.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import ru.runa.wfe.EmptyURLDialogFragment
import ru.runa.wfe.data.PreferencesManager
import ru.runa.wfe.R
import ru.runa.wfe.rest.ApiClient
import ru.runa.wfe.rest.ServerCheckResult
import ru.runa.wfe.ui.notification.NotificationSettingsFragment

class SettingsFragment : Fragment(R.layout.settings_fragment) {
    private lateinit var preferencesManager: PreferencesManager

    private lateinit var showUrlCheckbox: CheckBox
    private lateinit var changeURLView: SearchView
    private lateinit var backButton: ImageButton
    private var isShowUrl: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferencesManager = PreferencesManager(view.context)
        showUrlCheckbox = view.findViewById(R.id.showUrlCheckbox)
        changeURLView = view.findViewById(R.id.searchView)
        backButton = view.findViewById(R.id.backButton)

        val wfurl = preferencesManager
            .getValue(PreferencesManager.WEBVIEW_URL, "")
        changeURLView.setQuery(wfurl, true)

        isShowUrl = preferencesManager
            .getValue(PreferencesManager.SHOW_URL, false)
        showUrlCheckbox.isChecked = isShowUrl

        view.findViewById<LinearLayout>(R.id.rootLayout).setOnClickListener {
            hideKeyboard()
        }

        backButton.setOnClickListener {
            savePreferences()
        }

        showUrlCheckbox.setOnCheckedChangeListener { _, isChecked ->
            isShowUrl = isChecked
            saveShowUrl()
        }

        changeURLView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                onUrlChanged()
            }
        }

        if (childFragmentManager.findFragmentById(R.id.notificationSettingsContainer) == null) {
            childFragmentManager
                .beginTransaction()
                .replace(R.id.notificationSettingsContainer, NotificationSettingsFragment())
                .commit()
        }
    }

    private fun saveShowUrl() {
        lifecycleScope.launch {
            preferencesManager.setKey(PreferencesManager.SHOW_URL, isShowUrl)
        }
    }

    private fun savePreferences() {
        saveShowUrl()
        onUrlChanged()
    }

    private fun onUrlChanged() {
        val changeUrl = changeURLView.query.toString()
        if (changeUrl.isEmpty()) {
            // TODO: it's the third copy, will it be better as interface for Activity?
            val emptyURLDialogFragment = EmptyURLDialogFragment()
            emptyURLDialogFragment.activityOfMessage = requireActivity()
            emptyURLDialogFragment.show(parentFragmentManager, "emptyURLDialog")
            return
        }
        val oldUrl = preferencesManager.getValue(PreferencesManager.WEBVIEW_URL, "")
        if (changeUrl == oldUrl) {
            return
        }
        val originChangeUrl = ApiClient.toOrigin(changeUrl)
        val isUrlHostsEqual = originChangeUrl == ApiClient.toOrigin(ApiClient.baseUrl)
        lifecycleScope.launch {
            if (isUrlHostsEqual) {
                preferencesManager.setKey(PreferencesManager.WEBVIEW_URL, changeUrl)
                ApiClient.setServerUrl(ServerCheckResult.Valid(originChangeUrl))
            } else {
                val checkResult: ServerCheckResult = ApiClient.checkServer(changeUrl)
                when {
                    // Don't block possibility to change url in case of bad network
                    checkResult != ServerCheckResult.Invalid -> {
                        preferencesManager.setKey(PreferencesManager.WEBVIEW_URL, changeUrl)
                    }

                    checkResult is ServerCheckResult.Valid -> {
                        ApiClient.setServerUrl(checkResult)
                        preferencesManager.setKey(PreferencesManager.IS_LOGGED, false)
                        preferencesManager.deleteKeyValue(PreferencesManager.TOKEN)
                    }

                    else -> {
                        view?.let {
                            Snackbar.make(
                                it,
                                if (checkResult is ServerCheckResult.Invalid)
                                    R.string.invalid_url
                                else R.string.network_error_url,
                                30000
                            )
                                .show()
                        }
                    }
                }
            }
        }
    }

    private fun hideKeyboard() {
        val view = requireActivity().currentFocus
        if (view != null) {
            val imm = requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}