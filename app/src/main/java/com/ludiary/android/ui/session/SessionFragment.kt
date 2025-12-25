package com.ludiary.android.ui.session

import SessionsAdapter
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.ludiary.android.R
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.viewmodel.SessionsViewModel
import com.ludiary.android.viewmodel.SessionsViewModelFactory

class SessionFragment : Fragment(R.layout.fragment_sessions) {

    private lateinit var vm: SessionsViewModel
    private lateinit var adapter: SessionsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = LudiaryDatabase.getInstance(requireContext())
        val auth = FirebaseAuth.getInstance()
        vm = SessionsViewModelFactory(db, auth).create(SessionsViewModel::class.java)

        adapter = SessionsAdapter { session ->

            val args = Bundle().apply { putString("sessionId", session.id) }
        }
    }
}
