@file:Suppress("DEPRECATION")

package ru.runa.wfe.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.runa.wfe.EmptyURLDialogFragment
import ru.runa.wfe.R
import ru.runa.wfe.databinding.LoginFragmentBinding
import ru.runa.wfe.ui.login.LoginViewModel

class LoginFragment : Fragment(R.layout.login_fragment) {
    private val loginViewModel: LoginViewModel by viewModels()
    private var _binding: LoginFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LoginFragmentBinding.inflate(inflater, container, false)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(view.context)
        val login = binding.login
        val password = binding.login
        val error = binding.errorMessage
        val loginButton = binding.loginButton

        val settingsButton = binding.settingsButton

        loginButton.setOnClickListener {
            loginViewModel.login(login.text.toString(), password.text.toString())
            lifecycleScope.launch {
                loginViewModel.loginFormState.collectLatest { loginResult ->
                    if (loginResult != null && loginResult.success) {
                        if (prefs.getString("urlQuery", "").isNullOrEmpty()) {
                            val emptyURLDialogFragment = EmptyURLDialogFragment()
                            emptyURLDialogFragment.activityOfMessage = requireActivity()
                            emptyURLDialogFragment.show(parentFragmentManager, "emptyURLDialog")
                        }
                        else {
                            prefs.edit().putLogged()
                            findNavController().navigate(R.id.login_to_main)
                        }
                    }
                    else {
                        error.text = getString(R.string.auth_error)
                    }
                }
            }
        }

        settingsButton.setOnClickListener {
            findNavController().navigate(R.id.login_to_settings)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun SharedPreferences.Editor.putLogged(logged: Boolean = true) {
        putBoolean("isLogged", logged)
        apply()
    }
}