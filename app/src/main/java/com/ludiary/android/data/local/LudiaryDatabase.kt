package com.ludiary.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ludiary.android.data.local.dao.*
import com.ludiary.android.data.local.entity.*

/**
 * Punto central de acceso a la base de datos de la aplicación
 */
@Database(
    entities = [
        UserEntity::class,
        GameBaseEntity::class,
        UserGameEntity::class,
        GameSuggestionEntity::class,
        SessionEntity::class,
        SessionPlayerEntity::class,
        FriendEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class,
        GroupInviteEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(LudiaryConverters::class)
abstract class LudiaryDatabase: RoomDatabase() {

    /**
     * DAO asociado a la tabla 'user'
     * @return [UserDao]
     */
    abstract fun userDao(): UserDao

    /**
     * DAO asociado a la tabla 'games_base'
     * @return [GameBaseDao]
     */
    abstract fun gameBaseDao(): GameBaseDao

    /**
     * DAO asociado a la tabla 'user_games'
     * @return [UserGameDao]
     */
    abstract fun userGameDao(): UserGameDao

    /**
     * DAO asociado a la tabla 'game_suggestions'
     * @return [GameSuggestionDao]
     */
    abstract fun gameSuggestionDao(): GameSuggestionDao

    /**
     * DAO asociado a la tabla 'sessions'
     * @return [SessionDao]
     */
    abstract fun sessionDao(): SessionDao

    /**
     * DAO asociado a la tabla 'friendDao'
     * @return [FriendDao]
     */
    abstract fun friendDao(): FriendDao

    /**
     * DAO asociado a la tabla 'groups'
     * @return [GroupDao]
     */
    abstract fun groupDao(): GroupDao

    /**
     * Instancia singleton de la base de datos
     * @param context Contexto de la aplicación
     */
    companion object {
        @Volatile
        private var INSTANCE: LudiaryDatabase? = null

        fun getInstance(context: Context): LudiaryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LudiaryDatabase::class.java,
                    "ludiary.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}