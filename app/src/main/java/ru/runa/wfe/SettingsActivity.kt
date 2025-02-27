@file:Suppress("DEPRECATION")

package ru.runa.wfe

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.SearchView

class SettingsActivity : Activity() {

    private lateinit var rootLayout: RelativeLayout
    private lateinit var showUrlCheckbox: CheckBox
    private lateinit var changeURLView: SearchView
    private lateinit var prefs: SharedPreferences
    private lateinit var backButton: ImageButton
    private var isShowUrl: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        rootLayout = findViewById(R.id.rootLayout)
        showUrlCheckbox = findViewById(R.id.showUrlCheckbox)
        changeURLView = findViewById(R.id.searchView)

        val wfurl = prefs.getString("urlQuery","https://wf.processtech.ru/spa/").toString()
        changeURLView.setQuery(wfurl,true)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        isShowUrl = prefs.getBoolean("showUrl", false)
        showUrlCheckbox.isChecked = isShowUrl

        rootLayout.setOnClickListener {
            hideKeyboard()
        }
        showUrlCheckbox.setOnCheckedChangeListener { _, isChecked ->
            isShowUrl = isChecked
        }
        backButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            getToMainScreen()
        }
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        getToMainScreen()
    }

    private fun getToMainScreen() {
        prefs.edit().putBoolean("showUrl", isShowUrl).apply()
        prefs.edit().putString("urlQuery", changeURLView.query.toString()).apply()
        startActivity(Intent(this, MainActivity::class.java))
        super.onBackPressed()
    }

    private fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

}