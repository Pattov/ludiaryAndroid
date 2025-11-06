package com.ludiary.android.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.ludiary.android.databinding.FragmentLoginBinding
import com.ludiary.android.viewmodel.LoginViewModel
import com.ludiary.android.viewmodel.LoginViewModelFactory
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val vm: LoginViewModel by viewModels { LoginViewModelFactory() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInputListeners()
        setupClickListeners()
        observeUiState()
    }

    private fun setupInputListeners() {
        binding.etEmail.doOnTextChanged { text, _, _, _ ->
            vm.onEmailChanged(
                text?.toString().orEmpty()
            )
        }
        binding.etPassword.doOnTextChanged { text, _, _, _ ->
            vm.onPasswordChanged(
                text?.toString().orEmpty()
            )
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener { vm.login() }
        binding.btnAnonymous.setOnClickListener { vm.loginAnonymous() }
        binding.tvForgot.setOnClickListener { vm.resetPassword() }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    vm.ui.collect { st ->
                        val isLoading = st.loading
                        binding.btnLogin.isEnabled = !isLoading
                        binding.btnAnonymous.isEnabled = !isLoading
                        binding.tilEmail.isEnabled = !isLoading
                        binding.tilPassword.isEnabled = !isLoading

                        binding.tilEmail.error = null
                        binding.tilPassword.error = null
                        st.error?.let { msg ->
                            when {
                                msg.contains("correo", true) -> binding.tilEmail.error = msg
                                msg.contains("contraseña", true) -> binding.tilPassword.error = msg
                                else -> Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG)
                                    .show()
                            }
                        }

                        if (st.success) {
                            //navegar a Dashboard
                        }
                    }
                }
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}