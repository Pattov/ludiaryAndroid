package com.ludiary.android.ui.main

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.ludiary.android.R
import com.ludiary.android.data.repository.notification.FcmTokensRepository
import com.ludiary.android.data.repository.notification.FirestoreNotificationsRepository
import com.ludiary.android.data.repository.notification.FunctionsNotificationsRepository
import com.ludiary.android.sync.SyncPrefs
import com.ludiary.android.sync.SyncScheduler
import com.ludiary.android.util.LocaleManager
import com.ludiary.android.util.ThemeManager
import com.ludiary.android.data.repository.notification.NotificationsRepository
import com.ludiary.android.data.repository.notification.FirestoreFcmTokensRepository
import com.ludiary.android.util.FirebaseProviders
import com.ludiary.android.viewmodel.NotificationsViewModel
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val CHANNEL_SOCIAL = "social"
    private val fcmTokensRepo by lazy {
        FirestoreFcmTokensRepository(FirebaseFirestore.getInstance())
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
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bnvMain)

        setupBottomNavigation(bottomNav, navController)
        setupDestinationFixes(bottomNav, navController)
        setupSyncScheduling()

        ensureNotificationChannel()
        requestPostNotificationsIfNeeded()

        registerFcmTokenIfLoggedIn(FirebaseAuth.getInstance(), fcmTokensRepo)

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

    private fun registerFcmTokenIfLoggedIn(
        auth: FirebaseAuth,
        repo: FcmTokensRepository
    ) {
        val user = auth.currentUser ?: return

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                lifecycleScope.launch {
                    runCatching { repo.upsertToken(user.uid, token) }
                }
            }
    }

    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(POST_NOTIFICATIONS) !=
                PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(POST_NOTIFICATIONS), 1001)
            }
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
    }

    /**
     * Aplica el tema de la aplicación.
     */
    private fun applyAppTheme() {
        val theme = readPrefs().getString(KEY_APP_THEME, DEFAULT_THEME)
        ThemeManager.applyTheme(theme)
    }

    /**
     * Busca el NavController del fragment principal.
     * @return NavController del fragment principal.
     */
    private fun findNavController(): NavController {
        val navHost = supportFragmentManager.findFragmentById(R.id.navHostMain) as NavHostFragment
        return navHost.navController
    }

    private val notificationsViewModel by lazy {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        val remote = FirestoreNotificationsRepository(firestore)
        val functions = FunctionsNotificationsRepository(FirebaseProviders.functions)

        val repo = NotificationsRepository(
            auth = auth,
            remote = remote,
            functions = functions,
            firestore = firestore
        )

        NotificationsViewModel(repo)
    }

    /**
     * Configura la navegación inferior.
     * @param bottomNav BottomNavigationView a configurar.
     * @param navController NavController a configurar.
     */
    private fun setupBottomNavigation(
        bottomNav: BottomNavigationView,
        navController: NavController
    ) {
        bottomNav.setupWithNavController(navController)
    }

    /**
     * Cambiar de pestañas entre fragment hijos de un fragment padre.
     * @param bottomNav BottomNavigationView a configurar.
     * @param navController NavController a configurar.
     */
    private fun setupDestinationFixes(
        bottomNav: BottomNavigationView,
        navController: NavController
    ) {
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

    /**
     * Configura el planificador de tareas de sincronización.
     */
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

    /**
     * Lee las preferencias de la aplicación.
     */
    private fun readPrefs() = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

    /**
     * Guarda el idioma de la aplicación en las preferencias.
     */
    private fun Context.readAppLanguage(): String? {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_APP_LANGUAGE, null)
    }

    private companion object {
        const val PREFS_NAME = "sync_prefs"
        const val KEY_APP_LANGUAGE = "app_language"
        const val KEY_APP_THEME = "app_theme"
        const val DEFAULT_THEME = "system"
    }

}