@file:Suppress("DEPRECATION")
package ru.runa.wfe

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        as NavHostFragment
        navController = navHostFragment.navController
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val wfURL = prefs.getString("urlQuery", "").toString()
        val isLoggedUser = prefs.getBoolean("isLogged", false)
        if (wfURL.isEmpty()) {
            navController.navigate(R.id.to_settings)
        }
        else if (!isLoggedUser) {
            navController.navigate(R.id.loginFragment)
        }
        else {
            navController.navigate(R.id.mainFragment)
        }
    }
}