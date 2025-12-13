@file:Suppress("DEPRECATION")
package ru.runa.wfe.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import ru.runa.wfe.R
import ru.runa.wfe.ui.notification.NotificationSettingsFragment

class SettingsFragment : Fragment(R.layout.settings_fragment) {
    private lateinit var showUrlCheckbox: CheckBox
    private lateinit var changeURLView: SearchView
    private lateinit var prefs: SharedPreferences
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
            findNavController().popBackStack()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(view.context)
        showUrlCheckbox = view.findViewById(R.id.showUrlCheckbox)
        changeURLView = view.findViewById(R.id.searchView)
        backButton = view.findViewById(R.id.backButton)

        val wfurl = prefs.getString("urlQuery", "").toString()
        changeURLView.setQuery(wfurl, true)

        isShowUrl = prefs.getBoolean("showUrl", false)
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

        if (childFragmentManager.findFragmentById(R.id.notificationSettingsContainer) == null) {
            childFragmentManager
                .beginTransaction()
                .replace(R.id.notificationSettingsContainer, NotificationSettingsFragment())
                .commit()
        }
    }

    private fun savePreferences() {
        prefs.edit().putBoolean("showUrl", isShowUrl).apply()
        val changeUrl = changeURLView.query.toString()
        isUrlChanged = changeUrl != prefs.getString("urlQuery", "").toString()
        prefs.edit().putString("urlQuery", changeUrl).apply()
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