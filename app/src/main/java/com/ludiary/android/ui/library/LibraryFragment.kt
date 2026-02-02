package com.ludiary.android.ui.library

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.R
import com.ludiary.android.data.local.LocalGameBaseDataSource
import com.ludiary.android.data.local.LocalUserGamesDataSource
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.repository.library.FirestoreGameBaseRepositoryImpl
import com.ludiary.android.data.repository.library.FirestoreUserGamesRepository
import com.ludiary.android.data.repository.library.GameBaseRepository
import com.ludiary.android.data.repository.library.GameBaseRepositoryImpl
import com.ludiary.android.data.repository.library.UserGamesRepository
import com.ludiary.android.data.repository.library.UserGamesRepositoryImpl
import com.ludiary.android.viewmodel.LibraryViewModel
import com.ludiary.android.viewmodel.LibraryViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Pantalla principal del módulo de Ludoteca.
 */
class LibraryFragment : Fragment(R.layout.fragment_library) {

    /**
     * ViewModel de Ludoteca.
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
        val localGameBaseDataSource = LocalGameBaseDataSource(db.gameBaseDao())
        val firestoreGameBaseRepository = FirestoreGameBaseRepositoryImpl(FirebaseFirestore.getInstance())

        val gameBaseRepository: GameBaseRepository =
            GameBaseRepositoryImpl(local = localGameBaseDataSource, remote = firestoreGameBaseRepository)

        LibraryViewModelFactory(
            uid = FirebaseAuth.getInstance().currentUser!!.uid,
            userGamesRepository = userGamesRepository,
            gameBaseRepository = gameBaseRepository,
            syncCatalogAutomatically = true
        )
    }

    private lateinit var adapter: UserGameAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var textSearch: TextInputEditText
    private lateinit var textEmpty: TextView

    private var currentQuery: String = ""
    private var lastGames: List<Any> = emptyList() // cambia Any por tu tipo real si quieres (recomendado)

    /**
     * Inicializa vistas, recycler, búsqueda y observación de estado.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupRecycler()
        setupFab()
        setupSearch()
        observeUiState()
    }

    /**
     * Enlaza las vistas del layout con propiedades del fragment.
     */
    private fun bindViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerLibrary)
        fab = view.findViewById(R.id.fabAddGame)
        textSearch = view.findViewById(R.id.textSearch)
        textEmpty = view.findViewById(R.id.textEmpty)
    }

    /**
     * Configura el RecyclerView y su adapter.
     */
    private fun setupRecycler() {
        adapter = UserGameAdapter(
            onEdit = { gameId ->
                findNavController().navigate(
                    R.id.nav_edit_user_game,
                    bundleOf("gameId" to gameId)
                )
            },
            onDelete = { gameId -> viewModel.onDeleteGameClicked(gameId) }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    /**
     * Configura el FAB para crear un nuevo juego.
     */
    private fun setupFab() {
        fab.setOnClickListener {
            findNavController().navigate(R.id.nav_edit_user_game)
        }
    }

    /**
     * Configura la búsqueda local.
     */
    private fun setupSearch() {
        textSearch.doAfterTextChanged { editable ->
            currentQuery = editable?.toString().orEmpty()
            applyFilterAndRender()
        }
    }

    /**
     * Observa el `uiState` del ViewModel.
     */
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { ui ->
                lastGames = ui.games as List<Any>
                applyFilterAndRender()
            }
        }
    }

    /**
     * Aplica el filtro de búsqueda y renderiza:
     * - la lista filtrada
     * - el mensaje de estado vacío
     */
    private fun applyFilterAndRender() {
        val q = currentQuery.trim()

        val filtered = if (q.isEmpty()) {
            lastGames
        } else {
            lastGames.filter { item ->
                val title = extractTitle(item)

                title.contains(q, ignoreCase = true)
            }
        }

        @Suppress("UNCHECKED_CAST")
        adapter.submitList(filtered as List<Nothing>)

        val isEmpty = filtered.isEmpty()
        val isSearching = q.isNotEmpty()

        if (isEmpty) {
            textEmpty.visibility = View.VISIBLE
            textEmpty.text = if (isSearching) {
                getString(R.string.search_no_results, q)
            } else {
                getString(R.string.library_empty)
            }
        } else {
            textEmpty.visibility = View.GONE
        }
    }

    /**
     * Fallback temporal para extraer el título de un item genérico.
     * @param item Elemento genérico de la lista.
     * @return Título del juego o string vacío si no se encuentra.
     */
    private fun extractTitle(item: Any): String {
        return try {
            val field = item::class.java.getDeclaredField("titleSnapshot")
            field.isAccessible = true
            (field.get(item) as? String).orEmpty()
        } catch (_: Exception) {
            try {
                val field = item::class.java.getDeclaredField("title")
                field.isAccessible = true
                (field.get(item) as? String).orEmpty()
            } catch (_: Exception) {
                ""
            }
        }
    }
}