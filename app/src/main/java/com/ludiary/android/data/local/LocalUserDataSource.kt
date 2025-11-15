package com.ludiary.android.data.local

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.ludiary.android.data.model.User
import com.ludiary.android.data.model.UserPreferences

class LocalUserDataSource(context: Context) {

    private val prefs = context.getSharedPreferences("ludiary_user",MODE_PRIVATE)

    fun getLocalUser(): User {
        val displayName = prefs.getString("displayName", "Invitado")
        val language = prefs.getString("language", "es") ?: "es"
        val theme = prefs.getString("theme", "system") ?: "system"

        return User(
            uid = "local-guest",
            email = null,
            displayName = displayName,
            isAnonymous = true,
            createdAt = null,
            updatedAt = null,
            preferences = UserPreferences(language = language, theme = theme),
            isAdmin = false
        )
    }

    fun saveLocalUser(user: User) {
        prefs.edit()
            .putString("displayName",user.displayName ?: "Invitado")
            .putString("language",user.preferences?.language ?: "es")
            .putString("theme",user.preferences?.theme ?: "system")
            .apply()
    }

    fun clear(){
        prefs.edit().clear().apply()
    }
}