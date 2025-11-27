package com.ludiary.android.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.ludiary.android.R

class EditUserGameFragment : Fragment(R.layout.fragment_edit_user_game) {

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

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
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "El título es obligatorio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(requireContext(), "Juego guardado", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()

        }
    }
}