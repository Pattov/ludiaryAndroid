package com.ludiary.android.ui.profile.friends

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.ludiary.android.R
import com.ludiary.android.viewmodel.FriendsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.local.LocalFriendsDataSource
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.repository.FirestoreFriendsRepository
import com.ludiary.android.data.repository.FriendsRepositoryImpl
import com.ludiary.android.data.repository.GroupsRepositoryImpl
import com.ludiary.android.viewmodel.FriendsViewModelFactory

class GroupDetailFragment : Fragment(R.layout.fragment_group_detail) {

    private lateinit var vm: FriendsViewModel

    private val groupId: String by lazy { requireArguments().getString("groupId").orEmpty() }
    private val groupName: String by lazy { requireArguments().getString("groupName").orEmpty() }

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
        val topAppBar: MaterialToolbar = view.findViewById(R.id.topAppBar)
        val recycler: RecyclerView = view.findViewById(R.id.recyclerMembers)
        val empty: TextView = view.findViewById(R.id.tvEmptyMembers)
        val btnInvite: MaterialButton = view.findViewById(R.id.btnInviteMember)
        val btnLeave: MaterialButton = view.findViewById(R.id.btnLeaveGroup)

        topAppBar.title = groupName.ifBlank { "Grupo" }
        topAppBar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        val adapter = GroupMembersAdapter()
        recycler.adapter = adapter

        // Miembros (y mapeo a nombres de amigos)
        viewLifecycleOwner.lifecycleScope.launch {
            vm.groupMembers(groupId).collectLatest { members ->
                val friends = vm.friendsSnapshotForInvite()

                val friendMap: Map<String, String> = friends
                    .filter { !it.friendUid.isNullOrBlank() }
                    .associate { f ->
                        val label = f.nickname?.takeIf { it.isNotBlank() }
                            ?: f.displayName?.takeIf { it.isNotBlank() }
                            ?: f.friendCode?.takeIf { it.isNotBlank() }
                            ?: f.friendUid!! // último fallback

                        f.friendUid!! to label
                    }

                val ui = members.map { m ->
                    val label = friendMap[m.uid] ?: m.uid
                    GroupMemberUi(
                        uid = m.uid,
                        label = label,
                        isFriend = friendMap.containsKey(m.uid)
                    )
                }

                topAppBar.subtitle = if (ui.size == 1) "1 miembro" else "${ui.size} miembros"

                adapter.submitList(ui)
                empty.visibility = if (ui.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // Invitar (BottomSheet con desactivado si ya es miembro)
        btnInvite.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val members = vm.groupMembersOnce(groupId)
                InviteFriendsBottomSheet
                    .newInstance(groupId, groupName, members.map { it.uid })
                    .show(parentFragmentManager, "InviteFriendsBottomSheet")
            }
        }

        // Salir del grupo
        btnLeave.setOnClickListener {
            vm.leaveGroup(groupId)
            Snackbar.make(view, "Has salido del grupo", Snackbar.LENGTH_SHORT).show()
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
}