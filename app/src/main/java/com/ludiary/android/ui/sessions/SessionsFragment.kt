package com.ludiary.android.ui.sessions

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.ludiary.android.R
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.sync.SyncScheduler
import com.ludiary.android.util.showError
import com.ludiary.android.viewmodel.SessionsViewModel
import com.ludiary.android.viewmodel.SessionsViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragmento que muestra la lista de sesiones.
 * @param R.layout.fragment_sessions layout de este fragmento.
 */
class SessionsFragment : Fragment(R.layout.fragment_sessions) {

    private lateinit var vm: SessionsViewModel
    private lateinit var adapter: SessionsAdapter

    private lateinit var recycler: RecyclerView
    private lateinit var fab: FloatingActionButton

    /**
     * Se llama cuando la vista del fragmento ha sido creada.
     * @param view Vista del fragmento.
     * @param savedInstanceState Estado de la instancia guardada.
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
     * @param view Vista del fragmento.
     */
    private fun bindViews(view: View) {
        recycler = view.findViewById(R.id.recyclerSession)
        fab = view.findViewById(R.id.fabAddSession)
    }

    /**
     * Configura el RecyclerView y su adaptador.
     */
    private fun setupRecycler() {
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = SessionsAdapter(
            onItemClick = { session -> navigateToEdit(session.id) },
            onEditClick = { session -> navigateToEdit(session.id) },
            onDeleteClick = { session -> vm.deleteSession(session.id) }
        )
        recycler.adapter = adapter
    }

    /**
     * Crea el ViewModel.
     */
    private fun setupViewModel() {
        val appContext = requireContext().applicationContext
        val db = LudiaryDatabase.getInstance(appContext)
        val auth = FirebaseAuth.getInstance()

        val factory = SessionsViewModelFactory(db, auth)
        vm = ViewModelProvider(this, factory)[SessionsViewModel::class.java]
    }

    /**
     * Configura los listeners de la UI.
     */
    private fun setupListeners() {
        fab.setOnClickListener { navigateToCreate() }
    }

    /**
     * Observa el estado del ViewModel y actualiza la UI.
     */
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            vm.uiState.collectLatest { state ->

                adapter.submitList(state.sessions)

                state.errorRes?.let {
                    requireContext().showError(it)
                }
            }
        }
    }

    /**
     * Lanza la sincronización de Firestore
     */
    private fun triggerSync() {
        SyncScheduler.enqueueSessionsSync(requireContext().applicationContext)
    }

    /**
     * Navega a la pantalla de crear una nueva partida.
     */
    private fun navigateToCreate() {
        findNavController().navigate(R.id.action_sessions_to_edit_session)
    }

    /**
     * Navega a la pantalla de edición de una partida.
     * @param sessionId Identificador único de la partida.
     */
    private fun navigateToEdit(sessionId: String) {
        val args = Bundle().apply { putString(ARG_SESSION_ID, sessionId) }
        findNavController().navigate(R.id.action_sessions_to_edit_session, args)
    }

    /**
     * Identificador único de la partida.
     * @param ARG_SESSION_ID Identificador único de la partida.
     */
    companion object {
        private const val ARG_SESSION_ID = "sessionId"
    }

}