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
import com.ludiary.android.databinding.FragmentForgotPasswordBinding
import com.ludiary.android.viewmodel.LoginViewModel
import com.ludiary.android.viewmodel.LoginViewModelFactory
import kotlinx.coroutines.launch

/**
 * Fragmento encargado de gestionar la vista de recuperación de contraseña
 *
 * Permite al usuario introducir el correo para recibir un enlace para restablecer la contraseña a través de Firebase
 *
 * Reutiliza el [LoginViewModel] para aprovechar la funcionalidad [LoginViewModel.resetPassword].
 */
class ForgotPasswordFragment : Fragment() {
    /** Referencia interna al objeto de ViewBinding asociado al fragmento */
    private var _binding: FragmentForgotPasswordBinding? = null

    /** Acceso no nulo al binding asociado al fragmento */
    private val binding get() = _binding!!

    /** ViewModel compartido con el flujo de autenticación, encargado de gestión la lógica de recuperación */
    private val vm: LoginViewModel by viewModels{ LoginViewModelFactory() }

    /**
     * Infla el layout [FragmentForgotPasswordBinding] correspondiente al fragmento.
     *
     * @return La vista raíz del fragmento.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Configura la lógica de interacción y la observación del estado del ViewModel
     *
     *  - Captura los cambios en el campo de correo electrónico.
     *  - Gestiona el envío del enlace de recuperación
     *  - Permite volver al inicia de sesión.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // Escucha los cambios en el campo de correo electrónico.
        binding.etEmail.doOnTextChanged { t,_,_,_ -> vm.onEmailChanged(t?.toString().orEmpty()) }

        // Envia la solicitud de recuperación de contraseña.
        binding.btnSendLink.setOnClickListener { vm.resetPassword() }

        // Regresa al fragmento de login anterior.
        binding.tvBackToLogin.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Observa el estado del ViewModel y actualiza la interfaz.
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.ui.collect { st ->
                    val loading = st.loading
                    binding.btnSendLink.isEnabled = !loading
                    binding.tilEmail.isEnabled = !loading
                    binding.tilEmail.error = null
                    // Muestra mensajes de error.
                    st.error?.let { msg ->
                        if (msg.contains("correo", true)) binding.tilEmail.error = msg
                        else Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                    }
                    // Confirma visualmente el envío del enlace.
                    if (st.success) {
                        Snackbar.make(binding.root, "Hemos enviado el enlace a tu correo.", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    /**
     * Libera los recursos del binding cuando la vista se destruye para evitar fugas de memoria.
     */
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}