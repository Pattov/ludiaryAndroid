package com.ludiary.android.ui.profile.friends

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
import com.ludiary.android.viewmodel.FriendsViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

    private fun showAddGroupDialog() {
        val til = TextInputLayout(requireContext()).apply {
            hint = "Nombre del grupo"
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
            .setTitle("Crear grupo")
            .setView(container)
            .setPositiveButton("Crear") { _, _ ->
                val name = input.text?.toString().orEmpty().trim()
                vm.createGroup(name)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditNicknameDialog(friendId: Long, currentNickname: String?) {
        val til = TextInputLayout(requireContext()).apply {
            hint = "Apodo"
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
        val dialogTitle = if (hasNickname) "Editar apodo" else "Añadir apodo"


        MaterialAlertDialogBuilder(requireContext())
            .setTitle(dialogTitle)
            .setView(til)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aceptar") { _, _ ->
                val nickname = input.text?.toString()?.trim().orEmpty()
                vm.saveNickname(friendId, nickname)
            }
            .show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx = requireContext().applicationContext
        val db = LudiaryDatabase.getInstance(ctx)

        val friendsLocal = LocalFriendsDataSource(db.friendDao())
        val friendsRemote = FirestoreFriendsRepository(FirebaseFirestore.getInstance())
        val friendsRepo = FriendsRepositoryImpl(friendsLocal, friendsRemote, FirebaseAuth.getInstance())

        val groupsLocal = LocalGroupsDataSource(db.groupDao())
        val groupsRemote = FirestoreGroupsRepository(FirebaseFirestore.getInstance())
        val groupsRepo = GroupsRepositoryImpl( groupsLocal, groupsRemote, FirebaseAuth.getInstance())
        vm = ViewModelProvider(
            this,
            FriendsViewModelFactory(friendsRepo, groupsRepo)
        )[FriendsViewModel::class.java]

        val topAppBar: MaterialToolbar = view.findViewById(R.id.topAppBar)
        val tabLayout: TabLayout = view.findViewById(R.id.tabFriends)
        val pager: ViewPager2 = view.findViewById(R.id.pagerFriends)
        val search: TextInputEditText = view.findViewById(R.id.textSearchFriends)
        val btnPrimary: MaterialButton = view.findViewById(R.id.btnPrimary)

        val tvMyFriendCode: TextView = view.findViewById(R.id.tvMyFriendCode)
        val btnCopy: View = view.findViewById(R.id.btnCopyCode)
        val btnShare: View = view.findViewById(R.id.btnShareCode)

        topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

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

        search.addTextChangedListener {
            vm.onQueryChanged(it?.toString().orEmpty())
        }

        btnPrimary.setOnClickListener {
            vm.onPrimaryActionClicked()
        }

        // UI state: friendCode + botón primary
        viewLifecycleOwner.lifecycleScope.launch {
            vm.uiState.collectLatest { state ->
                tvMyFriendCode.text = state.myFriendCode ?: "—"

                when (state.tab) {
                    FriendsTab.FRIENDS -> {
                        btnPrimary.visibility = View.VISIBLE
                        btnPrimary.text = getString(R.string.profile_friends_add_friend)
                    }
                    FriendsTab.GROUPS -> {
                        btnPrimary.visibility = View.VISIBLE
                        btnPrimary.text = getString(R.string.profile_friends_add_group)
                    }
                    FriendsTab.REQUESTS -> {
                        btnPrimary.visibility = View.GONE
                    }
                }
            }
        }

        // Copiar código
        btnCopy.setOnClickListener {
            val code = vm.uiState.value.myFriendCode ?: return@setOnClickListener
            val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Ludiary friend code", code))
            Snackbar.make(view, "Código copiado", Snackbar.LENGTH_SHORT).show()
        }

        // Compartir código
        btnShare.setOnClickListener {
            val code = vm.uiState.value.myFriendCode ?: return@setOnClickListener
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Añádeme en Ludiary con este código: $code")
            }
            startActivity(Intent.createChooser(shareIntent, "Compartir código"))
        }

        // Eventos
        viewLifecycleOwner.lifecycleScope.launch {
            vm.events.collectLatest { e ->
                when (e) {
                    is FriendsUiEvent.ShowSnack ->
                        Snackbar.make(view, e.message, Snackbar.LENGTH_SHORT).show()

                    FriendsUiEvent.OpenAddFriend ->
                        showAddFriendDialog()

                    FriendsUiEvent.OpenAddGroup ->
                        showAddGroupDialog()

                    is FriendsUiEvent.OpenEditNickname -> {
                        showEditNicknameDialog(e.friendId, e.currentNickname)
                    }
                }
            }
        }
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