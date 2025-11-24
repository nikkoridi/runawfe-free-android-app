@file:Suppress("DEPRECATION")

package ru.runa.wfe

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.SearchView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import ru.runa.wfe.ui.notification.NotificationSettingsFragment


class SettingsActivity : AppCompatActivity() {

    private lateinit var rootLayout: RelativeLayout
    private lateinit var showUrlCheckbox: CheckBox
    private lateinit var changeURLView: SearchView
    private lateinit var prefs: SharedPreferences
    private lateinit var backButton: ImageButton
    private var isShowUrl: Boolean = false

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        rootLayout = findViewById(R.id.rootLayout)
        showUrlCheckbox = findViewById(R.id.showUrlCheckbox)
        changeURLView = findViewById(R.id.searchView)
        backButton = findViewById(R.id.backButton)

        val wfurl = prefs.getString("urlQuery", "").toString()
        changeURLView.setQuery(wfurl, true)

        isShowUrl = prefs.getBoolean("showUrl", false)
        showUrlCheckbox.isChecked = isShowUrl

        rootLayout.setOnClickListener {
            hideKeyboard()
        }
        showUrlCheckbox.setOnCheckedChangeListener { _, isChecked ->
            isShowUrl = isChecked
        }
        backButton.setOnClickListener {
            getToMainScreen()
        }
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.notificationSettingsContainer, NotificationSettingsFragment())
                .commit()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
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