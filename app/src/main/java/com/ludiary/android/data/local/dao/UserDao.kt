package com.ludiary.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ludiary.android.data.local.entity.UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM user WHERE id = 0 Limit 1")
    suspend fun getLocalUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Query("DELETE FROM user")
    suspend fun clear()
}