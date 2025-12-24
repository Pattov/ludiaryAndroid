package com.ludiary.android.data.local

import com.ludiary.android.data.local.entity.toEntity
import com.ludiary.android.data.local.entity.toModel
import com.ludiary.android.data.model.User
import com.ludiary.android.data.model.UserPreferences
import java.util.Locale

/**
 * Fuente de datos local para el perfil de usuario cuando no existe una sesión en Firebase.
 * @property db Instancia de la base de datos Room.
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

        if (entity != null) {
            val model = entity.toModel()
            return model
        }

        // Se crea únicamente cuando no existe ningún usuario persistido en Room y no hay sesión Firebase.
        val guest = User(
            uid = "local_guest",
            email = null,
            displayName = null,
            isAnonymous = true,
            preferences = UserPreferences(
                language = Locale.getDefault().language,
                theme = "system"
            )
        )

        userDao.upsert(guest.toEntity())
        return userDao.getLocalUser()?.toModel() ?: guest
    }

    /**
     * Guarda o actualiza el usuario local en Room.
     */
    suspend fun saveLocalUser(user: User) {
        userDao.upsert(user.toEntity())
    }

    /**
     * Elimina el usuario local de Room
     */
    suspend fun clear(){
        userDao.clear()
    }
}