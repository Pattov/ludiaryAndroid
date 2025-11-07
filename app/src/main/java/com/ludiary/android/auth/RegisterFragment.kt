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
import com.ludiary.android.databinding.FragmentRegisterBinding
import com.ludiary.android.viewmodel.RegisterViewModel
import com.ludiary.android.viewmodel.RegisterViewModelFactory
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val vm: RegisterViewModel by viewModels{ RegisterViewModelFactory() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etEmail.doOnTextChanged { t, _, _, _ -> vm.onEmailChange(t?.toString().orEmpty()) }
        binding.etPassword.doOnTextChanged { t, _, _, _ -> vm.onPasswordChange(t?.toString().orEmpty()) }
        binding.etConfirm.doOnTextChanged { t, _, _, _ -> vm.onConfirmChange(t?.toString().orEmpty()) }

        binding.btnCreate.setOnClickListener { vm.register() }
        binding.tvGoLogin.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.ludiary.android.R.id.authContainer, LoginFragment())
                .addToBackStack(null)
                .commit()
        }

        viewLifecycleOwner.lifecycleScope.launch{
            repeatOnLifecycle(Lifecycle.State.STARTED){
                vm.ui.collect { st ->
                    val loading = st.loading
                    binding.btnCreate.isEnabled = !loading
                    binding.tilEmail.isEnabled = !loading
                    binding.tilPassword.isEnabled = !loading
                    binding.tilConfirm.isEnabled = !loading

                    binding.tilEmail.error = null
                    binding.tilPassword.error = null
                    binding.tilConfirm.error = null

                    st.error?.let { msg ->
                        when {
                            msg.contains("correo", true) -> binding.tilEmail.error = msg
                            msg.contains("contraseña", true) && !msg.contains("coinciden", true) -> binding.tilPassword.error = msg
                            msg.contains("coinciden", true) -> binding.tilConfirm.error = msg
                            else -> Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                        }

                    }

                    if (st.success) {
                        // TODO: Navigate tras registrar
                    }
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}