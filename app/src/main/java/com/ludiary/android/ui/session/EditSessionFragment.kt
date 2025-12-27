package com.ludiary.android.ui.session

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.ludiary.android.R
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.SessionWithPlayers
import com.ludiary.android.viewmodel.EditSessionsViewModel
import com.ludiary.android.viewmodel.PlayerDraft
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditSessionFragment : Fragment(R.layout.form_edit_session) {

    private lateinit var vm: EditSessionsViewModel

    private lateinit var inputGame: TextInputEditText
    private lateinit var inputDate: TextInputEditText
    private lateinit var inputRating: TextInputEditText
    private lateinit var inputNotes: TextInputEditText
    private lateinit var playersContainer: LinearLayout

    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = LudiaryDatabase.getInstance(requireContext().applicationContext)
        val auth = FirebaseAuth.getInstance()
        vm = EditSessionsViewModel(db, auth)

        val sessionId = arguments?.getString("sessionId")

        val titleView: TextView = view.findViewById(R.id.title)
        titleView.text = if (sessionId.isNullOrBlank())
            getString(R.string.edit_session_title_create)
        else
            getString(R.string.edit_session_title_edit)

        inputGame = view.findViewById(R.id.inputGame)
        inputDate = view.findViewById(R.id.inputDate)
        inputRating = view.findViewById(R.id.inputRating)
        inputNotes = view.findViewById(R.id.inputNotes)
        playersContainer = view.findViewById(R.id.playersContainer)

        view.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            findNavController().popBackStack()
        }

        inputDate.setOnClickListener { openDatePicker() }

        view.findViewById<MaterialButton>(R.id.btnAddPlayer).setOnClickListener {
            addPlayerRow()
        }

        view.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val gameTitle = inputGame.text?.toString()?.trim().orEmpty()
            if (gameTitle.isBlank()) {
                inputGame.error = getString(R.string.edit_session_game_hint_required)
                return@setOnClickListener
            }

            val dateText = inputDate.text?.toString()?.trim().orEmpty()
            val playedAtMillis = df.parse(dateText)?.time ?: return@setOnClickListener

            val rating = inputRating.text?.toString()?.trim()?.toIntOrNull()
            val notes = inputNotes.text?.toString()?.trim().takeIf { !it.isNullOrBlank() }

            val players = collectPlayers().filter { it.name.isNotBlank() }
            if (players.isEmpty()) return@setOnClickListener

            vm.saveSession(
                sessionId = sessionId, // null => crear, no-null => editar
                gameTitle = gameTitle,
                playedAtMillis = playedAtMillis,
                rating = rating,
                notes = notes,
                players = players
            )

            findNavController().popBackStack()
        }

        if (!sessionId.isNullOrBlank()) {
            vm.loadSession(sessionId) { fillForm(it) }
        }
    }

    private fun fillForm(data: SessionWithPlayers) {
        val s = data.session

        inputGame.setText(s.gameTitle)
        inputDate.setText(df.format(Date(s.playedAt)))
        inputRating.setText(s.overallRating?.toString().orEmpty())
        inputNotes.setText(s.notes.orEmpty())

        playersContainer.removeAllViews()

        data.players
            .sortedBy { it.sortOrder }
            .forEach { p ->
                addPlayerRow(
                    name = p.displayName,
                    score = p.score,
                    isWinner = p.isWinner
                )
            }
    }

    private fun openDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                inputDate.setText(df.format(picked.time))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun addPlayerRow(
        name: String = "",
        score: Int? = null,
        isWinner: Boolean = false
    ) {
        val row = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_session_player_form, playersContainer, false)

        val inputPlayerName: TextInputEditText = row.findViewById(R.id.inputPlayerName)
        val inputPlayerScore: TextInputEditText = row.findViewById(R.id.inputPlayerScore)
        val btnWinner: ImageButton = row.findViewById(R.id.btnWinner)
        val btnRemove: ImageButton = row.findViewById(R.id.btnRemovePlayer)

        inputPlayerName.setText(name)
        inputPlayerScore.setText(score?.toString().orEmpty())

        btnWinner.tag = isWinner
        renderWinnerIcon(btnWinner, isWinner)

        btnWinner.setOnClickListener {
            val next = !((btnWinner.tag as? Boolean) ?: false)
            btnWinner.tag = next
            renderWinnerIcon(btnWinner, next)
        }

        btnRemove.setOnClickListener { playersContainer.removeView(row) }

        playersContainer.addView(row)
    }

    private fun renderWinnerIcon(btn: ImageButton, isWinner: Boolean) {
        btn.setImageResource(
            if (isWinner) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )
    }

    private fun collectPlayers(): List<PlayerDraft> {
        val out = mutableListOf<PlayerDraft>()
        for (i in 0 until playersContainer.childCount) {
            val row = playersContainer.getChildAt(i)

            val name = row.findViewById<TextInputEditText>(R.id.inputPlayerName)
                .text?.toString()?.trim().orEmpty()

            val scoreText = row.findViewById<TextInputEditText>(R.id.inputPlayerScore)
                .text?.toString()?.trim()
            val score = scoreText?.toIntOrNull()

            val isWinner = (row.findViewById<ImageButton>(R.id.btnWinner).tag as? Boolean) ?: false

            out += PlayerDraft(name = name, score = score, isWinner = isWinner)
        }
        return out
    }
}
