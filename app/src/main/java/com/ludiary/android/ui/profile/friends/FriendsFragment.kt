package com.ludiary.android.ui.profile.friends

import android.os.Bundle
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.R
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.LocalFriendsDataSource
import com.ludiary.android.data.model.FriendsTab
import com.ludiary.android.data.repository.FirestoreFriendsRepository
import com.ludiary.android.data.repository.FriendsRepositoryImpl
import com.ludiary.android.viewmodel.FriendsUiEvent
import com.ludiary.android.viewmodel.FriendsViewModel
import com.ludiary.android.viewmodel.FriendsViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

class FriendsFragment : Fragment(R.layout.form_friends_profile) {

    private lateinit var vm: FriendsViewModel

    private fun showAddFriendDialog() {
        val til = TextInputLayout(requireContext()).apply {
            hint = "Friend code"
            isHintEnabled = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val input = TextInputEditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // ✅ Importante: TextInputLayout internamente espera LayoutParams tipo LinearLayout
        til.addView(input)

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0) // opcional, para que el dialog no quede pegado
            addView(til)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Añadir amigo")
            .setMessage("Introduce el código de amigo (10–12 caracteres).")
            .setView(container)
            .setPositiveButton("Enviar") { _, _ ->
                val code = input.text?.toString().orEmpty().trim().uppercase()
                vm.sendInviteByCode(code)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = requireContext().applicationContext
        val db = LudiaryDatabase.getInstance(ctx)

        val local = LocalFriendsDataSource(db.friendDao())
        val remote = FirestoreFriendsRepository(FirebaseFirestore.getInstance())
        val repo = FriendsRepositoryImpl(local, remote, FirebaseAuth.getInstance())

        vm = ViewModelProvider(this, FriendsViewModelFactory(repo))[FriendsViewModel::class.java]

        val topAppBar: MaterialToolbar = view.findViewById(R.id.topAppBar)
        val tabLayout: TabLayout = view.findViewById(R.id.tabFriends)
        val pager: ViewPager2 = view.findViewById(R.id.pagerFriends)
        val search: TextInputEditText = view.findViewById(R.id.textSearchFriends)
        val btnPrimary: MaterialButton = view.findViewById(R.id.btnPrimary)

        topAppBar.setNavigationOnClickListener { findNavController().popBackStack() }

        pager.adapter = FriendsPagerAdapter(this)

        TabLayoutMediator(tabLayout, pager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.profile_friends_tab_friends)
                1 -> getString(R.string.profile_friends_tab_groups)
                else -> getString(R.string.profile_friends_tab_requests)
            }
        }.attach()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                vm.onTabChanged(
                    when (tab.position) {
                        0 -> FriendsTab.FRIENDS
                        1 -> FriendsTab.GROUPS
                        else -> FriendsTab.REQUESTS
                    }
                )
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        search.addTextChangedListener { vm.onQueryChanged(it?.toString().orEmpty()) }

        btnPrimary.setOnClickListener { vm.onPrimaryActionClicked() }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.uiState.collectLatest { state ->
                when (state.tab) {
                    FriendsTab.FRIENDS -> {
                        btnPrimary.visibility = View.VISIBLE
                        btnPrimary.text = getString(R.string.profile_friends_add_friend)
                    }
                    FriendsTab.GROUPS -> {
                        btnPrimary.visibility = View.VISIBLE
                        btnPrimary.text = getString(R.string.profile_friends_add_group)
                    }
                    FriendsTab.REQUESTS -> btnPrimary.visibility = View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.events.collectLatest { e ->
                when (e) {
                    is FriendsUiEvent.ShowSnack ->
                        Snackbar.make(view, e.message, Snackbar.LENGTH_SHORT).show()

                    FriendsUiEvent.OpenAddFriend ->
                        showAddFriendDialog()

                    FriendsUiEvent.OpenAddGroup ->
                        Snackbar.make(view, "TODO: crear grupo", Snackbar.LENGTH_SHORT).show()

                    is FriendsUiEvent.OpenEditNickname ->
                        Snackbar.make(view, "TODO: editar mote (${e.friendId})", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        vm.start()
    }

    override fun onStart() {
        super.onStart()
        vm.start()
    }

    override fun onStop() {
        vm.stop()
        super.onStop()
    }

    private inner class FriendsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
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