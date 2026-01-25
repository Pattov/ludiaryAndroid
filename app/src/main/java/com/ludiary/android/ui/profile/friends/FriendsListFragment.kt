package com.ludiary.android.ui.profile.friends

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
import com.ludiary.android.data.model.FriendsTab
import com.ludiary.android.viewmodel.FriendsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FriendsListFragment : Fragment(R.layout.fragment_friends_list) {

    private val tab: FriendsTab by lazy {
        FriendsTab.valueOf(requireArguments().getString(ARG_TAB) ?: FriendsTab.FRIENDS.name)
    }

    private val vm: FriendsViewModel by lazy {
        ViewModelProvider(requireParentFragment())[FriendsViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler: RecyclerView = view.findViewById(R.id.recyclerFriends)
        val empty: TextView = view.findViewById(R.id.tvEmptyFriends)

        recycler.layoutManager = LinearLayoutManager(requireContext())

        when (tab) {
            FriendsTab.REQUESTS -> {
                val adapter = RequestsAdapter(
                    onFriendClick = { /* opcional */ },
                    onAcceptFriend = { friendId -> vm.acceptRequest(friendId) },
                    onRejectFriend = { friendId -> vm.rejectRequest(friendId) },
                    onAcceptGroup = { inviteId -> vm.acceptGroupInvite(inviteId) },
                    onRejectGroup = { inviteId -> vm.rejectGroupInvite(inviteId) }
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
                    onOpen = { row ->
                        val group = row.group
                        findNavController().navigate(
                            R.id.nav_groupDetailFragment,
                            bundleOf(
                                "groupId" to group.groupId,
                                "groupName" to group.nameSnapshot
                            )
                        )
                    },
                    onInvite = { row ->
                        val group = row.group
                        viewLifecycleOwner.lifecycleScope.launch {
                            val members = vm.groupMembersOnce(group.groupId)
                            InviteFriendsBottomSheet
                                .newInstance(group.groupId, group.nameSnapshot, members.map { it.uid })
                                .show(parentFragmentManager, "InviteFriendsBottomSheet")
                        }
                    },
                    onDelete = { row ->
                        val group = row.group
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
                    vm.groupRows().collectLatest { rows ->
                        adapter.submitList(rows)
                        empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
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