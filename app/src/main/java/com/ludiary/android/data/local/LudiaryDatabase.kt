package com.ludiary.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ludiary.android.data.local.dao.UserDao
import com.ludiary.android.data.local.entity.UserEntity

/**
 * Punto central de acceso a la base de datos de la aplicación
 */
@Database(
    entities = [UserEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LudiaryDatabase: RoomDatabase() {

    /**
     * DAO asociado a la tabla 'user'
     * @return [UserDao]
     */
    abstract fun userDao(): UserDao

    /**
     * Instancia singleton de la base de datos
     * @param context Contexto de la aplicación
     */
    companion object {
        @Volatile private var INSTANCE: LudiaryDatabase? = null

        fun getInstance(context: Context): LudiaryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LudiaryDatabase::class.java,
                    "ludiary.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}