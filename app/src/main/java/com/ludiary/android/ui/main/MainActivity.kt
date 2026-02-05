package com.ludiary.android.ui.main

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ludiary.android.R
import com.ludiary.android.sync.SyncPrefs
import com.ludiary.android.sync.SyncScheduler
import com.ludiary.android.util.LocaleManager
import com.ludiary.android.util.ThemeManager
import com.ludiary.android.viewmodel.NotificationsViewModel
import com.ludiary.android.viewmodel.NotificationsViewModelFactory
import com.ludiary.android.data.repository.notification.FcmTokensRepository
import com.ludiary.android.data.repository.notification.FcmTokensRepositoryProvider
import com.ludiary.android.data.repository.notification.NotificationsRepositoryProvider
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val fcmTokensRepo: FcmTokensRepository by lazy(LazyThreadSafetyMode.NONE) {
        FcmTokensRepositoryProvider.provide(applicationContext)
    }

    private val notificationsViewModel: NotificationsViewModel by viewModels {
        NotificationsViewModelFactory(
            NotificationsRepositoryProvider.provide(applicationContext)
        )
    }

    override fun attachBaseContext(newBase: Context?) {
        if (newBase == null) {
            super.attachBaseContext(null)
            return
        }

        val langToUse = newBase.readAppLanguage() ?: Locale.getDefault().language
        super.attachBaseContext(LocaleManager.applyLanguage(newBase, langToUse))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navController = findNavController()
        val bottomNav = findViewById<BottomNavigationView>(R.id.bnvMain)

        setupBottomNavigation(bottomNav, navController)
        setupDestinationFixes(bottomNav, navController)
        setupSyncScheduling()

        ensureNotificationChannel()
        requestPostNotificationsIfNeeded()

        bottomNav.post {
            val socialBadge = bottomNav.getOrCreateBadge(R.id.nav_profile).apply {
                maxCharacterCount = 3
            }

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    notificationsViewModel.unreadCount.collect { count ->
                        if (count > 0) {
                            socialBadge.isVisible = true
                            socialBadge.number = count
                        } else {
                            socialBadge.isVisible = false
                            socialBadge.clearNumber()
                        }
                    }
                }
            }
        }
    }

    private fun requestPostNotificationsIfNeeded() {
        if (checkSelfPermission(POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
            requestPermissions(arrayOf(POST_NOTIFICATIONS), 1001)
        }
    }

    private fun ensureNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_SOCIAL,
            "Social",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Invitaciones y avisos sociales"
        }

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun applyAppTheme() {
        val theme = readPrefs().getString(KEY_APP_THEME, DEFAULT_THEME)
        ThemeManager.applyTheme(theme)
    }

    private fun findNavController(): NavController {
        val navHost = supportFragmentManager.findFragmentById(R.id.navHostMain) as NavHostFragment
        return navHost.navController
    }

    private fun setupBottomNavigation(bottomNav: BottomNavigationView, navController: NavController) {
        bottomNav.setupWithNavController(navController)
    }

    private fun setupDestinationFixes(bottomNav: BottomNavigationView, navController: NavController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val itemIdToCheck = when (destination.id) {
                R.id.nav_edit_session -> R.id.nav_sessions
                R.id.nav_edit_user_game -> R.id.nav_library
                R.id.nav_editProfileFragment,
                R.id.nav_preferencesFragment,
                R.id.nav_syncFragment,
                R.id.nav_socialFragment -> R.id.nav_profile
                else -> null
            }

            itemIdToCheck?.let { bottomNav.menu.findItem(it)?.isChecked = true }
        }
    }

    private fun setupSyncScheduling() {
        val syncPrefs = SyncPrefs(applicationContext)
        if (syncPrefs.isAutoSyncEnabled()) {
            SyncScheduler.enableAutoSyncUserGames(applicationContext)
            SyncScheduler.enableAutoSyncSessions(applicationContext)
            SyncScheduler.enableAutoSyncFriendsGroups(applicationContext)
        } else {
            SyncScheduler.disableAutoSyncUserGames(applicationContext)
            SyncScheduler.disableAutoSyncSessions(applicationContext)
            SyncScheduler.disableAutoSyncFriendsGroups(applicationContext)
        }
    }

    private fun readPrefs() = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

    private fun Context.readAppLanguage(): String? {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_APP_LANGUAGE, null)
    }

    private companion object {
        const val PREFS_NAME = "sync_prefs"
        const val KEY_APP_LANGUAGE = "app_language"
        const val KEY_APP_THEME = "app_theme"
        const val DEFAULT_THEME = "system"
        const val CHANNEL_SOCIAL = "social"
    }
}