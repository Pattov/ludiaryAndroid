package com.ludiary.android.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.R
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.LocalUserDataSource
import com.ludiary.android.data.repository.FirestoreProfileRepository
import com.ludiary.android.util.ThemeManager
import com.ludiary.android.viewmodel.ProfileUiState
import com.ludiary.android.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.content.edit

class PreferencesFragment : Fragment(R.layout.form_preferences_profile) {

    // Compartimos el mismo ViewModel que el resto de fragments de perfil
    private val vm: ProfileViewModel by activityViewModels {

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

    private lateinit var spinnerLanguage: Spinner
    private lateinit var spinnerTheme: Spinner
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinnerLanguage = view.findViewById(R.id.spinnerLanguage)
        spinnerTheme = view.findViewById(R.id.spinnerTheme)
        btnSave = view.findViewById(R.id.btnSavePreferences)
        btnCancel = view.findViewById(R.id.btnCancelPreferences)

        setupSpinners()
        observeState()
        setupListeners()
    }

    private fun setupSpinners() {

        val languages = listOf("es", "en")
        val themes = listOf("system", "light", "dark")

        spinnerLanguage.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            languages
        )

        spinnerTheme.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            themes
        )
    }

    /**
     * Configura el observador del estado.
     */
    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            vm.ui.collectLatest { state: ProfileUiState ->
                val user = state.user ?: return@collectLatest

                val lang = user.preferences?.language ?: "es"
                val theme = user.preferences?.theme ?: "system"

                setSpinnerSelection(spinnerLanguage, lang)
                setSpinnerSelection(spinnerTheme, theme)
            }
        }
    }

    /**
     * Establece la selección del Spinner.
     * @param spinner Spinner a configurar
     * @param value Valor a seleccionar
     */
    private fun setSpinnerSelection(spinner: Spinner, value: String) {
        val adapter = spinner.adapter ?: return
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i)?.toString() == value) {
                spinner.setSelection(i)
                break
            }
        }
    }

    /**
     * Configura los listeners de los botones.
     */
    private fun setupListeners() {
        btnSave.setOnClickListener {
            val selectedLanguage = spinnerLanguage.selectedItem?.toString()
            val selectedTheme = spinnerTheme.selectedItem?.toString()

            viewLifecycleOwner.lifecycleScope.launch {
                // Actualizar en Firestore + Room (vía ViewModel/Repository)
                vm.updatePreferences(
                    language = selectedLanguage,
                    theme = selectedTheme
                )

                // Guardar también en SharedPreferences para que MainActivity lo lea
                val prefs = requireContext()
                    .getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

                prefs.edit {
                    putString("app_language", selectedLanguage) // "es", "en"...
                    .putString("app_theme", selectedTheme)       // "system", "light", "dark"
                }

                // Aplicar el tema inmediatamente
                ThemeManager.applyTheme(selectedTheme)

                // Recrear la Activity para que se aplique el idioma en toda la UI
                requireActivity().recreate()
            }
        }

        btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }
}
