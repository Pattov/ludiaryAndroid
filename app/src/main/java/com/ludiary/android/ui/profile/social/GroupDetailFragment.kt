package com.ludiary.android.ui.profile.social

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.R
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.LocalFriendsDataSource
import com.ludiary.android.data.local.LocalGroupsDataSource
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.repository.profile.FirestoreFriendsRepository
import com.ludiary.android.data.repository.profile.FirestoreGroupsRepository
import com.ludiary.android.data.repository.profile.FriendsRepositoryImpl
import com.ludiary.android.data.repository.profile.GroupsRepositoryImpl
import com.ludiary.android.viewmodel.FriendsViewModel
import com.ludiary.android.viewmodel.SocialViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Pantalla de detalle de un grupo.
 */
class GroupDetailFragment : Fragment(R.layout.fragment_group_detail) {

    private lateinit var vm: FriendsViewModel

    private val groupId: String by lazy { requireArguments().getString(ARG_GROUP_ID).orEmpty() }
    private val groupName: String by lazy { requireArguments().getString(ARG_GROUP_NAME).orEmpty() }

    /**
     * Inicializa UI, ViewModel y listeners.
     * @param view Vista del fragmento
     * @param savedInstanceState Estado de la instancia
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModel()

        val topAppBar: MaterialToolbar = view.findViewById(R.id.topAppBar)
        val recycler: RecyclerView = view.findViewById(R.id.recyclerMembers)
        val empty: TextView = view.findViewById(R.id.tvEmptyMembers)
        val btnInvite: MaterialButton = view.findViewById(R.id.btnInviteMember)
        val btnLeave: MaterialButton = view.findViewById(R.id.btnLeaveGroup)

        topAppBar.title = groupName.ifBlank { getString(R.string.groups_default_name) }
        topAppBar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        val adapter = GroupMembersAdapter()
        recycler.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            val friendsSnapshot = vm.friendsSnapshotForInvite()
            val friendMap = friendsSnapshot.toFriendLabelMap()

            vm.groupMembers(groupId).collectLatest { members ->
                val uiRows = members.map { m ->
                    val label = friendMap[m.uid] ?: m.uid
                    GroupMemberUi(
                        uid = m.uid,
                        label = label,
                        isFriend = friendMap.containsKey(m.uid)
                    )
                }

                val membersCount = members.size
                topAppBar.subtitle = resources.getQuantityString(
                    R.plurals.groups_members_count,
                    membersCount,
                    membersCount
                )

                adapter.submitList(uiRows)
                empty.visibility = if (uiRows.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // Invitar (BottomSheet)
        btnInvite.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val members = vm.groupMembersOnce(groupId)
                InviteFriendsBottomSheet
                    .newInstance(
                        groupId = groupId,
                        groupName = groupName,
                        memberUids = members.map { it.uid },
                        membersCount = members.size
                    )
                    .show(parentFragmentManager, "InviteFriendsBottomSheet")
            }
        }

        // Salir del grupo
        btnLeave.setOnClickListener {
            vm.leaveGroup(groupId)
            Snackbar.make(view, R.string.groups_member_left_snack, Snackbar.LENGTH_SHORT).show()
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    /**
     * Inicializa el ViewModel con repositorios de Friends y Groups.
     */
    private fun initViewModel() {
        val ctx = requireContext().applicationContext
        val db = LudiaryDatabase.getInstance(ctx)
        val fs = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()

        val friendsRepo = FriendsRepositoryImpl(
            local = LocalFriendsDataSource(db.friendDao()),
            remote = FirestoreFriendsRepository(fs),
            auth = auth
        )

        val groupsRepo = GroupsRepositoryImpl(
            local = LocalGroupsDataSource(db.groupDao()),
            remote = FirestoreGroupsRepository(fs),
            auth = auth
        )

        vm = ViewModelProvider(
            requireActivity(),
            SocialViewModelFactory(friendsRepo, groupsRepo)
        )[FriendsViewModel::class.java]
    }

    /**
     * Construye un map uid -> label con el “mejor nombre disponible” para UI.
     * 1. nickname del usuario
     * 2. Nombre del usuario
     * 3. FriendCode
     */
    private fun List<FriendEntity>.toFriendLabelMap(): Map<String, String> =
        this.asSequence()
            .filter { !it.friendUid.isNullOrBlank() }
            .associate { f ->
                val uid = f.friendUid!!
                val label = f.nickname?.takeIf { it.isNotBlank() }
                    ?: f.displayName?.takeIf { it.isNotBlank() }
                    ?: f.friendCode?.takeIf { it.isNotBlank() }
                    ?: uid
                uid to label
            }

    private companion object {
        private const val ARG_GROUP_ID = "groupId"
        private const val ARG_GROUP_NAME = "groupName"
    }
}