package com.ludiary.android.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.R
import com.ludiary.android.auth.AuthActivity
import com.ludiary.android.data.repository.FirestoreProfileRepository
import com.ludiary.android.viewmodel.ProfileUiState
import com.ludiary.android.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment (R.layout.fragment_profile){
    private val vm: ProfileViewModel by viewModels{
        val repo = FirestoreProfileRepository(
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance(),
            com.ludiary.android.data.local.LocalUserDataSource(requireContext().applicationContext)
        )
        object : androidx.lifecycle.ViewModelProvider.Factory{
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(repo) as T
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        val tvUid = view.findViewById<TextView>(R.id.tvUid)
        val tvCreatedAt = view.findViewById<TextView>(R.id.tvCreatedAt)
        val tvLang = view.findViewById<TextView>(R.id.tvLanguage)
        val tvTheme = view.findViewById<TextView>(R.id.tvTheme)
        val etName = view.findViewById<EditText>(R.id.etDisplayName)

        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        viewLifecycleOwner.lifecycleScope.launch{
            vm.ui.collectLatest { st: ProfileUiState ->
                val user = st.user
                if (user != null){
                    // Email
                    val emailText = when {
                        user.email != null ->
                            getString(R.string.profile_email, user.email)
                        user.isAnonymous ->
                            getString(R.string.profile_email_guest)
                        else ->
                            getString(R.string.profile_email_unknown)
                    }
                    tvEmail.text = emailText

                    // UID
                    if (user.isAdmin) {
                        tvUid.visibility = View.VISIBLE
                        tvUid.text = getString(R.string.profile_uid, user.uid)
                    } else {
                        tvUid.visibility = View.GONE
                    }

                    // Nombre visible
                    val displayName = user.displayName.orEmpty()
                    if (etName.text.toString() != displayName) {
                        etName.setText(displayName)
                    }

                    // Fecha de registro
                    val created = user.createdAt
                    if(created != null){
                        val dateStr = java.text.DateFormat
                            .getDateInstance()
                            .format(java.util.Date(created))
                        tvCreatedAt.text =
                            getString(R.string.profile_created_at, dateStr)
                    } else {
                        tvCreatedAt.text =
                            getString(R.string.profile_created_at)
                    }

                    // Idioma y tema
                    val lang = user.preferences?.language ?: "es"
                    val theme = user.preferences?.theme ?: "system"

                    tvLang.text = getString(R.string.profile_language, lang)
                    tvTheme.text = getString(R.string.profile_theme, theme)
                }
                btnSave.isEnabled = !st.loading
            }
        }

        btnSave.setOnClickListener {
            vm.save(etName.text?.toString())
        }

        btnLogout.setOnClickListener {
            vm.logout()
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finish()
        }
    }
}