package com.ludiary.android.ui.profile.social

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.ludiary.android.R
import com.ludiary.android.data.local.LudiaryDatabase
import com.ludiary.android.data.local.LocalFriendsDataSource
import com.ludiary.android.data.local.LocalGroupsDataSource
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.repository.profile.FriendsRepositoryProvider
import com.ludiary.android.data.repository.profile.GroupsRepositoryProvider
import com.ludiary.android.viewmodel.FriendsViewModel
import com.ludiary.android.viewmodel.SocialViewModelFactory
import kotlinx.coroutines.launch

/**
 * BottomSheet para invitar amigos a un grupo.
 */
class InviteFriendsBottomSheet : BottomSheetDialogFragment() {

    /**
     * ViewModel compartido con el host (Activity).
     */
    private val vm: FriendsViewModel by activityViewModels {
        val ctx = requireContext().applicationContext
        val db = LudiaryDatabase.getInstance(requireContext())

        val friendsRepo = FriendsRepositoryProvider.provide(
            context = ctx,
            local = LocalFriendsDataSource(db.friendDao())
        )

        val groupsRepo = GroupsRepositoryProvider.provide(
            context = ctx,
            local = LocalGroupsDataSource(db.groupDao())
        )

        SocialViewModelFactory(friendsRepo, groupsRepo)
    }


    /** ID del grupo al que se invitará. */
    private val groupId: String by lazy { requireArguments().getString(ARG_GROUP_ID).orEmpty() }

    /** Nombre (snapshot) del grupo al que se invitará. */
    private val groupName: String by lazy { requireArguments().getString(ARG_GROUP_NAME).orEmpty() }

    /** UIDs de miembros actuales del grupo, para deshabilitar filas ya miembros. */
    private val memberUids: Set<String> by lazy {
        requireArguments().getStringArrayList(ARG_MEMBER_UIDS)?.toSet().orEmpty()
    }

    private lateinit var adapter: InviteFriendsAdapter
    private lateinit var rootView: View

    /**
     * Infla el layout del bottom sheet.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottomsheet_invite_friends, container, false)

    /**
     * Inicializa RecyclerView, adapter y hace la primera carga de datos.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view

        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerInviteFriends)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = InviteFriendsAdapter(
            onClick = { row ->
                // Seguridad: no permitir invitar si ya es miembro o ya está pendiente
                if (row.isMember || row.isPending) return@InviteFriendsAdapter

                val uid = row.friendUid
                if (uid.isBlank()) {
                    Snackbar.make(view, R.string.invite_friend_missing_uid, Snackbar.LENGTH_SHORT).show()
                    return@InviteFriendsAdapter
                }

                vm.inviteToGroup(groupId, groupName, uid)
                Snackbar.make(view, R.string.invite_friend_sent, Snackbar.LENGTH_SHORT).show()
                refreshList()
            },
            onLongClickPending = { row ->
                val inviteId = row.pendingInviteId
                if (inviteId.isNullOrBlank()) {
                    Snackbar.make(view, R.string.invite_friend_cancel_missing_invite_id, Snackbar.LENGTH_SHORT).show()
                    return@InviteFriendsAdapter
                }

                vm.cancelGroupInvite(inviteId)
                Snackbar.make(view, R.string.invite_friend_cancelled, Snackbar.LENGTH_SHORT).show()
                refreshList()
            }
        )

        recycler.adapter = adapter

        // Primera carga
        refreshList()
    }

    /**
     * Reconstruye la lista en base a:
     * - amigos aceptados (snapshot desde VM)
     * - invitaciones salientes pendientes del grupo (local)
     * - miembros actuales (argumento)
     */
    private fun refreshList() {
        viewLifecycleOwner.lifecycleScope.launch {
            val friends: List<FriendEntity> = vm.friendsSnapshotForInvite()

            // Invitaciones pendientes ya enviadas para este grupo (local)
            val pendingInvites = vm.pendingOutgoingInvitesForGroup(groupId)

            // Map: toUid -> inviteId (para poder cancelar)
            val pendingByUid: Map<String, String> = pendingInvites.associate { it.toUid to it.inviteId }

            val rows = friends
                .map { f ->
                    val uid = f.friendUid.orEmpty()

                    val label = f.nickname?.takeIf { it.isNotBlank() }
                        ?: f.displayName?.takeIf { it.isNotBlank() }
                        ?: f.friendCode?.takeIf { it.isNotBlank() }
                        ?: uid

                    InviteFriendRow(
                        friendUid = uid,
                        label = label,
                        isMember = uid.isNotBlank() && memberUids.contains(uid),
                        isPending = uid.isNotBlank() && pendingByUid.containsKey(uid),
                        pendingInviteId = pendingByUid[uid]
                    )
                }
                .sortedWith(
                    compareBy<InviteFriendRow> { it.isMember || it.isPending }
                        .thenBy { it.label.lowercase() }
                )

            adapter.submitList(rows)
        }
    }

    companion object {
        private const val ARG_GROUP_ID = "groupId"
        private const val ARG_GROUP_NAME = "groupName"
        private const val ARG_MEMBER_UIDS = "memberUids"
        private const val ARG_MEMBERS_COUNT = "membersCount"

        /**
         * Crea una instancia del bottom sheet con los argumentos necesarios.
         *
         * @param groupId ID del grupo.
         * @param groupName Nombre (snapshot) del grupo.
         * @param memberUids Lista de UIDs de miembros actuales.
         * @param membersCount Número de miembros (opcional informativo).
         */
        fun newInstance(
            groupId: String,
            groupName: String,
            memberUids: List<String>,
            membersCount: Int
        ) = InviteFriendsBottomSheet().apply {
            arguments = bundleOf(
                ARG_GROUP_ID to groupId,
                ARG_GROUP_NAME to groupName,
                ARG_MEMBER_UIDS to ArrayList(memberUids),
                ARG_MEMBERS_COUNT to membersCount
            )
        }
    }
}

/**
 * Modelo de UI para cada fila del listado de invitación.
 *
 * @property friendUid UID del amigo (target). Si está vacío, no se puede invitar.
 * @property label Texto principal a mostrar (nickname/displayName/friendCode/uid).
 * @property isMember Indica si ya pertenece al grupo.
 * @property isPending Indica si ya hay invitación pendiente.
 * @property pendingInviteId ID de la invitación pendiente (si existe) para poder cancelarla.
 */
data class InviteFriendRow(
    val friendUid: String,
    val label: String,
    val isMember: Boolean,
    val isPending: Boolean,
    val pendingInviteId: String? = null
)