package com.ludiary.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ludiary.android.data.local.entity.UserEntity

/**
 * Acceso a datos de la tabla 'user'
 */
@Dao
interface UserDao {

    /**
     * Obtiene el usuario local de la base de datos
     *
     * @return El usuario local de la base de datos
     */
    @Query("SELECT * FROM user WHERE id = 0 Limit 1")
    suspend fun getLocalUser(): UserEntity?

    /**
     * Inserta o actualiza un usuario en la base de datos
     *
     * @param user El usuario a insertar o actualizar
     */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsert(user: UserEntity)

    /**
     * Elimina todos los usuarios de la base de datos
     */
    @Query("DELETE FROM user")
    suspend fun clear()
}