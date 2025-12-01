package com.ludiary.android.ui.library

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.R
import com.ludiary.android.data.repository.FirestoreUserGamesRepository
import com.ludiary.android.viewmodel.LibraryViewModel
import com.ludiary.android.viewmodel.LibraryViewModelFactory
import kotlinx.coroutines.launch

/**
 * Pantalla principal del módulo de biblioteca.
 */
class LibraryFragment : Fragment(R.layout.fragment_library) {

    /**
     * ViewModel inyectado mediante Factory
     */
    private val viewModel: LibraryViewModel by viewModels{
        LibraryViewModelFactory(
            uid = FirebaseAuth.getInstance().currentUser!!.uid,
            repository = FirestoreUserGamesRepository(
                FirebaseFirestore.getInstance()
            )
        )
    }

    /**
     * Adaptador para la lista de juegos del usuario.
     */
    private lateinit var adapter: UserGameAdapter

    /**
     * Llamada al crear la vista del fragmento.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Adaptador con callbacks para editar y borrar
        adapter = UserGameAdapter(
            onEdit = { gameId ->
                findNavController().navigate(
                    R.id.nav_edit_user_game,
                    bundleOf("gameId" to gameId)
                )
            },
            onDelete = { gameId -> viewModel.onDeleteGameClicked(gameId) }
        )

        // Configurar RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerLibrary)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // FAB → navegar al formulario de creación de juego
        val fab = view.findViewById<FloatingActionButton>(R.id.fabAddGame)
        fab.setOnClickListener {
            findNavController().navigate(R.id.nav_edit_user_game)
        }

        // Observar el estado emitido por el ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { ui ->
                adapter.submitList(ui.games)
            }
        }

    }
}