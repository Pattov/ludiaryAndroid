package com.ludiary.android.data.local

import com.ludiary.android.data.local.entity.toEntity
import com.ludiary.android.data.local.entity.toModel
import com.ludiary.android.data.model.User

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
        return entity?.toModel() ?: User()
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