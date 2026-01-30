package com.ludiary.android.ui.profile.social

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

/**
 * Fragment de lista para el módulo de Amigos.
 * Este fragment obtiene el [FriendsViewModel] desde el fragment padre (normalmente [FriendsFragment]) para compartir estado y acciones entre pestañas.
 */
class FriendsListFragment : Fragment(R.layout.fragment_friends_list) {

    private val tab: FriendsTab by lazy {
        FriendsTab.valueOf(requireArguments().getString(ARG_TAB) ?: FriendsTab.FRIENDS.name)
    }

    /**
     * ViewModel compartido con el fragment padre.
     * Se usa [requireParentFragment] para que las 3 listas (tabs) compartan el mismo VM.
     */
    private val vm: FriendsViewModel by lazy {
        ViewModelProvider(requireParentFragment())[FriendsViewModel::class.java]
    }

    /**
     * Configura el RecyclerView y conecta el adapter adecuado según [tab].
     * @param view Vista del fragmento
     * @param savedInstanceState Estado de la instancia
     */
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
                    onRejectGroup = { inviteId -> vm.rejectGroupInvite(inviteId) },
                    onCancelGroup = { inviteId -> vm.cancelGroupInvite(inviteId) }
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
                                .newInstance(
                                    groupId = group.groupId,
                                    groupName = group.nameSnapshot,
                                    memberUids = members.map { it.uid },
                                    membersCount = row.membersCount
                                )
                                .show(parentFragmentManager, "InviteFriendsBottomSheet")
                        }
                    },
                    onDelete = { row ->
                        val group = row.group
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.groups_leave_title)
                            .setMessage(getString(R.string.groups_leave_message, group.nameSnapshot))
                            .setPositiveButton(R.string.action_leave_confirm) { _, _ ->
                                vm.leaveGroup(group.groupId)
                            }
                            .setNegativeButton(R.string.action_cancel, null)
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

        /**
         * Crea una instancia del fragment configurada para una pestaña concreta.
         * @param tab Pestaña que se quiere renderizar.
         */
        fun newInstance(tab: FriendsTab) = FriendsListFragment().apply {
            arguments = bundleOf(ARG_TAB to tab.name)
        }
    }
}