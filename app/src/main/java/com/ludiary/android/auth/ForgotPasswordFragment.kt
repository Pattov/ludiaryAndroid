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
import com.ludiary.android.R
import com.ludiary.android.databinding.FragmentForgotPasswordBinding
import com.ludiary.android.databinding.FragmentRegisterBinding
import com.ludiary.android.viewmodel.LoginViewModel
import com.ludiary.android.viewmodel.LoginViewModelFactory
import com.ludiary.android.viewmodel.RegisterViewModel
import com.ludiary.android.viewmodel.RegisterViewModelFactory
import kotlinx.coroutines.launch

class ForgotPasswordFragment : Fragment() {
    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val vm: LoginViewModel by viewModels{ LoginViewModelFactory() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.etEmail.doOnTextChanged { t,_,_,_ -> vm.onEmailChanged(t?.toString().orEmpty()) }

        binding.btnSendLink.setOnClickListener { vm.resetPassword() }
        binding.tvBackToLogin.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.ui.collect { st ->
                    val loading = st.loading
                    binding.btnSendLink.isEnabled = !loading
                    binding.tilEmail.isEnabled = !loading
                    binding.tilEmail.error = null

                    st.error?.let { msg ->
                        if (msg.contains("correo", true)) binding.tilEmail.error = msg
                        else Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                    }

                    if (st.success) {
                        Snackbar.make(binding.root, "Hemos enviado el enlace a tu correo.", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}