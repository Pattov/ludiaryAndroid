package com.ludiary.android.ui.sessions

import android.app.DatePickerDialog
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupWindow
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.R
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.SessionWithPlayers
import com.ludiary.android.data.model.PlayerRefType
import com.ludiary.android.viewmodel.EditSessionsViewModel
import com.ludiary.android.viewmodel.EditSessionsViewModelFactory
import com.ludiary.android.viewmodel.PlayerDraft
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditSessionFragment : Fragment(R.layout.form_edit_session) {

    private lateinit var vm: EditSessionsViewModel

    private lateinit var inputGame: TextInputEditText
    private lateinit var inputDate: TextInputEditText
    private lateinit var inputDuration: TextInputEditText

    private lateinit var inputRating: TextInputEditText
    private lateinit var inputNotes: TextInputEditText
    private lateinit var playersContainer: LinearLayout

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val sessionId: String? by lazy { arguments?.getString(ARG_SESSION_ID) }

    private data class MentionPick(
        val kind: Kind,
        val id: String,
        val displayName: String
    ) {
        enum class Kind { FRIEND, GROUP, GROUP_MEMBER  }
    }

    private data class GroupMember(
        val uid: String,
        val displayName: String
    )

    // --- Popup state (uno a la vez) ---
    private var mentionsPopup: PopupWindow? = null
    private var mentionJob: Job? = null
    private var currentItems: List<MentionPick> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        bindViews(view)
        setupStaticUi(view)
        setupListeners(view)

        sessionId?.takeIf { it.isNotBlank() }?.let { id ->
            vm.loadSession(id) { fillForm(it) }
        } ?: run {
            // Inicializar fila por defecto para el usuario actual
            lifecycleScope.launch {
                val myName = vm.getCurrentUserDisplay() ?: getString(R.string.session_player_you)
                val suffix = getString(R.string.session_player_me_suffix)
                addPlayerRow(name = "$myName $suffix", isCurrentUser = true)
            }
        }
    }

    private fun setupViewModel() {
        val appContext = requireContext().applicationContext
        val db = LudiaryDatabase.getInstance(appContext)
        val auth = FirebaseAuth.getInstance()
        val factory = EditSessionsViewModelFactory(appContext, db, auth)
        vm = ViewModelProvider(this, factory)[EditSessionsViewModel::class.java]
    }

    private fun bindViews(view: View) {
        inputGame = view.findViewById(R.id.inputGame)
        inputDate = view.findViewById(R.id.inputDate)
        inputDuration = view.findViewById(R.id.inputDuration)
        inputRating = view.findViewById(R.id.inputSessionRating)
        inputNotes = view.findViewById(R.id.inputNotes)
        playersContainer = view.findViewById(R.id.playersContainer)
    }

    private fun setupStaticUi(view: View) {
        val topAppBar: com.google.android.material.appbar.MaterialToolbar = view.findViewById(R.id.topAppBar)
        topAppBar.title = if (sessionId.isNullOrBlank()) {
            getString(R.string.edit_session_title_create)
        } else {
            getString(R.string.edit_session_title_edit)
        }
    }

    private fun setupListeners(view: View) {
        val topAppBar: com.google.android.material.appbar.MaterialToolbar = view.findViewById(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener { findNavController().navigateUp() }

        inputDate.setOnClickListener { openDatePicker() }

        view.findViewById<MaterialButton>(R.id.btnAddPlayer).setOnClickListener { addPlayerRow() }
        view.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { onSaveClicked() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mentionJob?.cancel()
        dismissMentionsPopup()
    }

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

        val durationMinutes = inputDuration.text?.toString()?.trim()?.toIntOrNull()
        val rating = inputRating.text?.toString()?.trim()?.toIntOrNull()
        val notes = inputNotes.text?.toString()?.trim().takeIf { !it.isNullOrBlank() }

        val players = collectPlayers()
            .map { it.copy(name = it.name.trim()) }
            .filter { it.name.isNotBlank() }

        if (players.isEmpty()) return

        vm.saveSession(
            sessionId = sessionId,
            gameTitle = gameTitle,
            playedAtMillis = playedAtMillis,
            durationMinutes = durationMinutes,
            rating = rating,
            notes = notes,
            players = players
        )

        findNavController().popBackStack()
    }

    private fun parseDateMillis(text: String?): Long? {
        val value = text?.trim().orEmpty()
        if (value.isBlank()) return null
        return runCatching { dateFormatter.parse(value)?.time }.getOrNull()
    }

    private fun fillForm(data: SessionWithPlayers) {
        val s = data.session
        inputGame.setText(s.gameTitle)
        inputDate.setText(dateFormatter.format(Date(s.playedAt)))
        inputDuration.setText(s.durationMinutes?.toString().orEmpty())
        inputRating.setText(s.overallRating?.toString().orEmpty())
        inputNotes.setText(s.notes.orEmpty())

        playersContainer.removeAllViews()

        data.players.sortedBy { it.sortOrder }.forEach { p ->
            addPlayerRow(
                name = p.displayName,
                score = p.score,
                isWinner = p.isWinner,
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
                inputDate.setText(dateFormatter.format(picked.time))
                inputDate.error = null
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun addPlayerRow(
        name: String = "",
        score: Int? = null,
        isWinner: Boolean = false,
        isCurrentUser: Boolean = false
    ) {
        val row = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_session_player_form, playersContainer, false)

        val inputPlayerName: TextInputEditText = row.findViewById(R.id.inputPlayerName)
        val inputPlayerScore: TextInputEditText = row.findViewById(R.id.inputPlayerScore)

        val btnWinner: MaterialButton = row.findViewById(R.id.btnWinner)
        val btnRemove: View = row.findViewById(R.id.btnRemovePlayer)

        inputPlayerName.setText(name)
        inputPlayerScore.setText(score?.toString().orEmpty())

        if (isCurrentUser) {
            inputPlayerName.isEnabled = false
            btnRemove.visibility = View.INVISIBLE
            // Marcar que es el usuario actual para collectPlayers
            row.tag = MentionPick(MentionPick.Kind.FRIEND, vm.getCurrentUid(), name)
        }

        setWinner(btnWinner, isWinner)

        btnWinner.setOnClickListener {
            val next = !((btnWinner.tag as? Boolean) ?: false)
            setWinner(btnWinner, next)
        }

        btnRemove.setOnClickListener {
            dismissMentionsPopup()
            playersContainer.removeView(row)
        }

        setupMentionsPopup(inputPlayerName, row)

        playersContainer.addView(row)
    }

    private fun setWinner(btn: MaterialButton, isWinner: Boolean) {
        btn.tag = isWinner
        val winnerColor = requireContext().getColor(R.color.colorWinner)
        val normalColor = MaterialColors.getColor(
            btn,
            com.google.android.material.R.attr.colorOnSurfaceVariant
        )
        btn.iconTint = android.content.res.ColorStateList.valueOf(if (isWinner) winnerColor else normalColor)
    }

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

            val pick = row.tag as? MentionPick
            val (refType, refId) = when (pick?.kind) {
                MentionPick.Kind.FRIEND -> PlayerRefType.LUDIARY_USER to pick.id
                MentionPick.Kind.GROUP_MEMBER -> PlayerRefType.GROUP_MEMBER to pick.id
                else -> PlayerRefType.NAME to null
            }

            out += PlayerDraft(
                name = name,
                score = score,
                isWinner = isWinner,
                refType = refType,
                refId = refId
            )
        }

        return out
    }

    // -------------------- MENTIONS POPUP --------------------

    private fun setupMentionsPopup(input: TextInputEditText, row: View) {

        input.doAfterTextChanged { editable ->
            val text = editable?.toString().orEmpty()

            val prefs = requireContext().getSharedPreferences("sync_prefs", android.content.Context.MODE_PRIVATE)
            val userPrefix = prefs.getString("mention_user_prefix", "@") ?: "@"
            val groupPrefix = prefs.getString("mention_group_prefix", "#") ?: "#"

            val isUser = text.startsWith(userPrefix)
            val isGroup = text.startsWith(groupPrefix)

            // Si NO empieza por prefijos => texto normal
            if (!isUser && !isGroup) {
                row.tag = null
                dismissMentionsPopup()
                return@doAfterTextChanged
            }

            // Si el usuario está editando y ya no coincide con la selección previa, limpiamos selección
            val currentPick = row.tag as? MentionPick
            if (currentPick != null && text != currentPick.displayName) {
                row.tag = null
            }

            mentionJob?.cancel()
            mentionJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(150)

                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                    dismissMentionsPopup()
                    return@launch
                }

                val rawQuery = if (isUser) text.removePrefix(userPrefix) else text.removePrefix(groupPrefix)
                val q = rawQuery.trim().lowercase()

                currentItems = if (isUser) {
                    loadFriendSuggestions(uid, q)
                } else {
                    loadGroupSuggestions(uid, q)
                }

                if (currentItems.isEmpty()) {
                    dismissMentionsPopup()
                    return@launch
                }

                showMentionsPopup(anchor = input, row = row, items = currentItems)
            }
        }

        // Si pierde foco, cerramos popup
        input.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) dismissMentionsPopup()
        }
    }

    private fun showMentionsPopup(anchor: View, row: View, items: List<MentionPick>) {

        val popup = mentionsPopup ?: run {
            val listView = ListView(requireContext()).apply {
                divider = null
                isVerticalScrollBarEnabled = false
                isFocusable = false
                isFocusableInTouchMode = false
            }

            val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                radius = resources.getDimension(R.dimen.card_radius_m)
                cardElevation = resources.getDimension(R.dimen.elevation_m)
                setCardBackgroundColor(
                    MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface)
                )
                strokeWidth = resources.getDimensionPixelSize(R.dimen.stroke_s)
                strokeColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline)
                addView(
                    listView,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )
            }

            // 🔑 focusable = false => NO roba el foco al EditText (puedes seguir escribiendo)
            PopupWindow(
                card,
                anchor.width,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                false
            ).apply {
                isOutsideTouchable = true
                elevation = resources.getDimension(R.dimen.elevation_m)

                // Click en sugerencia
                listView.setOnItemClickListener { _, _, position, _ ->
                    val picked = items.getOrNull(position) ?: return@setOnItemClickListener

                    when (picked.kind) {
                        MentionPick.Kind.FRIEND -> {
                            val et = anchor as TextInputEditText
                            et.setText(picked.displayName)
                            et.setSelection(picked.displayName.length)
                            row.tag = picked
                            dismissMentionsPopup()
                        }

                        MentionPick.Kind.GROUP -> {
                            // ✅ Expandir grupo: reemplazar ESTA fila por N filas (miembros)
                            viewLifecycleOwner.lifecycleScope.launch {
                                expandGroupIntoRows(groupId = picked.id, replaceRow = row)
                            }
                            dismissMentionsPopup()
                        }

                        else -> Unit
                    }
                }
            }.also { mentionsPopup = it }
        }

        val listView = ((popup.contentView as com.google.android.material.card.MaterialCardView).getChildAt(0) as ListView)
        listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items.map { it.displayName })

        if (popup.isShowing) popup.dismiss()

        popup.width = anchor.width

        // Colocación: abajo si cabe, si no arriba
        val r = Rect()
        anchor.getGlobalVisibleRect(r)
        val screenH = resources.displayMetrics.heightPixels
        val spaceBelow = screenH - r.bottom
        val itemH = anchor.height.coerceAtLeast(resources.getDimensionPixelSize(R.dimen.space_l))
        val desired = (items.size.coerceAtMost(5) * itemH)

        if (spaceBelow >= desired) {
            popup.showAsDropDown(anchor)
        } else {
            popup.showAsDropDown(anchor, 0, -desired - anchor.height)
        }
    }

    private fun dismissMentionsPopup() {
        mentionsPopup?.dismiss()
    }

    private suspend fun loadFriendSuggestions(uid: String, query: String): List<MentionPick> {
        val snap = db.collection("users").document(uid)
            .collection("friends")
            .limit(50)
            .get()
            .await()

        val q = query.trim().lowercase()

        return snap.documents.mapNotNull { d ->
            val friendUid = d.getString("friendUid") ?: d.id
            val display = d.getString("nickname") ?: d.getString("displayName") ?: return@mapNotNull null
            MentionPick(MentionPick.Kind.FRIEND, friendUid, display)
        }
            .filter {
                q.isBlank() || it.displayName.lowercase().contains(q)
            }
            .take(10)
    }

    private suspend fun loadGroupSuggestions(uid: String, query: String): List<MentionPick> {
        val snap = db.collection("users").document(uid)
            .collection("groups")
            .limit(50)
            .get()
            .await()

        val q = query.trim().lowercase()

        return snap.documents.mapNotNull { d ->
            val groupId = d.id
            val name = d.getString("nameSnapshot") ?: return@mapNotNull null
            MentionPick(MentionPick.Kind.GROUP, groupId, name)
        }
            .filter { pick ->
                q.isBlank() || pick.displayName.lowercase().contains(q)
            }
            .take(10)
    }

    private suspend fun loadGroupMembers(groupId: String): List<GroupMember> {
        val snap = db.collection("groups")
            .document(groupId)
            .collection("members")
            .limit(200)
            .get()
            .await()

        return snap.documents.mapNotNull { d ->
            val uid = d.getString("uid") ?: d.id
            val name = d.getString("displayName") ?: return@mapNotNull null
            GroupMember(uid = uid, displayName = name)
        }
    }

    private suspend fun expandGroupIntoRows(groupId: String, replaceRow: View) {
        val members = loadGroupMembers(groupId)

        // Si no hay miembros, no hacemos nada (o podrías dejarlo como texto plano)
        if (members.isEmpty()) return

        // Quitar la fila donde se eligió el grupo
        val index = playersContainer.indexOfChild(replaceRow).takeIf { it >= 0 } ?: return
        playersContainer.removeViewAt(index)

        // Insertar N filas en esa posición
        members.forEachIndexed { offset, m ->
            val newRow = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_session_player_form, playersContainer, false)

            val inputPlayerName: TextInputEditText = newRow.findViewById(R.id.inputPlayerName)
            val inputPlayerScore: TextInputEditText = newRow.findViewById(R.id.inputPlayerScore)
            val btnWinner: MaterialButton = newRow.findViewById(R.id.btnWinner)
            val btnRemove: View = newRow.findViewById(R.id.btnRemovePlayer)

            inputPlayerName.setText(m.displayName)
            inputPlayerScore.setText("")

            setWinner(btnWinner, false)

            btnWinner.setOnClickListener {
                val next = !((btnWinner.tag as? Boolean) ?: false)
                setWinner(btnWinner, next)
            }

            btnRemove.setOnClickListener {
                dismissMentionsPopup()
                playersContainer.removeView(newRow)
            }

            // Guardamos referencia: ESTE jugador es miembro de grupo (refId = uid del miembro)
            newRow.tag = MentionPick(MentionPick.Kind.GROUP_MEMBER, m.uid, m.displayName)

            // Si quieres permitir editar el nombre manualmente, entonces:
            // - si lo tocan y cambia, deberías poner newRow.tag = null (texto plano)
            inputPlayerName.doAfterTextChanged { editable ->
                val t = editable?.toString().orEmpty()
                val pick = newRow.tag as? MentionPick
                if (pick != null && pick.kind == MentionPick.Kind.GROUP_MEMBER && t != pick.displayName) {
                    newRow.tag = null
                }
            }

            playersContainer.addView(newRow, index + offset)
        }
    }


    companion object {
        private const val ARG_SESSION_ID = "sessionId"
    }
}