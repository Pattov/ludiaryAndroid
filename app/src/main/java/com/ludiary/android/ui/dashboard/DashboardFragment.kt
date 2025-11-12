package com.ludiary.android.ui.dashboard

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.ludiary.android.R

class DashboardFragment : Fragment (R.layout.fragment_placeholder) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tvPlaceholder).text = "Resumen de actividad lúdica"
    }
}