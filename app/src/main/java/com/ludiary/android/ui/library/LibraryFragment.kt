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
import com.ludiary.android.data.local.LocalUserGamesDataSource
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.repository.FirestoreUserGamesRepository
import com.ludiary.android.data.repository.UserGamesRepository
import com.ludiary.android.data.repository.UserGamesRepositoryImpl
import com.ludiary.android.viewmodel.LibraryViewModel
import com.ludiary.android.viewmodel.LibraryViewModelFactory
import kotlinx.coroutines.launch
import com.ludiary.android.data.local.LocalGameBaseDataSource
import com.ludiary.android.data.repository.GameBaseRepository
import com.ludiary.android.data.repository.GameBaseRepositoryImpl
import com.ludiary.android.data.repository.FirestoreGameBaseRepositoryImpl

/**
 * Pantalla principal del módulo de Ludoteca.
 */
class LibraryFragment : Fragment(R.layout.fragment_library) {

    /**
     * ViewModel inyectado mediante Factory
     */
    private val viewModel: LibraryViewModel by viewModels {
        val appContext = requireContext().applicationContext
        val db = LudiaryDatabase.getInstance(appContext)

        // --- Repositorio de la ludoteca del usuario (userGames) ---
        val localUserGames = LocalUserGamesDataSource(db.userGameDao())
        val remoteUserGames = FirestoreUserGamesRepository(FirebaseFirestore.getInstance())
        val userGamesRepository: UserGamesRepository =
            UserGamesRepositoryImpl(localUserGames, remoteUserGames)

        // --- Repositorio del catálogo oficial (games_base) ---
        val gameBaseDao = db.gameBaseDao()
        val localGameBaseDataSource = LocalGameBaseDataSource(gameBaseDao)

        val firestore = FirebaseFirestore.getInstance()
        val firestoreGameBaseRepository = FirestoreGameBaseRepositoryImpl(firestore)

        val gameBaseRepository: GameBaseRepository =
            GameBaseRepositoryImpl(local = localGameBaseDataSource, remote = firestoreGameBaseRepository)

        // --- Factory del ViewModel ---
        LibraryViewModelFactory(
            uid = FirebaseAuth.getInstance().currentUser!!.uid,
            userGamesRepository = userGamesRepository,
            gameBaseRepository = gameBaseRepository,
            syncCatalogAutomatically = true   // pon false si quieres solo sync manual
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