package com.ludiary.android.ui.sessions

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.ludiary.android.R
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.SessionWithPlayers
import com.ludiary.android.viewmodel.EditSessionsViewModel
import com.ludiary.android.viewmodel.EditSessionsViewModelFactory
import com.ludiary.android.viewmodel.PlayerDraft
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Fragmento para crear y editar una partida.
 * @param R.layout.form_edit_session layout de este fragmento.
 */
class EditSessionFragment : Fragment(R.layout.form_edit_session) {

    private lateinit var vm: EditSessionsViewModel

    private lateinit var inputGame: TextInputEditText
    private lateinit var inputDate: TextInputEditText
    private lateinit var inputRating: TextInputEditText
    private lateinit var inputNotes: TextInputEditText
    private lateinit var playersContainer: LinearLayout

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private val sessionId: String? by lazy {
        arguments?.getString(ARG_SESSION_ID)
    }

    /**
     * Se llama cuando la vista del fragmento ha sido creada.
     * @param view Vista del fragmento.
     * @param savedInstanceState Estado de la instancia guardada.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        bindViews(view)
        setupStaticUi(view)
        setupListeners(view)

        // Si estamos editando, cargamos los datos
        sessionId?.takeIf { it.isNotBlank() }?.let { id ->
            vm.loadSession(id) { fillForm(it) }
        }
    }

    /**
     * Crea el ViewModel.
     */
    private fun setupViewModel() {
        val appContext = requireContext().applicationContext
        val db = LudiaryDatabase.getInstance(appContext)
        val auth = FirebaseAuth.getInstance()

        val factory = EditSessionsViewModelFactory(db, auth)
        vm = ViewModelProvider(this, factory)[EditSessionsViewModel::class.java]
    }

    /**
     * Enlaza las vistas del fragmento.
     * @param view Vista del fragmento.
     */
    private fun bindViews(view: View) {
        inputGame = view.findViewById(R.id.inputGame)
        inputDate = view.findViewById(R.id.inputDate)
        inputRating = view.findViewById(R.id.inputRating)
        inputNotes = view.findViewById(R.id.inputNotes)
        playersContainer = view.findViewById(R.id.playersContainer)
    }

    /**
     * Configura la UI.
     * @param view Vista del fragmento.
     */
    private fun setupStaticUi(view: View) {
        val topAppBar: com.google.android.material.appbar.MaterialToolbar = view.findViewById(R.id.topAppBar)

        val title = if (sessionId.isNullOrBlank()) {
            getString(R.string.edit_session_title_create)
        } else {
            getString(R.string.edit_session_title_edit)
        }

        topAppBar.title = title
    }


    /**
     * Configura los listeners de la UI.
     * @param view Vista del fragmento.
     */
    private fun setupListeners(view: View) {
        val topAppBar: com.google.android.material.appbar.MaterialToolbar =
            view.findViewById(R.id.topAppBar)

        topAppBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        inputDate.setOnClickListener { openDatePicker() }

        view.findViewById<MaterialButton>(R.id.btnAddPlayer).setOnClickListener {
            addPlayerRow()
        }

        view.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            onSaveClicked()
        }
    }


    /**
     * Recoge los datos del formulario, valida lo mínimo y delega el guardado al ViewModel.
     */
    private fun onSaveClicked() {
        val gameTitle = inputGame.text?.toString()?.trim().orEmpty()
        if (gameTitle.isBlank()) {
            inputGame.error = getString(R.string.edit_session_game_hint_required)
            return
        }

        val playedAtMillis = parseDateMillis(inputDate.text?.toString())
        if (playedAtMillis == null) {
            inputDate.error = getString(R.string.edit_session_date_hint_required)
            return
        }

        val rating = inputRating.text?.toString()?.trim()?.toIntOrNull()
        val notes = inputNotes.text?.toString()?.trim().takeIf { !it.isNullOrBlank() }

        val players = collectPlayers()
            .map { it.copy(name = it.name.trim()) }
            .filter { it.name.isNotBlank() }

        if (players.isEmpty()) {
            return
        }

        vm.saveSession(
            sessionId = sessionId,
            gameTitle = gameTitle,
            playedAtMillis = playedAtMillis,
            rating = rating,
            notes = notes,
            players = players
        )

        findNavController().popBackStack()
    }

    /**
     * Convierte el texto de la fecha en millis.
     * @param text Texto de la fecha.
     * @return Fecha en millis o null.
     */
    private fun parseDateMillis(text: String?): Long? {
        val value = text?.trim().orEmpty()
        if (value.isBlank()) return null
        return runCatching { dateFormatter.parse(value)?.time }.getOrNull()
    }

    /**
     * Rellena el formulario con los datos de la partida.
     * @param data Datos de la partida.
     */
    private fun fillForm(data: SessionWithPlayers) {
        val s = data.session

        inputGame.setText(s.gameTitle)
        inputDate.setText(dateFormatter.format(Date(s.playedAt)))
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

    /**
     * Abre el selector de fecha.
     */
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
                inputDate.setText(dateFormatter.format(picked.time))
                inputDate.error = null
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * Añade una fila de formulario de jugador.
     * @param name Nombre del jugador.
     * @param score Puntuación del jugador.
     * @param isWinner Indica si el jugador es ganador.
     */
    private fun addPlayerRow(
        name: String = "",
        score: Int? = null,
        isWinner: Boolean = false
    ) {
        val row = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_session_player_form, playersContainer, false)

        val inputPlayerName: TextInputEditText = row.findViewById(R.id.inputPlayerName)
        val inputPlayerScore: TextInputEditText = row.findViewById(R.id.inputPlayerScore)

        val btnWinner: MaterialButton =
            row.findViewById(R.id.btnWinner)

        val btnRemove: View = row.findViewById(R.id.btnRemovePlayer)

        inputPlayerName.setText(name)
        inputPlayerScore.setText(score?.toString().orEmpty())

        setWinner(btnWinner, isWinner)

        btnWinner.setOnClickListener {
            val next = !((btnWinner.tag as? Boolean) ?: false)
            setWinner(btnWinner, next)
        }

        btnRemove.setOnClickListener { playersContainer.removeView(row) }

        playersContainer.addView(row)
    }

    /**
     * Establece el estado del botón de ganador.
     * @param btn Botón de ganador.
     * @param isWinner Indica si el jugador es ganador.
     */
    private fun setWinner(btn: MaterialButton, isWinner: Boolean) {
        btn.tag = isWinner

        val winnerColor = requireContext().getColor(R.color.colorWinner)

        val normalColor = MaterialColors.getColor(
            btn,
            com.google.android.material.R.attr.colorOnSurfaceVariant
        )

        val color = if (isWinner) winnerColor else normalColor

        btn.iconTint = ColorStateList.valueOf(color)
    }

    /**
     * Recoge los datos de los jugadores del formulario.
     * @return Lista de datos de jugadores.
     */
    private fun collectPlayers(): List<PlayerDraft> {
        val out = mutableListOf<PlayerDraft>()

        for (i in 0 until playersContainer.childCount) {
            val row = playersContainer.getChildAt(i)

            val name = row.findViewById<TextInputEditText>(R.id.inputPlayerName)
                .text?.toString().orEmpty()

            val scoreText = row.findViewById<TextInputEditText>(R.id.inputPlayerScore)
                .text?.toString()?.trim()
            val score = scoreText?.toIntOrNull()

            val isWinner = (row.findViewById<MaterialButton>(R.id.btnWinner).tag as? Boolean) ?: false

            out += PlayerDraft(name = name, score = score, isWinner = isWinner)
        }

        return out
    }

    /**
     * Identificador único de la partida.
     * @param ARG_SESSION_ID Identificador único de la partida.
     */
    companion object {
        private const val ARG_SESSION_ID = "sessionId"
    }
}