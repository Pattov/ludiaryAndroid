package com.ludiary.android.data.local

import com.ludiary.android.data.local.entity.UserEntity
import com.ludiary.android.data.model.User
import com.ludiary.android.data.model.UserPreferences

/**
 * Fuente de datos local para el perfil de usuario cuando no existe una sesión en Firebase.
 */
class LocalUserDataSource(
    private val db: LudiaryDatabase
) {
    private val userDao = db.userDao()

    /**
     * Obtiene el usuario local desde Room
     */
    suspend fun getLocalUser(): User {
        val entity = userDao.getLocalUser()
            ?: UserEntity()

        return User(
            uid = entity.uid,
            email = null,
            displayName = entity.displayName,
            isAnonymous = entity.isAnonymous,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            preferences = UserPreferences(
                language = entity.language,
                theme = entity.theme
            ),
            isAdmin = entity.isAdmin
        )
    }

    /**
     * Guarda o actualiza el usuario local en Room.
     */
    suspend fun saveLocalUser(user: User) {
        val entity = UserEntity(
            id = 0,
            uid = user.uid.ifEmpty { "local-guest" },
            displayName = user.displayName ?: "Invitado",
            language = user.preferences?.language?: "es",
            theme = user.preferences?.theme?: "system",
            isAnonymous = user.isAnonymous,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
            isAdmin = user.isAdmin
        )
        userDao.upsert(entity)
    }

    /**
     * Elimina el usuario local de Room
     */
    suspend fun clear(){
        userDao.clear()
    }
}