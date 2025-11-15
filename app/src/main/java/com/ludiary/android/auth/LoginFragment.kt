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
import com.ludiary.android.databinding.FragmentLoginBinding
import com.ludiary.android.viewmodel.LoginViewModel
import com.ludiary.android.viewmodel.LoginViewModelFactory
import kotlinx.coroutines.launch

/**
 * Fragmento encargado de manejar la vista de inicio de sesión.
 *
 * Implementa el patron MVVM [LoginViewModel] como fuente de datos y actualiza la interfaz mediante ViewBinding.
 *
 * Este fragmento permite:
 * - Iniciar sesión con correo electrónico y contraseña.
 * - Iniciar sesión de forma anónima.
 * - Recuperar la contraseña.
 */
class LoginFragment : Fragment() {
    /**
     * Referencia interna al objeto de **ViewBinding** asociado a este fragmento.
     */
    private var _binding: FragmentLoginBinding? = null

    /**
     * Acceso no nulo al binding
     */
    private val binding get() = _binding!!

    /**
     * ViewModel asociado al login, obtenido mediante un delegado de fragment
     */
    private val vm: LoginViewModel by viewModels { LoginViewModelFactory() }

    /**
     * Infla la vista de fragment utilizando *view binding*.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Configura la interfaz una vez creada la vista.
     *
     * Registra los listeners de entrada y de clic.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInputListeners()
        setupClickListeners()
        observeUiState()
    }

    /**
     * Asocia los cambios de texto a los campos de entrada del formulario.
     */
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

    /**
     * Configura los manejadores de clic para los botones principales:
     * - Login con correo y contraseña
     * - Login anónimo
     * - Recuperar contraseña
     */
    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener { vm.login() }
        binding.btnAnonymous.setOnClickListener { vm.loginAnonymous() }
        binding.tvForgot.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.authContainer, ForgotPasswordFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.tvGoRegister.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.authContainer, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    /**
     * Observa el flujo de estado [LoginViewModel.ui] y actualiza la interfaz.
     *
     * Muestra los mensajes de error.
     */
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.ui.collect { st ->
                    val isLoading = st.loading
                    binding.btnLogin.isEnabled = !isLoading
                    binding.btnAnonymous.isEnabled = !isLoading
                    binding.tilEmail.isEnabled = !isLoading
                    binding.tilPassword.isEnabled = !isLoading
                    //Limpieza de errores previos
                    binding.tilEmail.error = null
                    binding.tilPassword.error = null
                    //Gestión de errores nuevos
                    st.error?.let { msg ->
                        when {
                            msg.contains("correo", true) -> binding.tilEmail.error = msg
                            msg.contains("contraseña", true) -> binding.tilPassword.error = msg
                            else -> Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG)
                                .show()
                        }
                    }

                    //Estado de éxito -> navegar a la pantalla principal
                    if (st.success) {
                        startActivity(android.content.Intent(requireContext(), com.ludiary.android.ui.main.MainActivity::class.java))
                        requireActivity().finish()
                    }
                }
            }
        }
    }

    /**
     * Libera los recursos del *binding* cuando el fragmento es destruido.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}