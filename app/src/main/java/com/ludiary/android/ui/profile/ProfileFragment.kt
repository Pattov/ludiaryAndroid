package com.ludiary.android.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ludiary.android.R
import com.ludiary.android.auth.AuthActivity
import com.ludiary.android.viewmodel.ProfileUiState
import com.ludiary.android.viewmodel.ProfileViewModel
import com.ludiary.android.viewmodel.ProfileViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.content.edit

/**
 * Pantalla destinada a mostrar la información del perfil del usuario y opciones de configuración.
 */
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    /**
     * ViewModel inicializado mediante un Factory que construye el repositorio con Firebase + Room.
     */
    private val vm: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(requireContext())
    }

    /**
     * Configura la interfaz del perfil una vez que la vista ha sido creada.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Elementos de UI del nuevo layout
        val tvDisplayName = view.findViewById<TextView>(R.id.tvDisplayName)
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        val rowEditProfile = view.findViewById<LinearLayout>(R.id.rowEditProfile)
        val rowPreferences = view.findViewById<LinearLayout>(R.id.rowPreferences)
        val rowSync = view.findViewById<LinearLayout>(R.id.rowSync)
        val rowFriends = view.findViewById<LinearLayout>(R.id.rowFriends)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        // --------------------- Observación del estado -------------------------
        viewLifecycleOwner.lifecycleScope.launch {
            vm.ui.collectLatest { st: ProfileUiState ->
                val user = st.user

                if (user != null) {

                    val displayName = user.displayName?.takeIf { it.isNotBlank() }
                    val emailRaw = user.email
                    val isGuest = user.isAnonymous

                    val mainTitle: String
                    val subtitle: String?

                    if (isGuest && displayName == null && emailRaw.isNullOrBlank()) {
                        // Invitado sin nombre ni correo → "Invitado" como título
                        mainTitle = getString(R.string.profile_guest_name)
                        subtitle = getString(R.string.profile_email_guest)
                    } else if (displayName != null) {
                        // Hay alias → alias como título, email debajo
                        mainTitle = displayName
                        subtitle = when {
                            emailRaw != null -> emailRaw
                            isGuest -> getString(R.string.profile_email_guest)
                            else -> getString(R.string.profile_email_unknown)
                        }
                    } else {
                        // No hay alias → el correo (o texto) hace de título
                        mainTitle = when {
                            emailRaw != null -> emailRaw
                            else -> getString(R.string.profile_email_unknown)
                        }
                        subtitle = null
                    }

                    tvDisplayName.text = mainTitle

                    if (subtitle == null) {
                        tvEmail.visibility = View.GONE
                    } else {
                        tvEmail.visibility = View.VISIBLE
                        tvEmail.text = subtitle
                    }

                    // Texto del botón según tipo de usuario
                    btnLogout.text = if (user.isAnonymous) {
                        getString(R.string.login_sign_in)
                    } else {
                        getString(R.string.profile_logout)
                    }
                    val userLang = user.preferences?.language
                    if (!userLang.isNullOrEmpty()) {
                        val prefs = requireContext()
                            .getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                        val currentLang = prefs.getString("app_language", null)

                        if (currentLang != userLang) {
                            // Guardamos el idioma del usuario como idioma de la app
                            prefs.edit {
                                putString("app_language", userLang)
                            }


                            // Si hay un idioma previo, recreamos la Activity
                            if (currentLang != null) {
                                requireActivity().recreate()
                            }
                        }
                    }
                }
            }
        }

        // --------------------- Acciones de botones / filas --------------------

        rowEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_nav_profile_to_editProfileFragment)
        }

        rowFriends.setOnClickListener {
            findNavController().navigate(R.id.action_nav_profile_to_friendsFragment)
        }

        rowPreferences.setOnClickListener {
            findNavController().navigate(R.id.action_nav_profile_to_preferencesFragment)
        }

        rowSync.setOnClickListener {
            findNavController().navigate(R.id.action_nav_profile_to_syncFragment)
        }

        btnLogout.setOnClickListener {
            vm.logout(requireContext()) {
                goToAuth()
            }
        }
    }

    /**
     * Navega a la pantalla de autenticación.
     */
    private fun goToAuth() {
        val intent = Intent(requireContext(), AuthActivity::class.java).apply {
            // Limpiamos el backstack para que no pueda volver al perfil
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }
}