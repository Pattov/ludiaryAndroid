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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.ludiary.android.R
import com.ludiary.android.data.repository.notification.FcmTokensRepository
import com.ludiary.android.sync.SyncPrefs
import com.ludiary.android.sync.SyncScheduler
import com.ludiary.android.util.LocaleManager
import com.ludiary.android.util.ThemeManager
import com.ludiary.android.data.repository.notification.NotificationsRepository
import com.ludiary.android.data.repository.notification.FirestoreFcmTokensRepository
import com.ludiary.android.viewmodel.NotificationsViewModel
import com.ludiary.android.viewmodel.NotificationsViewModelFactory
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import android.view.View

class MainActivity : AppCompatActivity() {

    /**
     * Repositorio para registrar tokens FCM por usuario (multi-dispositivo).
     * Se inicializa con Firestore singleton.
     */
    private val fcmTokensRepo by lazy {
        FirestoreFcmTokensRepository(FirebaseFirestore.getInstance())
    }

    /**
     * Aplica el idioma antes de inflar recursos (strings/layouts).
     */
    private val notificationsViewModel: NotificationsViewModel by viewModels {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        NotificationsViewModelFactory(
            NotificationsRepository(
                auth = auth,
                firestore = firestore
            )
        )
    }

    /**
     * Aplica el idioma antes de inflar recursos (strings/layouts).
     */
    override fun attachBaseContext(newBase: Context?) {
        if (newBase == null) {
            super.attachBaseContext(null)
            return
        }

        val langToUse = newBase.readAppLanguage() ?: Locale.getDefault().language
        super.attachBaseContext(LocaleManager.applyLanguage(newBase, langToUse))
    }

    /**
     * Inicializa UI y servicios principales:
     * - tema + layout
     * - navegación
     * - sync scheduling
     * - notificaciones (canal + permiso)
     * - token FCM
     * - badge de no leídas
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navController = findNavController()
        val bottomNav = findViewById<BottomNavigationView>(R.id.bnvMain)

        setupBottomNavigation(bottomNav, navController)
        setupDestinationFixes(bottomNav, navController)
        setupSyncScheduling()

        // Notificaciones del sistema: canal + permiso
        ensureNotificationChannel()
        requestPostNotificationsIfNeeded()

        // Push notifications: registrar token solo si hay sesión
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

    /**
     * Registra el token FCM del dispositivo en backend (Firestore) si hay usuario autenticado.
     * @param auth FirebaseAuth para obtener el usuario actual.
     * @param repo Repositorio que persiste el token (por ejemplo en `/users/{uid}/fcmTokens/{token}`).
     */
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

    /**
     * Solicita el permiso POST_NOTIFICATIONS si no está concedido.
     */
    private fun requestPostNotificationsIfNeeded() {
        if (checkSelfPermission(POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
            requestPermissions(arrayOf(POST_NOTIFICATIONS), 1001)
        }
    }

    /**
     * Crea (si no existe) el canal de notificaciones sociales.
     */
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
        val root = findViewById<ConstraintLayout>(R.id.rootMain)
        val navHost = findViewById<View>(R.id.navHostMain)

        // Destinos que deben ir a pantalla completa (sin bottom nav)
        val fullScreenDestinations = setOf(
            R.id.nav_edit_session,
            R.id.nav_edit_user_game,
            R.id.nav_editProfileFragment,
            R.id.nav_preferencesFragment,
            R.id.nav_syncFragment,
            R.id.nav_socialFragment,
            R.id.nav_groupDetailFragment
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->

            val isFullScreen = destination.id in fullScreenDestinations

            // 1) Mostrar/ocultar bottom nav
            bottomNav.visibility = if (isFullScreen) View.GONE else View.VISIBLE

            // 2) Cambiar el constraint inferior del NavHost:
            //    - Fullscreen: bottom del NavHost al parent
            //    - Tabs: bottom del NavHost al top del BottomNav
            val set = ConstraintSet().apply { clone(root) }

            if (isFullScreen) {
                set.connect(
                    R.id.navHostMain,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
            } else {
                set.connect(
                    R.id.navHostMain,
                    ConstraintSet.BOTTOM,
                    R.id.bnvMain,
                    ConstraintSet.TOP
                )
            }

            set.applyTo(root)
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
        const val CHANNEL_SOCIAL = "social"
    }

}