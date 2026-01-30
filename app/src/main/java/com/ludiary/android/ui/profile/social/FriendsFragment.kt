package com.ludiary.android.ui.profile.social

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.R
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.LocalFriendsDataSource
import com.ludiary.android.data.local.LocalGroupsDataSource
import com.ludiary.android.data.model.FriendsTab
import com.ludiary.android.data.repository.FirestoreFriendsRepository
import com.ludiary.android.data.repository.FirestoreGroupsRepository
import com.ludiary.android.data.repository.FriendsRepositoryImpl
import com.ludiary.android.data.repository.GroupsRepositoryImpl
import com.ludiary.android.viewmodel.FriendsUiEvent
import com.ludiary.android.viewmodel.FriendsViewModel
import com.ludiary.android.viewmodel.SocialViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Pantalla de Amigos/Grupos/Solicitudes dentro del perfil.
 */
class FriendsFragment : Fragment(R.layout.form_social_profile) {

    private lateinit var vm: FriendsViewModel

    /**
     * Muestra un diálogo para introducir un código de amigo y enviar invitación.
     */
    private fun showAddFriendDialog() {
        val til = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.profile_my_code) // si no te cuadra, crea un string específico
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

        til.addView(input)

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
            addView(til)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.profile_friends_add_friend))
            .setMessage(getString(R.string.profile_friend_message))
            .setView(container)
            .setPositiveButton(getString(R.string.action_ok)) { _, _ ->
                val code = input.text?.toString().orEmpty().trim().uppercase()
                vm.sendInviteByCode(code)
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    /**
     * Muestra un diálogo para crear un grupo.
     * Delegará la acción en [FriendsViewModel.createGroup].
     */
    private fun showAddGroupDialog() {
        val til = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.profile_friends_add_group)
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

        til.addView(input)

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
            addView(til)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.profile_friends_add_group))
            .setView(container)
            .setPositiveButton(getString(R.string.action_ok)) { _, _ ->
                val name = input.text?.toString().orEmpty().trim()
                vm.createGroup(name)
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    /**
     * Muestra un diálogo para editar/añadir el nickname (alias) de un amigo.
     *
     * @param friendId ID local (Room) del amigo.
     * @param currentNickname Nickname actual (puede ser null).
     */
    private fun showEditNicknameDialog(friendId: Long, currentNickname: String?) {
        val til = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.friend_nickname_hint)
            val padH = (16 * resources.displayMetrics.density).toInt()
            val padTop = (8 * resources.displayMetrics.density).toInt()
            setPadding(padH, padTop, padH, 0)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val input = TextInputEditText(til.context).apply {
            setText(currentNickname.orEmpty())
            setSelection(text?.length ?: 0)
        }

        til.addView(input)

        val hasNickname = !currentNickname.isNullOrBlank()
        val dialogTitle = if (hasNickname) {
            getString(R.string.action_edit)
        } else {
            getString(R.string.action_add)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(dialogTitle)
            .setView(til)
            .setNegativeButton(getString(R.string.action_cancel), null)
            .setPositiveButton(getString(R.string.action_ok)) { _, _ ->
                val nickname = input.text?.toString()?.trim().orEmpty()
                vm.saveNickname(friendId, nickname)
            }
            .show()
    }

    /**
     * Inicializa UI, ViewModel y listeners.
     * @param view Vista del fragmento
     * @param savedInstanceState Estado de la instancia
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = requireContext().applicationContext
        val db = LudiaryDatabase.getInstance(ctx)

        val friendsLocal = LocalFriendsDataSource(db.friendDao())
        val friendsRemote = FirestoreFriendsRepository(FirebaseFirestore.getInstance())
        val friendsRepo = FriendsRepositoryImpl(friendsLocal, friendsRemote, FirebaseAuth.getInstance())

        val groupsLocal = LocalGroupsDataSource(db.groupDao())
        val groupsRemote = FirestoreGroupsRepository(FirebaseFirestore.getInstance())
        val groupsRepo = GroupsRepositoryImpl(groupsLocal, groupsRemote, FirebaseAuth.getInstance())

        vm = ViewModelProvider(
            this,
            SocialViewModelFactory(friendsRepo, groupsRepo)
        )[FriendsViewModel::class.java]

        val topAppBar: MaterialToolbar = view.findViewById(R.id.topAppBar)
        val tabLayout: TabLayout = view.findViewById(R.id.tabFriends)
        val pager: ViewPager2 = view.findViewById(R.id.pagerFriends)
        val search: TextInputEditText = view.findViewById(R.id.textSearchFriends)
        val btnPrimary: MaterialButton = view.findViewById(R.id.btnPrimary)

        val tvMyFriendCode: TextView = view.findViewById(R.id.tvMyFriendCode)
        val btnCopy: View = view.findViewById(R.id.btnCopyCode)
        val btnShare: View = view.findViewById(R.id.btnShareCode)

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

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        search.addTextChangedListener { vm.onQueryChanged(it?.toString().orEmpty()) }

        btnPrimary.setOnClickListener {
            // Regla UX actual: no puedes crear grupos si no tienes amigos
            if (vm.uiState.value.tab == FriendsTab.GROUPS && !vm.uiState.value.hasFriends) {
                Snackbar.make(view, R.string.profile_friends_empty_friends, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            vm.onPrimaryActionClicked()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.uiState.collectLatest { state ->
                tvMyFriendCode.text = state.myFriendCode ?: "—"

                when (state.tab) {
                    FriendsTab.FRIENDS -> {
                        btnPrimary.visibility = View.VISIBLE
                        btnPrimary.text = getString(R.string.profile_friends_add_friend)
                        btnPrimary.isEnabled = true
                        btnPrimary.alpha = 1f
                    }

                    FriendsTab.GROUPS -> {
                        btnPrimary.visibility = View.VISIBLE
                        btnPrimary.text = getString(R.string.profile_friends_add_group)
                        btnPrimary.isEnabled = state.hasFriends
                        btnPrimary.alpha = if (state.hasFriends) 1f else 0.5f
                    }

                    FriendsTab.REQUESTS -> {
                        btnPrimary.visibility = View.GONE
                    }
                }
            }
        }

        // --- Copy friend code ---
        btnCopy.setOnClickListener {
            val code = vm.uiState.value.myFriendCode ?: return@setOnClickListener
            val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Ludiary friend code", code))
            Snackbar.make(view, R.string.friends_code_copied, Snackbar.LENGTH_SHORT).show()
        }

        // --- Share friend code ---
        btnShare.setOnClickListener {
            val code = vm.uiState.value.myFriendCode ?: return@setOnClickListener
            val shareText = getString(R.string.friends_share_code, code)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }

            startActivity(
                Intent.createChooser(
                    shareIntent,
                    getString(R.string.friends_share_chooser)
                )
            )

        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.events.collectLatest { e ->
                when (e) {
                    is FriendsUiEvent.ShowSnack -> {
                        val msg = getString(e.messageRes)
                        Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
                    }

                    FriendsUiEvent.OpenAddFriend ->
                        showAddFriendDialog()

                    FriendsUiEvent.OpenAddGroup ->
                        showAddGroupDialog()

                    is FriendsUiEvent.OpenEditNickname ->
                        showEditNicknameDialog(e.friendId, e.currentNickname)
                }
            }
        }
    }

    /**
     * Inicia listeners/sincronización (delegado al ViewModel).
     */
    override fun onStart() {
        super.onStart()
        vm.start()
    }

    /**
     * Detiene listeners/sincronización (delegado al ViewModel).
     */
    override fun onStop() {
        vm.stop()
        super.onStop()
    }

    /**
     * Adaptador de pestañas:
     * - FRIENDS
     * - GROUPS
     * - REQUESTS
     */
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