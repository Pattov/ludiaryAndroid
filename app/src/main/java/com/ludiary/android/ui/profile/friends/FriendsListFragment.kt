package com.ludiary.android.ui.profile.friends

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ludiary.android.R
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.repository.FriendsRepositoryImpl
import com.ludiary.android.data.repository.GroupsRepositoryImpl
import com.ludiary.android.data.local.LocalFriendsDataSource
import com.ludiary.android.data.model.FriendsTab
import com.ludiary.android.viewmodel.FriendsViewModel
import com.ludiary.android.viewmodel.FriendsViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.repository.FirestoreFriendsRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FriendsListFragment : Fragment(R.layout.fragment_friends_list) {

    private val tab: FriendsTab by lazy {
        FriendsTab.valueOf(requireArguments().getString(ARG_TAB) ?: FriendsTab.FRIENDS.name)
    }

    private lateinit var vm: FriendsViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- VM (mismo patrón que FriendsFragment) ---
        val ctx = requireContext().applicationContext
        val db = LudiaryDatabase.getInstance(ctx)

        val local = LocalFriendsDataSource(db.friendDao())
        val remote = FirestoreFriendsRepository(FirebaseFirestore.getInstance())
        val friendsRepo = FriendsRepositoryImpl(local, remote, FirebaseAuth.getInstance())

        val groupsRepo = GroupsRepositoryImpl(
            db = db,
            fs = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance()
        )

        vm = ViewModelProvider(
            requireActivity(),
            FriendsViewModelFactory(friendsRepo, groupsRepo)
        )[FriendsViewModel::class.java]

        // --- UI ---
        val recycler: RecyclerView = view.findViewById(R.id.recyclerFriends)
        val empty: TextView = view.findViewById(R.id.tvEmptyFriends)

        recycler.layoutManager = LinearLayoutManager(requireContext())

        when (tab) {
            FriendsTab.REQUESTS -> {
                val adapter = RequestsAdapter(
                    onClick = { /* opcional */ },
                    onAccept = { friendId -> vm.acceptRequest(friendId) },
                    onReject = { friendId -> vm.rejectRequest(friendId) }
                )
                recycler.adapter = adapter

                viewLifecycleOwner.lifecycleScope.launch {
                    vm.requestRows().collectLatest { rows ->
                        adapter.submitList(rows)
                        empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }

            FriendsTab.GROUPS -> {
                val adapter = GroupsAdapter(
                    onOpen = { group ->
                        findNavController().navigate(
                            R.id.nav_groupDetailFragment,
                            bundleOf(
                                "groupId" to group.groupId,
                                "groupName" to group.nameSnapshot
                            )
                        )
                    },
                    onInvite = { group ->
                        // Abrir directamente el BottomSheet desde la lista
                        viewLifecycleOwner.lifecycleScope.launch {
                            val members = vm.groupMembersOnce(group.groupId)
                            InviteFriendsBottomSheet
                                .newInstance(group.groupId, group.nameSnapshot, members.map { it.uid })
                                .show(parentFragmentManager, "InviteFriendsBottomSheet")
                        }
                    },
                    onDelete = { group ->
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Salir del grupo")
                            .setMessage("¿Quieres salir de “${group.nameSnapshot}”?")
                            .setPositiveButton("Salir") { _, _ ->
                                vm.leaveGroup(group.groupId)
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }
                )

                recycler.adapter = adapter

                viewLifecycleOwner.lifecycleScope.launch {
                    vm.groupItems().collectLatest { groups ->
                        adapter.submitList(groups)
                        empty.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }

            else -> {
                val adapter = FriendsAdapter(
                    onClick = { vm.onFriendClicked(it) },
                    onEditNickname = { friendId, currentNickname ->
                        vm.editNickname(friendId, currentNickname)
                    },
                    onDeleteFriend = { friendId ->
                        vm.removeFriend(friendId)
                    }
                )
                recycler.adapter = adapter

                viewLifecycleOwner.lifecycleScope.launch {
                    vm.items(tab).collectLatest { items ->
                        adapter.submitList(items)
                        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    companion object {
        private const val ARG_TAB = "tab"

        fun newInstance(tab: FriendsTab) = FriendsListFragment().apply {
            arguments = bundleOf(ARG_TAB to tab.name)
        }
    }
}