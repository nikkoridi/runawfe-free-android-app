@file:Suppress("DEPRECATION")

package ru.runa.wfe

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.CheckBox
import android.widget.ImageButton

class SettingsActivity : Activity() {

    private lateinit var showUrlCheckbox: CheckBox
    private lateinit var prefs: SharedPreferences
    private lateinit var backButton: ImageButton
    private var isShowUrl: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        showUrlCheckbox = findViewById(R.id.showUrlCheckbox)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        isShowUrl = prefs.getBoolean("showUrl", true)
        showUrlCheckbox.isChecked = isShowUrl

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
        startActivity(Intent(this, MainActivity::class.java))
    }

}