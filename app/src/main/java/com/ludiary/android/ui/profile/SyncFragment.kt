package com.ludiary.android.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.R
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.LocalUserGamesDataSource
import com.ludiary.android.data.repository.FirestoreUserGamesRepository
import com.ludiary.android.data.repository.UserGamesRepository
import com.ludiary.android.data.repository.UserGamesRepositoryImpl
import com.ludiary.android.sync.SyncStatusPrefs
import com.ludiary.android.viewmodel.SyncViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Fragmento que muestra la pantalla de sincronización del perfil.
 * @param R.layout.form_sync_profile layout de este fragmento.
 */
class SyncFragment : Fragment(R.layout.form_sync_profile) {

    private lateinit var switchAutoSync: MaterialSwitch
    private lateinit var tvLastSync: TextView
    private lateinit var tvManualWarning: TextView
    private lateinit var btnSyncNow: MaterialButton

    private lateinit var vm: SyncViewModel

    /**
     * Se ejecuta cuando se crea la vista del fragmento.
     * @param view Vista del fragmento.
     * @param savedInstanceState Estado de la instancia.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupViewModel()
        bindListeners()
        observeUi()

        vm.start()
    }

    /**
     * Enlaza las vistas del layout con las propiedades del fragmento.
     * @param view Vista del fragmento.
     */
    private fun bindViews(view: View) {
        switchAutoSync = view.findViewById(R.id.switchAutoSync)
        tvLastSync = view.findViewById(R.id.tvLastSync)
        tvManualWarning = view.findViewById(R.id.tvManualWarning)
        btnSyncNow = view.findViewById(R.id.btnSyncNow)
    }

    /**
     * Observa el estado de la UI.
     */
    private fun setupViewModel() {
        val ctx = requireContext().applicationContext
        val auth = FirebaseAuth.getInstance()

        val userGamesRepo: UserGamesRepository = run {
            val db = LudiaryDatabase.getInstance(ctx)
            val local = LocalUserGamesDataSource(db.userGameDao())
            val remote = FirestoreUserGamesRepository(FirebaseFirestore.getInstance())
            UserGamesRepositoryImpl(local, remote)
        }

        val statusPrefs = SyncStatusPrefs(ctx)

        vm = SyncViewModel(
            appContext = ctx,
            auth = auth,
            userGamesRepo = userGamesRepo,
            statusPrefs = statusPrefs
        )
    }

    /**
     * Configura los listeners de la UI.
     */
    private fun bindListeners() {
        switchAutoSync.setOnCheckedChangeListener { _, isChecked ->
            vm.onToggleAutoSync(isChecked)
        }

        btnSyncNow.setOnClickListener {
            vm.onSyncNowClicked()
        }
    }

    /**
     * Observa el estado de la UI.
     */
    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            vm.ui.collectLatest { state ->
                if (switchAutoSync.isChecked != state.autoSyncEnabled) {
                    switchAutoSync.isChecked = state.autoSyncEnabled
                }

                tvLastSync.text = formatDateOrNever(state.lastSyncMillis)

                renderManualState(
                    autoSyncEnabled = state.autoSyncEnabled,
                    syncNowEnabled = state.syncNowEnabled
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.events.collectLatest { event ->
                when (event) {
                    is SyncViewModel.UiEvent.Toast -> {
                        android.widget.Toast.makeText(
                            requireContext(),
                            event.message,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    /**
     * Renderiza el estado de la sincronización manual.
     * @param autoSyncEnabled Estado de sincronización automática.
     * @param syncNowEnabled Estado de la sincronización manual.
     */
    private fun renderManualState(autoSyncEnabled: Boolean, syncNowEnabled: Boolean) {
        if (autoSyncEnabled) {
            tvManualWarning.visibility = View.GONE
            btnSyncNow.isEnabled = false
            return
        }

        btnSyncNow.isEnabled = syncNowEnabled
        tvManualWarning.visibility = if (syncNowEnabled) View.VISIBLE else View.GONE
    }

    private fun formatDateOrNever(timestamp: Long): String {
        if (timestamp <= 0L) return getString(R.string.profile_sync_never)

        val df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        return df.format(Date(timestamp))
    }
}
