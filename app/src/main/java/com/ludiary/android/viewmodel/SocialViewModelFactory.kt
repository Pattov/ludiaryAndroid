package com.ludiary.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ludiary.android.data.repository.profile.FriendsRepository
import com.ludiary.android.data.repository.profile.GroupsRepository

/**
 * Factory para instanciar [FriendsViewModel] con sus dependencias.
 * @property friendsRepo Repositorio de amigos (capa de dominio/datos).
 * @property groupsRepo Repositorio de grupos (capa de dominio/datos).
 */
class SocialViewModelFactory(
    private val friendsRepo: FriendsRepository,
    private val groupsRepo: GroupsRepository
) : ViewModelProvider.Factory {

    /**
     * Crea una instancia del ViewModel solicitado.
     * @param modelClass Clase del ViewModel que se desea crear.
     * @return Instancia del ViewModel.
     * @throws IllegalArgumentException si se solicita un ViewModel no soportado.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FriendsViewModel(friendsRepo, groupsRepo) as T
    }
}