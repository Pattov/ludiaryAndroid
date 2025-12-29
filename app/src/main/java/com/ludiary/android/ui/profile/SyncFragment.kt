package com.ludiary.android.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import com.ludiary.android.R
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.LocalUserGamesDataSource
import com.ludiary.android.data.repository.FirestoreUserGamesRepository
import com.ludiary.android.data.repository.UserGamesRepository
import com.ludiary.android.data.repository.UserGamesRepositoryImpl
import com.ludiary.android.sync.SyncPrefs
import com.ludiary.android.sync.SyncScheduler
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Fragmento que muestra la pantalla de sincronización del perfil.
 * @param R.layout.form_sync_profile layout de este fragmento.
 */
class SyncFragment : Fragment(R.layout.form_sync_profile) {

    private lateinit var switchAutoSync: MaterialSwitch
    private lateinit var tvLastCatalogSync: TextView
    private lateinit var tvLastLibrarySync: TextView
    private lateinit var tvManualWarning: TextView
    private lateinit var btnSyncNow: MaterialButton

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val syncPrefs by lazy { SyncPrefs(requireContext().applicationContext) }

    /**
     * Repositorio de juegos del usuario. No se utiliza para ejecutar sincronizaciones reales desde la UI.
     * Unicamente se usa para consultar el número de elementos pendientes de sincronización.
     * @return Instancia de [UserGamesRepository].
     */
    private val userGamesRepo: UserGamesRepository by lazy {
        val ctx = requireContext().applicationContext
        val db = LudiaryDatabase.getInstance(ctx)
        val local = LocalUserGamesDataSource(db.userGameDao())
        val remote = FirestoreUserGamesRepository(com.google.firebase.firestore.FirebaseFirestore.getInstance())
        UserGamesRepositoryImpl(local, remote)
    }

    /**
     * Se ejecuta cuando se crea la vista del fragmento.
     * @param view Vista del fragmento.
     * @param savedInstanceState Estado de la instancia.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        bindInitialState()
        bindListeners()
        refreshPendingAndRender()
    }

    /**
     * Enlaza las vistas del layout con las propiedades del fragmento.
     * @param view Vista del fragmento.
     */
    private fun bindViews(view: View) {
        switchAutoSync = view.findViewById(R.id.switchAutoSync)
        tvLastCatalogSync = view.findViewById(R.id.tvLastCatalogSync)
        tvLastLibrarySync = view.findViewById(R.id.tvLastLibrarySync)
        tvManualWarning = view.findViewById(R.id.tvManualWarning)
        btnSyncNow = view.findViewById(R.id.btnSyncNow)
    }

    /**
     * Aplica el estado inicial de las preferencias de sincronización.
     */
    private fun bindInitialState() {
        val autoEnabled = syncPrefs.isAutoSyncEnabled()

        switchAutoSync.isChecked = autoEnabled
        tvLastCatalogSync.text = formatDateOrNever(syncPrefs.getLastCatalogSyncMillis())
        tvLastLibrarySync.text = formatDateOrNever(syncPrefs.getLastLibrarySyncMillis())

        renderManualState(autoEnabled, hasPending = false)
    }

    /**
     * Configura los listeners de la UI.
     */
    private fun bindListeners() {
        switchAutoSync.setOnCheckedChangeListener { _, isChecked ->
            val ctx = requireContext().applicationContext

            syncPrefs.setAutoSyncEnabled(isChecked)

            if (isChecked) {
                SyncScheduler.enableAutoSyncUserGames(ctx)
                SyncScheduler.enableAutoSyncSessions(ctx)
            } else {
                SyncScheduler.disableAutoSyncUserGames(ctx)
                SyncScheduler.disableAutoSyncSessions(ctx)
            }

            refreshPendingAndRender()
        }

        btnSyncNow.setOnClickListener {
            val ctx = requireContext().applicationContext
            val uid = auth.currentUser?.uid

            if (uid.isNullOrBlank()) {
                requireContext().getString(R.string.profile_sync_login_required).also {
                    android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }

            SyncScheduler.enqueueOneTimeUserGamesSync(ctx)
            SyncScheduler.enqueueOneTimeSessionsSync(ctx)

            android.widget.Toast.makeText(
                requireContext(),
                getString(R.string.profile_sync_started),
                android.widget.Toast.LENGTH_SHORT
            ).show()

            refreshPendingAndRender()
        }
    }

    /**
     * Recalcular el número de elementos pendientes de sincronización.
     */
    private fun refreshPendingAndRender() {
        val autoEnabled = syncPrefs.isAutoSyncEnabled()
        val uid = auth.currentUser?.uid

        if (uid.isNullOrBlank()) {
            renderManualState(autoEnabled, hasPending = false)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val pendingCount = runCatching { userGamesRepo.countPending(uid) }.getOrElse { 0 }
            val hasPending = pendingCount > 0
            renderManualState(autoEnabled, hasPending)
        }
    }

    /**
     * Renderiza el estado de la sincronización manual.
     * @param autoSyncEnabled Estado de sincronización automática.
     * @param hasPending Si hay elementos pendientes de sincronización.
     */
    private fun renderManualState(autoSyncEnabled: Boolean, hasPending: Boolean) {
        if (autoSyncEnabled) {
            tvManualWarning.visibility = View.GONE
            btnSyncNow.isEnabled = false
            return
        }

        if (hasPending) {
            tvManualWarning.visibility = View.VISIBLE
            btnSyncNow.isEnabled = true
        } else {
            tvManualWarning.visibility = View.GONE
            btnSyncNow.isEnabled = false
        }
    }

    /**
     * Formatea una fecha o devuelve "Nunca".
     * @param timestamp Fecha en milisegundos.
     */
    private fun formatDateOrNever(timestamp: Long): String {
        if (timestamp <= 0L) return getString(R.string.profile_sync_never)

        val df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        return df.format(Date(timestamp))
    }
}
