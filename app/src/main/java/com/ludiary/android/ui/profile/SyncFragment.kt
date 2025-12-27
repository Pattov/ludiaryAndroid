package com.ludiary.android.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.R
import com.ludiary.android.data.local.LocalSessionsDataSource
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.LocalUserGamesDataSource
import com.ludiary.android.data.repository.FirestoreSessionsRepository
import com.ludiary.android.data.repository.FirestoreUserGamesRepository
import com.ludiary.android.data.repository.GroupIdProvider
import com.ludiary.android.data.repository.SessionsRepository
import com.ludiary.android.data.repository.SessionsRepositoryImpl
import com.ludiary.android.data.repository.UserGamesRepository
import com.ludiary.android.data.repository.UserGamesRepositoryImpl
import com.ludiary.android.sync.SyncScheduler
import com.ludiary.android.sync.SyncPrefs
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Fragmento para sincronización de juegos del usuario.
 * Permite activar/desactivar la sincronización, ofrecer sincronización manual y mostrar la información del usuario.
 */
class SyncFragment : Fragment(R.layout.form_sync_profile) {

    private lateinit var switchAutoSync: MaterialSwitch
    private lateinit var tvLastCatalogSync: TextView
    private lateinit var tvLastLibrarySync: TextView
    private lateinit var tvManualWarning: TextView
    private lateinit var btnSyncNow: Button

    /**
     * Obtiene las preferencias de la sincronización.
     */
    private val prefs by lazy {
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Obtiene la instancia de Firebase Authentication.
     */
    private val auth by lazy { FirebaseAuth.getInstance() }

    /**
     * Obtiene las preferencias de sincronización.
     */
    private val syncPrefs by lazy { SyncPrefs(requireContext().applicationContext) }

    /**
     * Repositorio de juegos del usuario.
     * Se inicializa al crear el fragmento.
     * @return Instancia de [UserGamesRepository].
     * @throws Exception si ocurre un error al inicializar el repositorio.
     */
    private val userGamesRepo: UserGamesRepository by lazy {
        val db = LudiaryDatabase.getInstance(requireContext().applicationContext)
        val localDS = LocalUserGamesDataSource(db.userGameDao())
        val remote = FirestoreUserGamesRepository(FirebaseFirestore.getInstance())
        UserGamesRepositoryImpl(localDS, remote)
    }

    /**
     * Repositorio de sesiones.
     * Se inicializa al crear el fragmento.
     * @return Instancia de [SessionsRepository].
     */
    private val sessionsRepo: SessionsRepository by lazy {
        val context = requireContext().applicationContext
        val db = LudiaryDatabase.getInstance(context)

        val localDS = LocalSessionsDataSource(db.sessionDao())
        val remote = FirestoreSessionsRepository(FirebaseFirestore.getInstance())
        val prefs = SyncPrefs(context)

        val groupProvider = object : GroupIdProvider {
            override suspend fun getGroupIdsForUser(uid: String): List<String> = emptyList()
        }

        SessionsRepositoryImpl(
            local = localDS,
            remote = remote,
            syncPrefs = prefs,
            groupIdProvider = groupProvider
        )
    }


    /**
     * Configura la interfaz de usuario al crear el fragmento.
     * @param view La vista del fragmento.
     * @param savedInstanceState El estado guardado del fragmento.
     * @return Instancia de [SyncFragment].
     * @throws Exception si ocurre un error al cargar las preferencias.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        switchAutoSync = view.findViewById(R.id.switchAutoSync)
        tvLastCatalogSync = view.findViewById(R.id.tvLastCatalogSync)
        tvLastLibrarySync = view.findViewById(R.id.tvLastLibrarySync)
        tvManualWarning = view.findViewById(R.id.tvManualWarning)
        btnSyncNow = view.findViewById(R.id.btnSyncNow)

        setupUi()
    }

    /**
     * Configura la interfaz de usuario.
     * Carga las preferencias y configura los listeners.
     * @return Instancia de [SyncFragment].
     * @throws Exception si ocurre un error al cargar las preferencias.
     */
    private fun setupUi() {
        val autoSyncEnabled = prefs.getBoolean(KEY_AUTO_SYNC, true)
        val lastCatalogSync = prefs.getLong(KEY_LAST_CATALOG_SYNC, 0L)
        val lastLibrarySync = prefs.getLong(KEY_LAST_LIBRARY_SYNC, 0L)

        switchAutoSync.isChecked = autoSyncEnabled
        tvLastCatalogSync.text = formatDateOrNever(lastCatalogSync)
        tvLastLibrarySync.text = formatDateOrNever(lastLibrarySync)

        refreshPendingAndRender()

        switchAutoSync.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean(KEY_AUTO_SYNC, isChecked) }

            if (isChecked) {
                SyncScheduler.enableAutoSync(requireContext().applicationContext)
            } else {
                SyncScheduler.disableAutoSync(requireContext().applicationContext)
            }

            refreshPendingAndRender()
        }

        // Sync manual
        btnSyncNow.setOnClickListener {
            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Necesitas iniciar sesión para sincronizar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSyncNow.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val syncedUp = userGamesRepo.syncPending(uid)
                    val since = syncPrefs.getLastUserGamesPull(uid)
                    val (appliedDown, maxTs) = userGamesRepo.syncDownIncremental(uid, since)

                    val now = System.currentTimeMillis()
                    if (maxTs != null) syncPrefs.setLastUserGamesPull(uid, maxTs)
                    if (syncedUp > 0 || appliedDown > 0) {
                        prefs.edit { putLong(KEY_LAST_LIBRARY_SYNC, now) }
                        tvLastLibrarySync.text = formatDateOrNever(now)
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error UserGames: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                try {
                    sessionsRepo.sync(uid)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error Sessions: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                refreshPendingAndRender()
            }
        }
    }

    /**
     * Recalcula pendientes en Room y muestra:
     * - si auto-sync ON → ocultar warning y deshabilitar botón
     * - si auto-sync OFF → mostrar warning+botón solo si hay pendientes
     */
    private fun refreshPendingAndRender() {
        val autoSyncEnabled = prefs.getBoolean(KEY_AUTO_SYNC, true)

        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            renderManualState(autoSyncEnabled, hasPending = false)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val pendingCount = try {
                userGamesRepo.countPending(uid)
            } catch (_: Exception) {
                0
            }

            val hasPending = pendingCount > 0
            prefs.edit { putBoolean(KEY_HAS_PENDING, hasPending) }

            renderManualState(autoSyncEnabled, hasPending)
        }
    }

    /**
     * Renderiza el estado de sincronización.
     * @return Instancia de [SyncFragment].
     * @throws Exception si ocurre un error al cargar las preferencias.
     */
    private fun renderManualState(autoSyncEnabled: Boolean, hasPending: Boolean) {
        if (autoSyncEnabled) {
            tvManualWarning.visibility = View.GONE
            btnSyncNow.isEnabled = false
        } else {
            if (hasPending) {
                tvManualWarning.visibility = View.VISIBLE
                btnSyncNow.isEnabled = true
            } else {
                tvManualWarning.visibility = View.GONE
                btnSyncNow.isEnabled = false
            }
        }
    }

    /**
     * Formatea un timestamp en texto legible. Si no hay timestamp, devuelve "Nunca".
     */
    private fun formatDateOrNever(timestamp: Long): String {
        if (timestamp <= 0L) return getString(R.string.profile_sync_never)

        val df = DateFormat.getDateTimeInstance(
            DateFormat.SHORT,
            DateFormat.SHORT
        )
        return df.format(Date(timestamp))
    }

    /**
     * Claves de SharedPreferences
     * @return Instancia de [SyncFragment].
     * @throws Exception si ocurre un error al cargar las preferencias.
     */
    companion object {
        private const val PREFS_NAME = "sync_prefs"

        // Claves de SharedPreferences
        const val KEY_AUTO_SYNC = "auto_sync_enabled"
        const val KEY_LAST_CATALOG_SYNC = "last_catalog_sync"
        const val KEY_LAST_LIBRARY_SYNC = "last_library_sync"
        const val KEY_HAS_PENDING = "has_pending_manual_sync"
    }
}