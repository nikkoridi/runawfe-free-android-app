@file:Suppress("DEPRECATION")
package ru.runa.wfe.ui.login

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.lifecycle.Observer
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import ru.runa.wfe.MainActivity
import ru.runa.wfe.SettingsActivity
import ru.runa.wfe.databinding.ActivityLoginBinding


class LoginActivity : AppCompatActivity() {
    private val loginViewModel: LoginViewModel by viewModels()
    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val login = binding.login
        val password = binding.login
        val error = binding.errorMessage
        val loginButton = binding.loginButton

        val settingsButton = binding.settingsButton

        loginButton.setOnClickListener {
            loginViewModel.login(login.text.toString(), password.text.toString())
        }

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer
            when (loginState) {
                is LoginResult.Error -> {
                    error.text = loginState.message
                }
                is  LoginResult.Success -> {
                    setResult(Activity.RESULT_OK)
                    // Complete and destroy login activity once successful
                    prefs.edit().putLogged()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
            }
        })

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

    }

    private fun SharedPreferences.Editor.putLogged(logged: Boolean = true) {
        putBoolean("isLogged", logged)
        apply()
    }
 }