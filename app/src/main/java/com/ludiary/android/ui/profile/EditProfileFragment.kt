package com.ludiary.android.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.ludiary.android.R
import com.ludiary.android.data.model.User
import com.ludiary.android.viewmodel.ProfileUiState
import com.ludiary.android.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import com.google.android.material.textfield.TextInputLayout
import com.ludiary.android.viewmodel.ProfileViewModelFactory

/**
 * Fragmento para editar el perfil del usuario.
 * @property vm Instancia del ViewModel
 * @return Instancia del ViewModel
 */
class EditProfileFragment : Fragment(R.layout.form_edit_profile) {

    /**
     * ViewModel inicializado mediante un Factory que construye el repositorio con Firebase + Room.
     */
    private val vm: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(requireContext())
    }

    /**
     * Configura la interfaz del perfil una vez que la vista ha sido creada.
     * @param view Vista del fragmento
     * @param savedInstanceState Estado de la instancia
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inputAlias = view.findViewById<TextInputEditText>(R.id.inputAlias)
        val inputEmail = view.findViewById<TextInputEditText>(R.id.inputEmail)
        val inputCreatedAt = view.findViewById<TextInputEditText>(R.id.inputCreatedAt)
        val inputUid = view.findViewById<TextInputEditText>(R.id.inputUid)
        val inputLayoutUid = view.findViewById<TextInputLayout>(R.id.inputLayoutUid)

        val btnCancel = view.findViewById<Button>(R.id.btnCancelEdit)
        val btnSave = view.findViewById<Button>(R.id.btnSaveEdit)

        viewLifecycleOwner.lifecycleScope.launch {
            vm.ui.collectLatest { st: ProfileUiState ->
                val user = st.user ?: return@collectLatest

                bindUserToForm(user, inputAlias, inputEmail, inputCreatedAt, inputUid)

                // Mostrar / ocultar UID solo para admins
                if (user.isAdmin) {
                    inputLayoutUid.visibility = View.VISIBLE
                    inputUid.setText(user.uid)
                } else {
                    inputLayoutUid.visibility = View.GONE
                    inputUid.text = null
                }

                btnSave.isEnabled = !st.loading
            }
        }

        btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        btnSave.setOnClickListener {
            val newAlias = inputAlias.text?.toString()?.trim()

            val currentUser = vm.ui.value.user
            // Invitado: no dejamos editar nada
            if (currentUser?.isAnonymous == true) {
                findNavController().navigateUp()
                return@setOnClickListener
            }

            vm.save(newAlias)
            findNavController().navigateUp()
        }
    }

    /**
     * Rellena el formulario con los datos del usuario.
     * @param user Usuario a mostrar
     * @param inputAlias Input para el alias
     * @param inputEmail Input para el email
     * @param inputCreatedAt Input para la fecha de creación
     * @param inputUid Input para el UID
     */
    private fun bindUserToForm(
        user: User,
        inputAlias: TextInputEditText,
        inputEmail: TextInputEditText,
        inputCreatedAt: TextInputEditText,
        inputUid: TextInputEditText
    ) {
        inputAlias.setText(user.displayName.orEmpty())
        inputEmail.setText(user.email.orEmpty())

        val created = user.createdAt
        if (created != null) {
            val dateStr = DateFormat.getDateInstance().format(Date(created))
            inputCreatedAt.setText(dateStr)
        } else {
            inputCreatedAt.setText("-")
        }

        inputUid.setText(user.uid)

        // Invitado → no permitimos cambiar alias ni email
        if (user.isAnonymous) {
            inputAlias.isEnabled = false
            inputEmail.isEnabled = false
        }
    }
}
