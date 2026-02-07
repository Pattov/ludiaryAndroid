package com.ludiary.android.data.repository.profile

import com.ludiary.android.data.model.User

/**
 * Interfaz encargada de gestionar el repositorio de perfil.
 */
interface ProfileRepository {

    /**
     * Obtiene el perfil del usuario.
     *
     * @return El perfil del usuario.
     */
    suspend fun getOrCreate(): User

    /**
     * Actualiza el perfil del usuario.
     *
     * @param displayName El nombre de usuario.
     * @param language El idioma.
     * @param theme El tema.
     * @return El perfil del usuario actualizado.
     */
    suspend fun update(
        displayName: String?,
        language: String?,
        theme: String?,
        mentionUserPrefix: String?,
        mentionGroupPrefix: String?
    ): User

    /**
     * Cierra la sesión del usuario.
     */
    suspend fun signOut()
}