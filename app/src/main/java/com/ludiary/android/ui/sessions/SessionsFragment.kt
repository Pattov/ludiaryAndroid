package com.ludiary.android.ui.sessions

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.ludiary.android.R
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.entity.SessionEntity
import com.ludiary.android.sync.SyncScheduler
import com.ludiary.android.viewmodel.SessionsViewModel
import com.ludiary.android.viewmodel.SessionsViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragmento que muestra la lista de partidas (sesiones).
 * @param R.layout.fragment_sessions layout de este fragmento.
 */
class SessionsFragment : Fragment(R.layout.fragment_sessions) {

    private lateinit var vm: SessionsViewModel
    private lateinit var adapter: SessionsAdapter

    private lateinit var recycler: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var progress: ProgressBar
    private lateinit var textEmpty: TextView
    private lateinit var textError: TextView
    private lateinit var textSearch: TextInputEditText

    private var currentQuery: String = ""
    private var lastSessions: List<SessionEntity> = emptyList()
    private var lastLoading: Boolean = false
    private var lastErrorRes: Int? = null

    /**
     * Inicializa vistas, recycler, viewmodel, listeners y observación de estado.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupRecycler()
        setupViewModel()
        setupListeners()
        observeUiState()

        vm.start()
        triggerSync()
    }

    /**
     * Enlaza las vistas del fragmento.
     */
    private fun bindViews(view: View) {
        recycler = view.findViewById(R.id.recyclerSession)
        fab = view.findViewById(R.id.fabAddSession)
        progress = view.findViewById(R.id.progressLoading)
        textEmpty = view.findViewById(R.id.textEmpty)
        textError = view.findViewById(R.id.textError)
        textSearch = view.findViewById(R.id.textSearch)
    }

    /**
     * Configura el RecyclerView y su adaptador.
     */
    private fun setupRecycler() {
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = SessionsAdapter(
            onItemClick = { session -> navigateToEdit(session.id) },
            onEditClick = { session -> navigateToEdit(session.id) },
            onDeleteClick = { session -> confirmDelete(session.id, session.gameTitle) }
        )

        recycler.adapter = adapter
    }

    /**
     * Crea el ViewModel.
     */
    private fun setupViewModel() {
        val context = requireContext().applicationContext
        val db = LudiaryDatabase.getInstance(context)
        val auth = FirebaseAuth.getInstance()

        val factory = SessionsViewModelFactory(context, db, auth)
        vm = ViewModelProvider(this, factory)[SessionsViewModel::class.java]
    }

    /**
     * Configura los listeners de la UI.
     */
    private fun setupListeners() {
        fab.setOnClickListener { navigateToCreate() }

        textSearch.doAfterTextChanged { editable ->
            currentQuery = editable?.toString().orEmpty()
            applyFilterAndRender()
        }
    }

    /**
     * Observa el estado del ViewModel y actualiza UI.
     */
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            vm.uiState.collectLatest { state ->
                lastLoading = state.loading
                lastErrorRes = state.errorRes
                lastSessions = state.sessions

                applyFilterAndRender()
            }
        }
    }

    /**
     * Aplica el filtro de búsqueda sobre el último listado recibido y renderiza estado + lista.
     */
    private fun applyFilterAndRender() {
        val filtered = filterSessions(lastSessions, currentQuery)

        renderState(
            isLoading = lastLoading,
            isEmpty = filtered.isEmpty(),
            errorRes = lastErrorRes
        )

        adapter.submitList(filtered)
    }

    /**
     * Filtrado simple de sesiones por título del juego.
     * @param sessions Lista base sin filtrar.
     * @param query Texto de búsqueda.
     * @return Lista filtrada.
     */
    private fun filterSessions(
        sessions: List<SessionEntity>,
        query: String
    ): List<SessionEntity> {
        val q = query.trim()
        if (q.isEmpty()) return sessions

        return sessions.filter { s ->
            s.gameTitle.contains(q, ignoreCase = true)
        }
    }

    /**
     * Renderiza estados simples de pantalla: loading, vacío y error.
     */
    private fun renderState(
        isLoading: Boolean,
        isEmpty: Boolean,
        errorRes: Int?
    ) {
        val hasError = errorRes != null

        progress.visibility = if (isLoading) View.VISIBLE else View.GONE
        recycler.visibility = if (!isLoading && !hasError) View.VISIBLE else View.INVISIBLE
        textError.visibility = if (hasError) View.VISIBLE else View.GONE

        if (!isLoading && !hasError && isEmpty) {
            textEmpty.visibility = View.VISIBLE

            textEmpty.text = if (currentQuery.isNotBlank()) {
                getString(
                    R.string.search_no_results,
                    currentQuery
                )
            } else {
                getString(R.string.sessions_empty)
            }
        } else {
            textEmpty.visibility = View.GONE
        }

        errorRes?.let { textError.setText(it) }
    }

    /**
     * Muestra confirmación antes de borrar una sesión.
     *
     * @param sessionId ID de la sesión a borrar.
     * @param title Título del juego (para mostrar en el mensaje).
     */
    private fun confirmDelete(sessionId: String, title: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.action_delete))
            .setMessage(getString(R.string.session_delete_confirm, title))
            .setPositiveButton(getString(R.string.action_delete)) { _: DialogInterface, _: Int ->
                vm.deleteSession(sessionId)
            }
            .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Lanza la sincronización contra Firestore (one-shot).
     */
    private fun triggerSync() {
        SyncScheduler.enqueueOneTimeSessionsSync(requireContext().applicationContext)
    }

    /**
     * Navega a la pantalla de crear una nueva partida.
     */
    private fun navigateToCreate() {
        findNavController().navigate(R.id.action_sessions_to_edit_session)
    }

    /**
     * Navega a la pantalla de edición de una partida.
     */
    private fun navigateToEdit(sessionId: String) {
        val args = Bundle().apply { putString(ARG_SESSION_ID, sessionId) }
        findNavController().navigate(R.id.action_sessions_to_edit_session, args)
    }

    companion object {
        private const val ARG_SESSION_ID = "sessionId"
    }
}
