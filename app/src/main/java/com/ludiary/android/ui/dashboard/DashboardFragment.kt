package com.ludiary.android.ui.dashboard

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.ludiary.android.R

/**
 * Fragment principal del Dashboard de la aplicación Ludiary.
 *
 * Actúa como la pantalla inicial tras el inicio de sesión, mostrando un resumen general de la
 * actividad lúdica del usuario.
 */
class DashboardFragment : Fragment (R.layout.fragment_placeholder) {

    /**
     * Metodo del ciclo de vida del fragment.
     * Se ejecuta cuando la vista ya ha sido creada y asociada al layout.
     *
     * @param view Vista raíz del fragment.
     * @param savedInstanceState Esta guardado.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tvPlaceholder).text = "Resumen de actividad lúdica"
    }
}