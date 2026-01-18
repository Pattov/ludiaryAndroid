package com.ludiary.android.ui.profile.friends

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        if (tab == FriendsTab.REQUESTS) {
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
        } else {
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

    companion object {
        private const val ARG_TAB = "tab"

        fun newInstance(tab: FriendsTab) = FriendsListFragment().apply {
            arguments = bundleOf(ARG_TAB to tab.name)
        }
    }
}