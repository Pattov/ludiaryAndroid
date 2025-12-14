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
    private lateinit var languageValues: Array<String>
    private lateinit var themeValues: Array<String>
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

        val languageEntries = resources.getStringArray(R.array.language_entries)
        languageValues = resources.getStringArray(R.array.language_values)

        spinnerLanguage.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            languageEntries
        )

        val themeEntries = resources.getStringArray(R.array.theme_entries)
        themeValues = resources.getStringArray(R.array.theme_values)

        spinnerTheme.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            themeEntries
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

                setSpinnerSelectionByValue(spinnerLanguage, languageValues, lang)
                setSpinnerSelectionByValue(spinnerTheme, themeValues, theme)
            }
        }
    }

    /**
     * Selecciona el índice del spinner según un array de valores internos.
     */
    private fun setSpinnerSelectionByValue(spinner: Spinner, values: Array<String>, value: String) {
        val idx = values.indexOf(value).takeIf { it >= 0 } ?: 0
        spinner.setSelection(idx)
    }

    /**
     * Configura los listeners de los botones.
     */
    private fun setupListeners() {
        btnSave.setOnClickListener {
            val selectedLanguage = languageValues[spinnerLanguage.selectedItemPosition]
            val selectedTheme = themeValues[spinnerTheme.selectedItemPosition]

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
                    putString("app_language", selectedLanguage)
                    putString("app_theme", selectedTheme)
                }

                // Aplicar el tema inmediatamente
                ThemeManager.applyTheme(selectedTheme)

                // Recrear la Activity para que se aplique el idioma en toda la UI
                requireActivity().recreate()
            }
        }

        btnCancel.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
}
