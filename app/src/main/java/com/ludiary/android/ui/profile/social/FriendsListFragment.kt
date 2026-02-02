package com.ludiary.android.ui.profile.social

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.ludiary.android.R
import com.ludiary.android.data.model.FriendsTab
import com.ludiary.android.viewmodel.FriendsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Fragment de lista para el módulo de Amigos.
 * Este fragment obtiene el [FriendsViewModel] desde el fragment padre para compartir estado entre pestañas.
 */
class FriendsListFragment : Fragment(R.layout.fragment_friends_list) {

    private val tab: FriendsTab by lazy {
        FriendsTab.valueOf(requireArguments().getString(ARG_TAB) ?: FriendsTab.FRIENDS.name)
    }

    private val vm: FriendsViewModel by lazy {
        ViewModelProvider(requireParentFragment())[FriendsViewModel::class.java]
    }

    private val queryFlow = MutableStateFlow("")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler: RecyclerView = view.findViewById(R.id.recyclerFriends)
        val empty: TextView = view.findViewById(R.id.tvEmptyFriends)
        val defaultEmptyText = empty.text // guardamos el texto original del XML

        recycler.layoutManager = LinearLayoutManager(requireContext())

        bindSearchFromParent()

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
                    combine(vm.requestRows(), queryFlow) { rows, q ->
                        val query = q.trim()
                        if (query.isEmpty()) rows
                        else rows.filter { matchesQuery(it, query) }
                    }.collectLatest { filtered ->
                        adapter.submitList(filtered)
                        renderEmptyState(empty, defaultEmptyText, filtered.isEmpty())
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
                    combine(vm.groupRows(), queryFlow) { rows, q ->
                        val query = q.trim()
                        if (query.isEmpty()) rows
                        else rows.filter { matchesQuery(it, query) }
                    }.collectLatest { filtered ->
                        adapter.submitList(filtered)
                        renderEmptyState(empty, defaultEmptyText, filtered.isEmpty())
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
                    combine(vm.items(tab), queryFlow) { items, q ->
                        val query = q.trim()
                        if (query.isEmpty()) items
                        else items.filter { matchesQuery(it, query) }
                    }.collectLatest { filtered ->
                        adapter.submitList(filtered)
                        renderEmptyState(empty, defaultEmptyText, filtered.isEmpty())
                    }
                }
            }
        }
    }

    /**
     * Lee el EditText de búsqueda desde el fragment padre y alimenta [queryFlow].
     * Si no existe el id, simplemente no hace nada (no crashea).
     */
    private fun bindSearchFromParent() {
        val parentView = try {
            requireParentFragment().view
        } catch (_: Exception) {
            null
        } ?: return

        val search = parentView.findViewById<TextInputEditText?>(R.id.textSearch) ?: return

        queryFlow.value = search.text?.toString().orEmpty()

        search.doAfterTextChanged { editable ->
            queryFlow.value = editable?.toString().orEmpty()
        }
    }

    /**
     * Estado vacío:
     * - si hay búsqueda -> "No existen coincidencias con “X”"
     * - si no hay búsqueda -> texto por defecto del XML
     */
    private fun renderEmptyState(empty: TextView, defaultText: CharSequence, isEmpty: Boolean) {
        if (!isEmpty) {
            empty.visibility = View.GONE
            return
        }

        val q = queryFlow.value.trim()
        empty.visibility = View.VISIBLE

        empty.text = if (q.isNotEmpty()) {
            getString(R.string.search_no_results, q)
        } else {
            defaultText
        }
    }

    /**
     * Intento “robusto” de buscar un nombre sin conocer exactamente el modelo.
     * Busca en campos típicos: nameSnapshot, displayName, nickname, email, title, etc.
     */
    private fun matchesQuery(item: Any, query: String): Boolean {
        val haystack = buildString {
            append(readStringField(item, "nameSnapshot"))
            append(" ")
            append(readStringField(item, "displayName"))
            append(" ")
            append(readStringField(item, "nickname"))
            append(" ")
            append(readStringField(item, "email"))
            append(" ")
            append(readStringField(item, "username"))
            append(" ")
            append(readStringField(item, "name"))
            append(" ")
            append(readStringField(item, "title"))
        }.trim()

        val text = haystack.ifEmpty { item.toString() }
        return text.contains(query, ignoreCase = true)
    }

    private fun readStringField(target: Any, fieldName: String): String {
        return try {
            val f = target::class.java.getDeclaredField(fieldName)
            f.isAccessible = true
            (f.get(target) as? String).orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    companion object {
        private const val ARG_TAB = "tab"

        fun newInstance(tab: FriendsTab) = FriendsListFragment().apply {
            arguments = bundleOf(ARG_TAB to tab.name)
        }
    }
}
