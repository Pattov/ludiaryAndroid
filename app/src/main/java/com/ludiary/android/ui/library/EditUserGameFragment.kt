package com.ludiary.android.ui.library

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.R
import com.ludiary.android.data.repository.FirestoreUserGamesRepository
import com.ludiary.android.viewmodel.EditUserGameEvent
import com.ludiary.android.viewmodel.EditUserGameViewModel
import com.ludiary.android.viewmodel.EditUserGameViewModelFactory
import kotlinx.coroutines.launch

/**
 * Fragmento para añadir o editar un juego del usuario.
 */
class EditUserGameFragment : Fragment(R.layout.fragment_edit_user_game) {

    /**
     * ViewModel para gestionar la lógica del fragmento.
     */
    private val viewModel: EditUserGameViewModel by viewModels{
        EditUserGameViewModelFactory(
            uid = FirebaseAuth.getInstance().currentUser!!.uid,
            repository = FirestoreUserGamesRepository(
                FirebaseFirestore.getInstance()
            )
        )
    }

    /**
     * Llamada al crear la vista del fragmento.
     */
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        //Saber si estamos editando o creando un juego nuevo.
        val gameId = arguments?.getString("gameId")
        val isEditing = gameId != null

        // Título dinámico de la pantalla
        val titleScreen = view.findViewById<TextView>(R.id.titleEditGame)

        titleScreen.text = if (isEditing) {
            "Editar juego"
        } else {
            "Añadir juego a tu ludoteca"
        }

        val inputTitle = view.findViewById<TextInputEditText>(R.id.inputTitle)
        val inputRating = view.findViewById<TextInputEditText>(R.id.inputRating)
        val inputLanguage = view.findViewById<TextInputEditText>(R.id.inputLanguage)
        val inputEdition = view.findViewById<TextInputEditText>(R.id.inputEdition)
        val inputNotes = view.findViewById<TextInputEditText>(R.id.inputNotes)

        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)

        // Si estamos editando, rellenar el formulario con los datos del juego
        if (gameId != null){
            viewModel.loadGame(gameId)
        }

        // Botón cancelar → volver atrás sin guardar
        btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        // Botón guardar → recoger datos de la UI y enviarlos al ViewModel
        btnSave.setOnClickListener {
            val title = inputTitle.text?.toString()?.trim().orEmpty()
            val rating = inputRating.text?.toString()?.toFloatOrNull()
            val language = inputLanguage.text?.toString()?.trim().orEmpty()
            val edition = inputEdition.text?.toString()?.trim().orEmpty()
            val notes = inputNotes.text?.toString()?.trim().orEmpty()

            val gameId = arguments?.getString("gameId")

            viewModel.onSaveClicked(
                gameId = gameId,
                title = title,
                rating = rating,
                language = language,
                edition = edition,
                notes = notes
            )
        }

        // Observar los eventos del ViewModel y actualizar la UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {

                    /** Mostrar un error */
                    is EditUserGameEvent.ShowError -> {
                        Toast.makeText(
                            requireContext(),
                            event.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    /** Cerrar pantalla tras añadir o editar correctamente */
                    is EditUserGameEvent.CloseScreen -> {
                        Toast.makeText(
                            requireContext(),
                            event.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().navigateUp()
                    }

                    /** Rellenar el formulario con los datos del juego (modo edición) */
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