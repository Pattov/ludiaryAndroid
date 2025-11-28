package com.ludiary.android.ui.library

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ludiary.android.R
import com.ludiary.android.data.repository.FakeUserGamesRepository
import com.ludiary.android.viewmodel.LibraryViewModel
import com.ludiary.android.viewmodel.LibraryViewModelFactory
import kotlinx.coroutines.launch

class LibraryFragment : Fragment(R.layout.fragment_library) {
    private val viewModel: LibraryViewModel by viewModels{
        LibraryViewModelFactory(
            uid = "TEST_UID",
            repository = FakeUserGamesRepository()
        )
    }

    private lateinit var adapter: UserGameAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = UserGameAdapter(
            onEdit = { gameId ->
                findNavController().navigate(
                    R.id.nav_edit_user_game,
                    bundleOf("gameId" to gameId)
                )
            },
            onDelete = { gameId -> viewModel.onDeleteGameClicked(gameId) }
        )

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerLibrary)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val fab = view.findViewById<FloatingActionButton>(R.id.fabAddGame)
        fab.setOnClickListener {
            findNavController().navigate(R.id.nav_edit_user_game)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { ui ->
                adapter.submitList(ui.games)
            }
        }

    }
}