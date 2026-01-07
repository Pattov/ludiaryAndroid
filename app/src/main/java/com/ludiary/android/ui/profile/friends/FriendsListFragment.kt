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
        FriendsTab.valueOf(requireArguments().getString(ARG_TAB)!!)
    }

    private val vm: FriendsViewModel by lazy {
        ViewModelProvider(requireParentFragment())[FriendsViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerFriends)
        val empty = view.findViewById<TextView>(R.id.tvEmptyFriends)

        empty.text = when (tab) {
            FriendsTab.FRIENDS -> getString(R.string.profile_friends_empty_friends)
            FriendsTab.GROUPS -> getString(R.string.profile_friends_empty_groups)
            FriendsTab.REQUESTS -> getString(R.string.profile_friends_empty_requests)
        }

        val adapter = FriendsAdapter(onClick = { vm.onFriendClicked(it) })
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            vm.items(tab).collectLatest { items ->
                adapter.submitList(items)
                empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
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