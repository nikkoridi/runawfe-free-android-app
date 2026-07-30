package ru.runa.wfe.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.runa.wfe.EmptyURLDialogFragment
import ru.runa.wfe.data.PreferencesManager
import ru.runa.wfe.R
import ru.runa.wfe.databinding.LoginFragmentBinding
import ru.runa.wfe.rest.TokenManager
import ru.runa.wfe.ui.login.LoginViewModel

class LoginFragment : Fragment(R.layout.login_fragment) {
    private lateinit var preferencesManager: PreferencesManager

    private val loginViewModel: LoginViewModel by viewModels()
    private var _binding: LoginFragmentBinding? = null
    private val binding get() = _binding!!

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
        preferencesManager = PreferencesManager.getInstance(view.context)
        val login = binding.login
        val password = binding.password
        val error = binding.errorMessage
        val loginButton = binding.loginButton

        val settingsButton = binding.settingsButton

        loginButton.setOnClickListener {
            loginHandler(login, password, error)
        }

        settingsButton.setOnClickListener {
            findNavController().navigate(R.id.login_to_settings)
        }

    }

    private fun loginHandler(
        login: EditText,
        password: EditText,
        error: TextView
    ) {
        if (preferencesManager
                .getValue(PreferencesManager.WEBVIEW_URL, "").isEmpty()
        ) {
            val emptyURLDialogFragment = EmptyURLDialogFragment()
            emptyURLDialogFragment.activityOfMessage = requireActivity()
            emptyURLDialogFragment.show(parentFragmentManager, "emptyURLDialog")
        }
        val loginValue = login.text.toString().trim()
        val passwordValue = password.text.toString().trim()
        loginViewModel.login(loginValue, passwordValue) { loginResult ->
            if (loginResult.success) {
                saveToken()
                findNavController().popBackStack()
                findNavController().navigate(R.id.mainFragment)
            } else {
                if (loginResult.error != null) {
                    error.text = getString(loginResult.error)
                } else {
                    error.text = getString(R.string.auth_error)
                }
            }
        }
    }

    private fun saveToken() {
        requireActivity().lifecycleScope.launch(Dispatchers.IO) {
            preferencesManager.setSecureKey(
                PreferencesManager.TOKEN,
                TokenManager.token
            )
            TokenManager.clearToken()
            preferencesManager.setKey(PreferencesManager.IS_LOGGED, true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}