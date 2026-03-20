package ru.runa.wfe.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import ru.runa.wfe.data.PreferencesManager
import ru.runa.wfe.R
import ru.runa.wfe.rest.ApiClient
import ru.runa.wfe.ui.notification.NotificationSettingsFragment

class SettingsFragment : Fragment(R.layout.settings_fragment) {
    private lateinit var preferencesManager: PreferencesManager

    private lateinit var showUrlCheckbox: CheckBox
    private lateinit var changeURLView: SearchView
    private lateinit var backButton: ImageButton
    private var isShowUrl: Boolean = false
    private var isUrlChanged: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity()
            .onBackPressedDispatcher
            .addCallback(this) {
                savePreferences()
                goBack()
            }
    }

    private fun goBack() {
        if (isUrlChanged) {
            findNavController().navigate(R.id.settings_to_login)
        } else {
            findNavController().navigateUp()
        }
    }

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
            goBack()
        }

        showUrlCheckbox.setOnCheckedChangeListener { _, isChecked ->
            isShowUrl = isChecked
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

    private fun savePreferences() {
        lifecycleScope.launch {
            preferencesManager.setKey(PreferencesManager.SHOW_URL, isShowUrl)
        }
        onUrlChanged()
    }

    private fun onUrlChanged() {
        val changeUrl = changeURLView.query.toString()
        val oldUrl = preferencesManager
            .getValue(PreferencesManager.WEBVIEW_URL, "")
        if (changeUrl != oldUrl) {
            lifecycleScope.launch {
                preferencesManager.setKey(PreferencesManager.WEBVIEW_URL, changeUrl)
            }
            isUrlChanged = ApiClient.getBaseUrl(changeUrl) != ApiClient.getBaseUrl(oldUrl)
            if (isUrlChanged) {
                lifecycleScope.launch {
                    preferencesManager.setKey(PreferencesManager.IS_LOGGED, false)
                    preferencesManager.deleteKeyValue(PreferencesManager.TOKEN)
                }
                ApiClient.setServerUrl(changeUrl)
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