package com.ludiary.android.ui.library

import android.os.Bundle
import android.view.View
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.ludiary.android.R
import com.ludiary.android.data.local.LocalUserDataSource
import com.ludiary.android.data.local.LocalUserGamesDataSource
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.repository.auth.AuthRepositoryProvider
import com.ludiary.android.data.repository.library.UserGamesRepository
import com.ludiary.android.data.repository.library.UserGamesRepositoryLocal
import com.ludiary.android.util.ResourceProvider
import com.ludiary.android.viewmodel.EditUserGameEvent
import com.ludiary.android.viewmodel.EditUserGameViewModel
import com.ludiary.android.viewmodel.EditUserGameViewModelFactory
import kotlinx.coroutines.launch

/**
 * Fragmento para añadir o editar un juego del usuario.
 */
class EditUserGameFragment : Fragment(R.layout.form_user_game) {

    /**
     * ViewModel para gestionar la lógica del fragmento.
     */
    private val viewModel: EditUserGameViewModel by viewModels {
        val appContext = requireContext().applicationContext
        val db = LudiaryDatabase.getInstance(appContext)

        val local = LocalUserGamesDataSource(db.userGameDao())
        val repository: UserGamesRepository =
            UserGamesRepositoryLocal(local)

        val localUser = LocalUserDataSource(db)
        val authRepo = AuthRepositoryProvider.provide(
            context = appContext,
            localUserDataSource = localUser,
            resourceProvider = ResourceProvider(requireContext())
        )

        EditUserGameViewModelFactory(
            uid = authRepo.currentUser?.uid ?: "LOCAL_USER",
            repository = repository
        )
    }


    /**
     * Llamada al crear la vista del fragmento.
     * @param view La vista del fragmento.
     * @param savedInstanceState El estado guardado de la instancia.
     */
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Saber si estamos editando o creando un juego nuevo.
        val gameId = arguments?.getString("gameId")
        val isEditing = !gameId.isNullOrBlank()

        // Views
        val topAppBar = view.findViewById<MaterialToolbar>(R.id.topAppBar)

        val inputTitle = view.findViewById<TextInputEditText>(R.id.inputTitle)
        val inputRating = view.findViewById<TextInputEditText>(R.id.inputRating)
        val inputLanguage = view.findViewById<TextInputEditText>(R.id.inputLanguage)
        val inputEdition = view.findViewById<TextInputEditText>(R.id.inputEdition)
        val inputNotes = view.findViewById<TextInputEditText>(R.id.inputNotes)

        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)

        topAppBar.title = if (isEditing) {
            getString(R.string.edit_game_edit_title_form)
        } else {
            getString(R.string.edit_game_title_default)
        }

        topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        if (isEditing) {
            viewModel.loadGame(gameId)
        }

        btnSave.setOnClickListener {
            val title = inputTitle.text?.toString()?.trim().orEmpty()
            val rating = inputRating.text?.toString()?.toFloatOrNull()
            val language = inputLanguage.text?.toString()?.trim().orEmpty()
            val edition = inputEdition.text?.toString()?.trim().orEmpty()
            val notes = inputNotes.text?.toString()?.trim().orEmpty()

            viewModel.onSaveClicked(
                gameId = gameId, // null si es crear
                title = title,
                rating = rating,
                language = language,
                edition = edition,
                notes = notes
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {

                    is EditUserGameEvent.ShowError -> {
                        Snackbar.make(
                            requireView(),
                            event.message,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }

                    is EditUserGameEvent.CloseScreen -> {
                        Snackbar.make(
                            requireView(),
                            event.message,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        findNavController().navigateUp()
                    }

                    is EditUserGameEvent.FillForm -> {
                        inputTitle.setText(event.game.titleSnapshot)
                        inputRating.setText(event.game.personalRating?.toString().orEmpty())
                        inputLanguage.setText(event.game.language.orEmpty())
                        inputEdition.setText(event.game.edition.orEmpty())
                        inputNotes.setText(event.game.notes.orEmpty())
                    }
                }
            }
        }
    }

}