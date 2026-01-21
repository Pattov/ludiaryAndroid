package com.ludiary.android.ui.profile.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.ludiary.android.R
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.viewmodel.FriendsViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ludiary.android.data.local.LocalFriendsDataSource
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.repository.FirestoreFriendsRepository
import com.ludiary.android.data.repository.FriendsRepositoryImpl
import com.ludiary.android.data.repository.GroupsRepositoryImpl
import com.ludiary.android.viewmodel.FriendsViewModelFactory

class InviteFriendsBottomSheet : BottomSheetDialogFragment() {

    private lateinit var vm: FriendsViewModel
    private val groupId: String by lazy { requireArguments().getString(ARG_GROUP_ID).orEmpty() }
    private val groupName: String by lazy { requireArguments().getString(ARG_GROUP_NAME).orEmpty() }
    private val memberUids: Set<String> by lazy {
        requireArguments().getStringArrayList(ARG_MEMBER_UIDS)?.toSet().orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottomsheet_invite_friends, container, false)

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
        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerInviteFriends)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        val adapter = InviteFriendsAdapter { row ->
            if (row.isMember) return@InviteFriendsAdapter

            val uid = row.friendUid
            if (uid.isBlank()) {
                Snackbar.make(view, "Este amigo aún no tiene UID", Snackbar.LENGTH_SHORT).show()
                return@InviteFriendsAdapter
            }

            vm.inviteToGroup(groupId, groupName, uid)
            Snackbar.make(view, "Invitación enviada", Snackbar.LENGTH_SHORT).show()
        }
        recycler.adapter = adapter

        // Cargamos amigos una vez (MVP)
        viewLifecycleOwner.lifecycleScope.launch {
            val friends: List<FriendEntity> = vm.friendsSnapshotForInvite()

            val rows = friends.map { f ->
                val uid = f.friendUid.orEmpty()
                val label = f.nickname?.takeIf { it.isNotBlank() }
                    ?: f.displayName?.takeIf { it.isNotBlank() }
                    ?: f.friendCode?.takeIf { it.isNotBlank() }
                    ?: uid

                InviteFriendRow(
                    friendUid = uid,
                    label = label,
                    isMember = uid.isNotBlank() && memberUids.contains(uid)
                )
            }.sortedWith(
                compareBy<InviteFriendRow> { it.isMember } // primero invitables
                    .thenBy { it.label.lowercase() }
            )

            adapter.submitList(rows)
        }
    }

    companion object {
        private const val ARG_GROUP_ID = "groupId"
        private const val ARG_GROUP_NAME = "groupName"
        private const val ARG_MEMBER_UIDS = "memberUids"

        fun newInstance(groupId: String, groupName: String, memberUids: List<String>) =
            InviteFriendsBottomSheet().apply {
                arguments = bundleOf(
                    ARG_GROUP_ID to groupId,
                    ARG_GROUP_NAME to groupName,
                    ARG_MEMBER_UIDS to ArrayList(memberUids)
                )
            }
    }
}

data class InviteFriendRow(
    val friendUid: String,
    val label: String,
    val isMember: Boolean
)