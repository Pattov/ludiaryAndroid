package com.ludiary.android.ui.main

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ludiary.android.R
import com.ludiary.android.sync.SyncScheduler
import com.ludiary.android.ui.profile.SyncFragment
import com.ludiary.android.util.LocaleManager
import com.ludiary.android.util.ThemeManager
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        if (newBase == null) {
            super.attachBaseContext(null)
            return
        }

        val prefs = newBase.getSharedPreferences("sync_prefs", MODE_PRIVATE)
        val savedLang = prefs.getString("app_language", null)

        val langToUse = savedLang ?: Locale.getDefault().language

        val localizedContext = LocaleManager.applyLanguage(newBase, langToUse)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("sync_prefs", MODE_PRIVATE)
        val autoSyncEnabled = prefs.getBoolean(SyncFragment.KEY_AUTO_SYNC, true)

        // 2026 sincro
        if (autoSyncEnabled) {
            SyncScheduler.enableAutoSyncUserGames(applicationContext)
            SyncScheduler.enableAutoSyncSessions(applicationContext)
        }

        val theme = prefs.getString("app_theme", "system")
        ThemeManager.applyTheme(theme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHost = supportFragmentManager.findFragmentById(R.id.navHostMain) as NavHostFragment

        val navController = navHost.navController

        findViewById<BottomNavigationView>(R.id.bnvMain).setupWithNavController(navController)
    }
}