package com.ludiary.android.ui.library

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.ludiary.android.R
import com.ludiary.android.data.local.LocalGameBaseDataSource
import com.ludiary.android.data.local.LocalGroupsDataSource
import com.ludiary.android.data.local.LocalUserGamesDataSource
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.model.UserGame
import com.ludiary.android.data.repository.library.FirestoreGameBaseRepositoryImpl
import com.ludiary.android.data.repository.library.FirestoreUserGamesRepository
import com.ludiary.android.data.repository.library.GameBaseRepository
import com.ludiary.android.data.repository.library.GameBaseRepositoryImpl
import com.ludiary.android.data.repository.library.UserGamesRepository
import com.ludiary.android.data.repository.library.UserGamesRepositoryImpl
import com.ludiary.android.data.repository.profile.FirestoreGroupsRepository
import com.ludiary.android.data.repository.profile.FunctionsSocialRepository
import com.ludiary.android.data.repository.profile.GroupsRepository
import com.ludiary.android.data.repository.profile.GroupsRepositoryImpl
import com.ludiary.android.viewmodel.LibraryFilter
import com.ludiary.android.viewmodel.LibraryItem
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
        val firestore = FirebaseFirestore.getInstance()

        // --- Repositorio de la ludoteca del usuario (userGames) ---
        val localUserGames = LocalUserGamesDataSource(db.userGameDao())
        val remoteUserGames = FirestoreUserGamesRepository(firestore)
        val userGamesRepository: UserGamesRepository =
            UserGamesRepositoryImpl(localUserGames, remoteUserGames)

        // --- Repositorio del catálogo oficial (games_base) ---
        val localGameBaseDataSource = LocalGameBaseDataSource(db.gameBaseDao())
        val firestoreGameBaseRepository = FirestoreGameBaseRepositoryImpl(firestore)

        val gameBaseRepository: GameBaseRepository =
            GameBaseRepositoryImpl(local = localGameBaseDataSource, remote = firestoreGameBaseRepository)

        // --- Repositorio de grupos ---
        val localGroups = LocalGroupsDataSource(db.groupDao())
        val remoteGroups = FirestoreGroupsRepository(firestore)
        val functions = FunctionsSocialRepository(FirebaseFunctions.getInstance())
        val auth = FirebaseAuth.getInstance()
        val groupsRepository: GroupsRepository = GroupsRepositoryImpl(localGroups, remoteGroups, functions, auth)

        LibraryViewModelFactory(
            uid = auth.currentUser!!.uid,
            userGamesRepository = userGamesRepository,
            gameBaseRepository = gameBaseRepository,
            groupsRepository = groupsRepository,
            syncCatalogAutomatically = true
        )
    }

    private lateinit var adapter: UserGameAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var textSearch: TextInputEditText
    private lateinit var textEmpty: TextView
    private lateinit var btnFilter: MaterialButton

    private var currentQuery: String = ""
    private var lastItems: List<LibraryItem> = emptyList()

    /**
     * Inicializa vistas, recycler, búsqueda y observación de estado.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupRecycler()
        setupFab()
        setupSearch()
        setupFilter()
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
        btnFilter = view.findViewById(R.id.btnFilter)
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
            onDelete = { game -> confirmDelete(game) }
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

    private fun setupFilter() {
        btnFilter.setOnClickListener { view ->
            val state = viewModel.uiState.value
            val popup = PopupMenu(requireContext(), view)

            // Options
            popup.menu.add(0, 0, 0, "Todos")
            popup.menu.add(0, 1, 1, "Míos")

            state.availableGroups.forEachIndexed { index, pair ->
                // pair is (id, name)
                // Use index + 2 as id
                popup.menu.add(1, index + 2, index + 2, pair.second)
            }

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    0 -> viewModel.setFilter(LibraryFilter.All)
                    1 -> viewModel.setFilter(LibraryFilter.Mine)
                    else -> {
                        // Group
                        val groupIndex = item.itemId - 2
                        if (groupIndex in state.availableGroups.indices) {
                            val group = state.availableGroups[groupIndex]
                            viewModel.setFilter(LibraryFilter.Group(group.first, group.second))
                        }
                    }
                }
                true
            }
            popup.show()
        }
    }

    /**
     * Observa el `uiState` del ViewModel.
     */
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { ui ->
                lastItems = ui.items
                btnFilter.visibility = if (ui.availableGroups.isNotEmpty()) View.VISIBLE else View.GONE
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
            lastItems
        } else {
            lastItems.filter { item ->
                val title = item.game.titleSnapshot
                title.contains(q, ignoreCase = true)
            }
        }

        adapter.submitList(filtered)

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
     * Muestra confirmación antes de borrar un juego de la ludoteca del usuario.
     * @param game Juego (para mostrar el título y borrar por id).
     */
    private fun confirmDelete(game: UserGame) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.action_delete))
            .setMessage(getString(R.string.library_delete_confirm, game.titleSnapshot))
            .setPositiveButton(getString(R.string.action_delete)) { _: DialogInterface, _: Int ->
                viewModel.onDeleteGameClicked(game.id)
            }
            .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}