package com.ludiary.android.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.R
import com.ludiary.android.data.local.LocalUserDataSource
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.model.User
import com.ludiary.android.data.repository.FirestoreProfileRepository
import com.ludiary.android.viewmodel.ProfileUiState
import com.ludiary.android.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class EditProfileFragment : Fragment(R.layout.form_edit_profile) {

    private val vm: ProfileViewModel by viewModels {

        val db = LudiaryDatabase.getInstance(requireContext().applicationContext)
        val localDS = LocalUserDataSource(db)

        val repo = FirestoreProfileRepository(
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance(),
            localDS
        )

        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(repo) as T
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inputAlias = view.findViewById<TextInputEditText>(R.id.inputAlias)
        val inputEmail = view.findViewById<TextInputEditText>(R.id.inputEmail)
        val inputCreatedAt = view.findViewById<TextInputEditText>(R.id.inputCreatedAt)
        val inputUid = view.findViewById<TextInputEditText>(R.id.inputUid)
        val tvRoleValue = view.findViewById<TextView>(R.id.tvRoleValue)

        val btnCancel = view.findViewById<Button>(R.id.btnCancelEdit)
        val btnSave = view.findViewById<Button>(R.id.btnSaveEdit)

        viewLifecycleOwner.lifecycleScope.launch {
            vm.ui.collectLatest { st: ProfileUiState ->
                val user = st.user ?: return@collectLatest

                bindUserToForm(user, inputAlias, inputEmail, inputCreatedAt, inputUid, tvRoleValue)

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

    private fun bindUserToForm(
        user: User,
        inputAlias: TextInputEditText,
        inputEmail: TextInputEditText,
        inputCreatedAt: TextInputEditText,
        inputUid: TextInputEditText,
        tvRoleValue: TextView
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

        tvRoleValue.text = if (user.isAdmin) {
            getString(R.string.profile_admin_yes)
        } else {
            getString(R.string.profile_admin_no)
        }

        // Invitado -> no permitimos cambiar alias ni email
        if (user.isAnonymous) {
            inputAlias.isEnabled = false
            inputEmail.isEnabled = false
        }
    }
}
