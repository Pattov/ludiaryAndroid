package com.ludiary.android.ui.profile.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.ludiary.android.R
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.viewmodel.FriendsViewModel
import androidx.lifecycle.lifecycleScope

class InviteFriendsBottomSheet : BottomSheetDialogFragment() {

    private val vm: FriendsViewModel by activityViewModels()

    private val groupId: String by lazy { requireArguments().getString(ARG_GROUP_ID).orEmpty() }
    private val groupName: String by lazy { requireArguments().getString(ARG_GROUP_NAME).orEmpty() }
    private val memberUids: Set<String> by lazy {
        requireArguments().getStringArrayList(ARG_MEMBER_UIDS)?.toSet().orEmpty()
    }

    private lateinit var adapter: InviteFriendsAdapter
    private lateinit var rootView: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottomsheet_invite_friends, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view

        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerInviteFriends)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = InviteFriendsAdapter(
            onClick = { row ->
                if (row.isMember || row.isPending) return@InviteFriendsAdapter

                val uid = row.friendUid
                if (uid.isBlank()) {
                    Snackbar.make(view, "Este amigo aún no tiene UID", Snackbar.LENGTH_SHORT).show()
                    return@InviteFriendsAdapter
                }

                vm.inviteToGroup(groupId, groupName, uid)
                Snackbar.make(view, "Invitación enviada", Snackbar.LENGTH_SHORT).show()
                refreshList()
            },
            onLongClickPending = { row ->
                val inviteId = row.pendingInviteId
                if (inviteId.isNullOrBlank()) {
                    Snackbar.make(view, "No se pudo cancelar (sin inviteId)", Snackbar.LENGTH_SHORT).show()
                    return@InviteFriendsAdapter
                }

                vm.cancelGroupInvite(inviteId)
                Snackbar.make(view, "Invitación cancelada", Snackbar.LENGTH_SHORT).show()
                refreshList()
            }
        )

        recycler.adapter = adapter

        // Primera carga
        refreshList()
    }

    private fun refreshList() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            val friends: List<FriendEntity> = vm.friendsSnapshotForInvite()

            // Invitaciones pendientes ya enviadas (local)
            val pendingInvites = vm.pendingOutgoingInvitesForGroup(groupId)
            val pendingByUid: Map<String, String> = pendingInvites.associate { it.toUid to it.inviteId }
            val pendingUids = pendingInvites.map { it.toUid }.toSet()

            val rows = friends.map { f ->
                val uid = f.friendUid.orEmpty()
                val label = f.nickname?.takeIf { it.isNotBlank() }
                    ?: f.displayName?.takeIf { it.isNotBlank() }
                    ?: f.friendCode?.takeIf { it.isNotBlank() }
                    ?: uid

                val pendingInviteId = pendingByUid[uid]

                InviteFriendRow(
                    friendUid = uid,
                    label = label,
                    isMember = uid.isNotBlank() && memberUids.contains(uid),
                    isPending = uid.isNotBlank() && pendingByUid.containsKey(uid),
                    pendingInviteId = pendingInviteId
                )
            }.sortedWith(
                compareBy<InviteFriendRow> { it.isMember || it.isPending } // invitables primero
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
    val isMember: Boolean,
    val isPending: Boolean,
    val pendingInviteId: String? = null
)