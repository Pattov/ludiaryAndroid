package com.ludiary.android.ui.profile.friends

import android.os.Bundle
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import com.ludiary.android.R
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.model.FriendsTab
import com.ludiary.android.data.repository.FriendsRepositoryImpl
import com.ludiary.android.viewmodel.FriendsViewModel
import com.ludiary.android.viewmodel.FriendsViewModelFactory

class FriendsFragment : Fragment(R.layout.form_friends_profile) {

    private lateinit var vm: FriendsViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = requireContext().applicationContext
        val db = LudiaryDatabase.getInstance(ctx)
        val repo = FriendsRepositoryImpl(db.friendDao())
        vm = ViewModelProvider(this, FriendsViewModelFactory(repo))[FriendsViewModel::class.java]

        val topAppBar: MaterialToolbar = view.findViewById(R.id.topAppBar)
        val tabLayout: TabLayout = view.findViewById(R.id.tabFriends)
        val pager: ViewPager2 = view.findViewById(R.id.pagerFriends)
        val search: TextInputEditText = view.findViewById(R.id.textSearchFriends)
        val fab: ExtendedFloatingActionButton = view.findViewById(R.id.fabFriends)

        topAppBar.setNavigationOnClickListener { findNavController().popBackStack() }

        pager.adapter = FriendsPagerAdapter(this)

        TabLayoutMediator(tabLayout, pager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.profile_friends_tab_friends)               // Amigos
                1 -> getString(R.string.profile_friends_tab_groups)          // Grupos
                else -> getString(R.string.profile_friends_tab_requests)   // Solicitudes
            }
        }.attach()

        fun updateFabFor(position: Int) {
            when (position) {
                0 -> { // Amigos
                    fab.visibility = View.VISIBLE
                    fab.setText(R.string.profile_friends_add_friend)
                    fab.setOnClickListener { vm.onAddFriendClicked() }
                }
                1 -> { // Grupos
                    fab.visibility = View.VISIBLE
                    fab.setText(R.string.profile_friends_add_group)
                    fab.setOnClickListener { vm.onAddGroupClicked() }
                }
                else -> { // Solicitudes
                    fab.visibility = View.GONE
                    fab.setOnClickListener(null)
                }
            }
        }

        updateFabFor(0)

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateFabFor(position)
            }
        })

        search.addTextChangedListener { text ->
            vm.onQueryChanged(text?.toString().orEmpty())
        }
    }

    private class FriendsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            val tab = when (position) {
                0 -> FriendsTab.FRIENDS
                1 -> FriendsTab.GROUPS
                else -> FriendsTab.REQUESTS
            }
            return FriendsListFragment.newInstance(tab)
        }
    }
}