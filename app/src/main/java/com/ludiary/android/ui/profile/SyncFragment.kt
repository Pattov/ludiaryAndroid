package com.ludiary.android.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ludiary.android.R
import java.text.DateFormat
import java.util.Date

class SyncFragment : Fragment(R.layout.form_sync_profile) {

    private lateinit var switchAutoSync: Switch
    private lateinit var tvLastCatalogSync: TextView
    private lateinit var tvLastLibrarySync: TextView
    private lateinit var tvManualWarning: TextView
    private lateinit var btnSyncNow: Button

    private val prefs by lazy {
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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

    private fun setupUi() {
        // Cargar estado inicial desde SharedPreferences
        val autoSyncEnabled = prefs.getBoolean(KEY_AUTO_SYNC, true)
        val lastCatalogSync = prefs.getLong(KEY_LAST_CATALOG_SYNC, 0L)
        val lastLibrarySync = prefs.getLong(KEY_LAST_LIBRARY_SYNC, 0L)
        val hasPending = prefs.getBoolean(KEY_HAS_PENDING, false)

        switchAutoSync.isChecked = autoSyncEnabled
        tvLastCatalogSync.text = formatDateOrNever(lastCatalogSync)
        tvLastLibrarySync.text = formatDateOrNever(lastLibrarySync)

        renderManualState(autoSyncEnabled, hasPending)

        // Cambios en el switch
        switchAutoSync.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit()
                .putBoolean(KEY_AUTO_SYNC, isChecked)
                .apply()

            val pending = prefs.getBoolean(KEY_HAS_PENDING, false)
            renderManualState(isChecked, pending)
        }

        // Botón de sincronización manual
        btnSyncNow.setOnClickListener {
            // Evitamos toques repetidos
            btnSyncNow.isEnabled = false

            // Aquí deberías llamar a TU lógica real de sincronización.
            // Por ejemplo: sincronizar catálogo, ludoteca, etc.
            //
            // TODO:
            //  - Llamar a tu repositorio / viewmodel de sync
            //  - Manejar errores correctamente
            //
            // De momento, simulamos éxito inmediato:
            Toast.makeText(requireContext(), "Sincronización realizada", Toast.LENGTH_SHORT).show()

            val now = System.currentTimeMillis()

            // Actualizamos meta-datos de sync
            prefs.edit()
                .putLong(KEY_LAST_CATALOG_SYNC, now)
                .putLong(KEY_LAST_LIBRARY_SYNC, now)
                .putBoolean(KEY_HAS_PENDING, false)
                .apply()

            // Refrescamos UI
            tvLastCatalogSync.text = formatDateOrNever(now)
            tvLastLibrarySync.text = formatDateOrNever(now)

            val autoSync = prefs.getBoolean(KEY_AUTO_SYNC, true)
            renderManualState(autoSync, hasPending = false)
        }
    }

    private fun renderManualState(autoSyncEnabled: Boolean, hasPending: Boolean) {
        if (autoSyncEnabled) {
            // Si es automático, no mostramos warning ni dejamos el botón condicionado
            tvManualWarning.visibility = View.GONE
            btnSyncNow.isEnabled = false
        } else {
            // Modo manual
            if (hasPending) {
                tvManualWarning.visibility = View.VISIBLE
                btnSyncNow.isEnabled = true
            } else {
                tvManualWarning.visibility = View.GONE
                btnSyncNow.isEnabled = false
            }
        }
    }

    private fun formatDateOrNever(timestamp: Long): String {
        if (timestamp <= 0L) {
            return getString(R.string.profile_sync_never)
        }
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
