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
import com.ludiary.android.R
import com.ludiary.android.data.repository.FakeUserGamesRepository
import com.ludiary.android.viewmodel.EditUserGameEvent
import com.ludiary.android.viewmodel.EditUserGameViewModel
import com.ludiary.android.viewmodel.EditUserGameViewModelFactory
import kotlinx.coroutines.launch

class EditUserGameFragment : Fragment(R.layout.fragment_edit_user_game) {

    private val viewModel: EditUserGameViewModel by viewModels{
        EditUserGameViewModelFactory(
            uid = "TEST_UID",
            repository = FakeUserGamesRepository
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val gameId = arguments?.getString("gameId")
        val isEditing = gameId != null

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

        btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is EditUserGameEvent.ShowError -> {
                        Toast.makeText(
                            requireContext(),
                            event.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is EditUserGameEvent.CloseScreen -> {
                        Toast.makeText(
                            requireContext(),
                            event.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }
}