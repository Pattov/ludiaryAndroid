package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ludiary.android.data.local.entity.GroupEntity
import com.ludiary.android.data.model.UserGame
import com.ludiary.android.data.repository.library.UserGamesRepository
import com.ludiary.android.data.repository.library.GameBaseRepository
import com.ludiary.android.data.repository.profile.GroupsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de biblioteca.
 */
class LibraryViewModel(
    private val uid: String,
    private val userGamesRepository: UserGamesRepository,
    private val gameBaseRepository: GameBaseRepository,
    private val groupsRepository: GroupsRepository,
    private val syncCatalogAutomatically: Boolean = true
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState

    // Cache local
    private var myGames: List<UserGame> = emptyList()
    private var groups: List<GroupEntity> = emptyList()

    // Cache simple para juegos de grupos (groupId -> Lista de items)
    private val groupGamesCache = mutableMapOf<String, List<LibraryItem>>()

    init {
        viewModelScope.launch {
            userGamesRepository.initialSyncIfNeeded(uid)
        }

        if (syncCatalogAutomatically) {
            syncCatalog()
        }

        // Cargar mis juegos
        viewModelScope.launch {
            userGamesRepository.getUserGames(uid).collect { games ->
                myGames = games
                refreshContent()
            }
        }

        // Cargar grupos
        viewModelScope.launch {
            groupsRepository.observeGroups("").collect { list ->
                groups = list
                _uiState.update { it.copy(availableGroups = list.map { g -> g.groupId to g.nameSnapshot }) }
                // Si el filtro actual es "All", al llegar nuevos grupos podríamos querer refrescar
                if (_uiState.value.filter is LibraryFilter.All) {
                    refreshContent()
                }
            }
        }
    }

    /**
     * Cambia el filtro actual.
     */
    fun setFilter(filter: LibraryFilter) {
        if (_uiState.value.filter == filter) return
        _uiState.update { it.copy(filter = filter, isLoading = true) }
        viewModelScope.launch {
            refreshContent()
        }
    }

    private suspend fun refreshContent() {
        val filter = _uiState.value.filter

        // 1. Obtener mis items
        val myItems = myGames.map { LibraryItem(it, ownerName = "Yo") }

        val resultItems = when (filter) {
            LibraryFilter.Mine -> {
                myItems
            }
            is LibraryFilter.Group -> {
                val groupItems = fetchGroupGames(filter.groupId, filter.groupName)
                // Incluimos también mis juegos? Normalmente si filtro por grupo quiero ver lo del grupo.
                // Si yo estoy en el grupo, mis juegos "cuentan"?
                // Asumiremos que el usuario quiere ver "Juegos disponibles en el grupo X".
                // Eso incluiría los mios + los de mis compañeros.
                // Para simplificar, mostramos TODO lo de los miembros.
                groupItems + myItems // Simplificación: asumo que mis juegos son visibles para el grupo
            }
            LibraryFilter.All -> {
                // Todos = Mis juegos + Juegos de todos mis grupos
                val allGroupItems = mutableListOf<LibraryItem>()
                groups.forEach { g ->
                    allGroupItems.addAll(fetchGroupGames(g.groupId, g.nameSnapshot))
                }
                // Unimos y eliminamos duplicados por ID de juego (aunque IDs de UserGame son unicos por usuario)
                // Si un juego está en varios grupos (mismo usuario en varios grupos), aparecerá repetido?
                // fetchGroupGames devuelve items con "groupName". Si User A está en G1 y G2, y tiene juego J.
                // fetch(G1) -> J (G1)
                // fetch(G2) -> J (G2)
                // Al juntar, tendremos duplicados visuales.
                // Priorizaremos "Mios" y luego el resto.
                (myItems + allGroupItems).distinctBy { it.game.id }
            }
        }

        _uiState.update {
            it.copy(items = resultItems, isLoading = false, isEmpty = resultItems.isEmpty())
        }
    }

    private suspend fun fetchGroupGames(groupId: String, groupName: String): List<LibraryItem> {
        if (groupGamesCache.containsKey(groupId)) return groupGamesCache[groupId]!!

        val members = try {
            groupsRepository.observeMembers(groupId).first()
        } catch (e: Exception) {
            emptyList()
        }

        val list = mutableListOf<LibraryItem>()

        // Para cada miembro (excepto yo), buscamos sus juegos
        // Nota: Esto puede ser lento si hay muchos miembros.
        // En un caso real, esto debería paginarse o hacerse en backend.
        members.forEach { m ->
            if (m.uid == uid) return@forEach

            try {
                val games = userGamesRepository.getRemoteUserGames(m.uid)
                // TODO: Obtener nombre real del usuario. Por ahora usamos un placeholder o ID corto.
                val displayName = "Comp. ${m.uid.take(4)}"
                list.addAll(games.map {
                    LibraryItem(it, ownerName = displayName, groupName = groupName)
                })
            } catch (e: Exception) {
                // Ignorar fallos de red puntuales
            }
        }

        groupGamesCache[groupId] = list
        return list
    }

    fun onDeleteGameClicked(gameId: String) {
        viewModelScope.launch {
            userGamesRepository.deleteUserGame(uid, gameId)
        }
    }

    fun syncCatalog(forceFullSync: Boolean = false) {
        viewModelScope.launch {
            try {
                gameBaseRepository.syncGamesBase(forceFullSync)
            } catch (e: Exception) {
                // Log
            }
        }
    }
}