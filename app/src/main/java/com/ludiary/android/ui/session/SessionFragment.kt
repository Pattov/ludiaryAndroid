package com.ludiary.android.ui.session

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.ludiary.android.R
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.viewmodel.SessionsViewModel
import com.ludiary.android.viewmodel.SessionsViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SessionFragment : Fragment(R.layout.fragment_sessions) {

    private lateinit var vm: SessionsViewModel
    private lateinit var adapter: SessionsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler: RecyclerView = view.findViewById(R.id.recyclerSession)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        val db = LudiaryDatabase.getInstance(requireContext().applicationContext)
        val auth = FirebaseAuth.getInstance()
        vm = SessionsViewModelFactory(db, auth).create(SessionsViewModel::class.java)

        adapter = SessionsAdapter(
            onItemClick = { session ->
                // TODO: abrir editar
            },
            onEditClick = { session ->
                // TODO: abrir editar
            },
            onDeleteClick = { session ->
                vm.deleteSession(session.id)
            }
        )
        recycler.adapter = adapter

        vm.start()

        viewLifecycleOwner.lifecycleScope.launch {
            vm.uiState.collectLatest { state ->
                adapter.submitList(state.sessions)
            }
        }
    }
}
