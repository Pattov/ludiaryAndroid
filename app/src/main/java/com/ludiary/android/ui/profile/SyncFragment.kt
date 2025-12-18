package com.ludiary.android.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.R
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.LocalUserGamesDataSource
import com.ludiary.android.data.repository.FirestoreUserGamesRepository
import com.ludiary.android.data.repository.UserGamesRepository
import com.ludiary.android.data.repository.UserGamesRepositoryImpl
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import androidx.core.content.edit
import com.ludiary.android.sync.SyncScheduler

/**
 * Fragmento para sincronización de juegos del usuario.
 * Permite activar/desactivar la sincronización, ofrecer sincronización manual y mostrar la información del usuario.
 */
class SyncFragment : Fragment(R.layout.form_sync_profile) {

    private lateinit var switchAutoSync: SwitchCompat
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

    private val auth by lazy { FirebaseAuth.getInstance() }

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
        // Estado inicial desde SharedPreferences
        val autoSyncEnabled = prefs.getBoolean(KEY_AUTO_SYNC, true)
        val lastCatalogSync = prefs.getLong(KEY_LAST_CATALOG_SYNC, 0L)
        val lastLibrarySync = prefs.getLong(KEY_LAST_LIBRARY_SYNC, 0L)

        switchAutoSync.isChecked = autoSyncEnabled
        tvLastCatalogSync.text = formatDateOrNever(lastCatalogSync)
        tvLastLibrarySync.text = formatDateOrNever(lastLibrarySync)

        // Al entrar, recalculamos pendientes desde Room para mostrar warning/botón correctamente.
        refreshPendingAndRender()

        // Activar/desactivar sync automática
        switchAutoSync.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit {
                putBoolean(KEY_AUTO_SYNC, isChecked)
            }

            if(isChecked) {
                SyncScheduler.enableAutoSync(requireContext().applicationContext)
            } else {
                SyncScheduler.disableAutoSync(requireContext().applicationContext)
            }

            refreshPendingAndRender()
        }

        // Sincronización manual bajo demanda
        btnSyncNow.setOnClickListener {
            val uid = auth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Necesitas iniciar sesión para sincronizar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Evita dobles toques mientras se ejecuta la operación
            btnSyncNow.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val synced = userGamesRepo.syncPending(uid)

                    // Actualizar fecha de última sync de ludoteca si se sincronizó algo
                    if (synced > 0) {
                        val now = System.currentTimeMillis()
                        prefs.edit {
                            putLong(KEY_LAST_LIBRARY_SYNC, now)
                        }
                        tvLastLibrarySync.text = formatDateOrNever(now)
                    }

                    Toast.makeText(
                        requireContext(),
                        "Sincronización realizada (${synced})",
                        Toast.LENGTH_SHORT
                    ).show()

                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Error al sincronizar: ${e.message ?: "desconocido"}",
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    refreshPendingAndRender()
                }
            }
        }
    }

    /**
     * Actualiza el estado de sincronización.
     * @return Instancia de [SyncFragment].
     * @throws Exception si ocurre un error al cargar las preferencias.
     */
    private fun refreshPendingAndRender() {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            // Sin usuario: no podemos sincronizar contra Firestore
            val autoSyncEnabled = prefs.getBoolean(KEY_AUTO_SYNC, true)
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
            prefs.edit {
                putBoolean(KEY_HAS_PENDING, hasPending)
            }

            val autoSyncEnabled = prefs.getBoolean(KEY_AUTO_SYNC, true)
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

    companion object {
        private const val PREFS_NAME = "sync_prefs"

        // Claves de SharedPreferences
        const val KEY_AUTO_SYNC = "auto_sync_enabled"
        const val KEY_LAST_CATALOG_SYNC = "last_catalog_sync"
        const val KEY_LAST_LIBRARY_SYNC = "last_library_sync"
        const val KEY_HAS_PENDING = "has_pending_manual_sync"
    }
}