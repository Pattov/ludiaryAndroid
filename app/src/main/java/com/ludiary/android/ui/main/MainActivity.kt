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

        // Leemos el idioma guardado (por defecto "es")
        val prefs = newBase.getSharedPreferences("sync_prefs", MODE_PRIVATE)
        val savedLang = prefs.getString("app_language", null)

        // Si no hay idioma guardado → usamos el del sistema
        val langToUse = savedLang ?: Locale.getDefault().language

        // Aplicamos el idioma al contexto
        val localizedContext = LocaleManager.applyLanguage(newBase, langToUse)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("sync_prefs", MODE_PRIVATE)
        val autoSyncEnabled = prefs.getBoolean(SyncFragment.KEY_AUTO_SYNC, true)

        if (autoSyncEnabled) {
            SyncScheduler.enableAutoSync(applicationContext)
        }

        val theme = prefs.getString("app_theme", "system")
        ThemeManager.applyTheme(theme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Obtiene el NavHostFragment declarado en el layout principal
        val navHost = supportFragmentManager.findFragmentById(R.id.navHostMain) as NavHostFragment

        // Recupera el NavController para manejar la navegación
        val navController = navHost.navController

        // Asocia el BottomNavigationView con el NavController.
        // Esto sincroniza los ítems del menú.
        findViewById<BottomNavigationView>(R.id.bnvMain).setupWithNavController(navController)
    }
}