package com.ludiary.android.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.ludiary.android.R
import com.ludiary.android.util.ThemeManager
import com.ludiary.android.viewmodel.ProfileUiState
import com.ludiary.android.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.content.edit
import com.google.android.material.appbar.MaterialToolbar
import com.ludiary.android.viewmodel.ProfileViewModelFactory

class PreferencesFragment : Fragment(R.layout.form_preferences_profile) {

    // Compartimos el mismo ViewModel que el resto de fragments de perfil
    private val vm: ProfileViewModel by activityViewModels {
        ProfileViewModelFactory(requireContext())
    }

    private lateinit var spinnerLanguage: Spinner
    private lateinit var spinnerTheme: Spinner
    private lateinit var languageValues: Array<String>
    private lateinit var themeValues: Array<String>
    private lateinit var etMentionUserPrefix: com.google.android.material.textfield.TextInputEditText
    private lateinit var etMentionGroupPrefix: com.google.android.material.textfield.TextInputEditText
    private lateinit var tilMentionUserPrefix: com.google.android.material.textfield.TextInputLayout
    private lateinit var tilMentionGroupPrefix: com.google.android.material.textfield.TextInputLayout

    private lateinit var btnSave: Button
    private lateinit var topAppBar: MaterialToolbar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinnerLanguage = view.findViewById(R.id.spinnerLanguage)
        spinnerTheme = view.findViewById(R.id.spinnerTheme)
        etMentionUserPrefix = view.findViewById(R.id.etMentionUserPrefix)
        etMentionGroupPrefix = view.findViewById(R.id.etMentionGroupPrefix)
        tilMentionUserPrefix = view.findViewById(R.id.tilMentionUserPrefix)
        tilMentionGroupPrefix = view.findViewById(R.id.tilMentionGroupPrefix)
        btnSave = view.findViewById(R.id.btnSavePreferences)
        topAppBar = view.findViewById(R.id.topAppBar)

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
                val mentionUser = user.preferences?.mentionUserPrefix ?: "@"
                val mentionGroup = user.preferences?.mentionGroupPrefix ?: "#"

                setSpinnerSelectionByValue(spinnerLanguage, languageValues, lang)
                setSpinnerSelectionByValue(spinnerTheme, themeValues, theme)
                etMentionUserPrefix.setText(mentionUser)
                etMentionGroupPrefix.setText(mentionGroup)
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
            tilMentionUserPrefix.error = null
            tilMentionGroupPrefix.error = null

            val selectedLanguage = languageValues[spinnerLanguage.selectedItemPosition]
            val selectedTheme = themeValues[spinnerTheme.selectedItemPosition]

            val userPrefixRaw = etMentionUserPrefix.text?.toString().orEmpty().trim()
            val groupPrefixRaw = etMentionGroupPrefix.text?.toString().orEmpty().trim()

            val userPrefix = (if (userPrefixRaw.isBlank()) "@" else userPrefixRaw)
            val groupPrefix = (if (groupPrefixRaw.isBlank()) "#" else groupPrefixRaw)

            // Validaciones mínimas
            fun isValidPrefix(s: String) = s.length in 1..2 && !s.any { it.isWhitespace() }

            var hasError = false
            if (!isValidPrefix(userPrefix)) {
                tilMentionUserPrefix.error = "Inválido (1-2 chars, sin espacios)"
                hasError = true
            }
            if (!isValidPrefix(groupPrefix)) {
                tilMentionGroupPrefix.error = "Inválido (1-2 chars, sin espacios)"
                hasError = true
            }

            if (hasError) return@setOnClickListener

            if (userPrefix == groupPrefix) {
                val errorMsg = getString(R.string.error_prefs_prefixes_same)
                tilMentionUserPrefix.error = errorMsg
                tilMentionGroupPrefix.error = errorMsg
                com.google.android.material.snackbar.Snackbar.make(
                    requireView(),
                    errorMsg,
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                vm.updatePreferences(
                    language = selectedLanguage,
                    theme = selectedTheme,
                    mentionUserPrefix = userPrefix,
                    mentionGroupPrefix = groupPrefix
                )

                val prefs = requireContext().getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                prefs.edit {
                    putString("app_language", selectedLanguage)
                    putString("app_theme", selectedTheme)
                    putString("mention_user_prefix", userPrefix)
                    putString("mention_group_prefix", groupPrefix)
                }

                ThemeManager.applyTheme(selectedTheme)
                requireActivity().recreate()
            }
        }

        topAppBar.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
}