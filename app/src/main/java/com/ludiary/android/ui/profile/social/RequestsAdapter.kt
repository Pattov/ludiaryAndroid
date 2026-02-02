package com.ludiary.android.ui.profile.social

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ludiary.android.R
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.local.entity.GroupInviteEntity
import com.ludiary.android.data.model.FriendStatus
import com.ludiary.android.viewmodel.FriendRowUi

/**
 * Adapter para la pestaña de "Solicitudes" (amigos + invitaciones de grupo).
 * @param onFriendClick Callback al pulsar sobre una solicitud de amigo (por ejemplo, abrir perfil).
 * @param onAcceptFriend Aceptar solicitud de amistad entrante (recibe id local Room).
 * @param onRejectFriend Rechazar solicitud entrante o cancelar solicitud saliente de amistad (id local Room).
 * @param onAcceptGroup Aceptar invitación entrante de grupo (inviteId remoto).
 * @param onRejectGroup Rechazar invitación entrante de grupo (inviteId remoto).
 * @param onCancelGroup Cancelar invitación saliente de grupo (inviteId remoto).
 */
class RequestsAdapter(
    private val onFriendClick: (FriendEntity) -> Unit,
    private val onAcceptFriend: (Long) -> Unit,
    private val onRejectFriend: (Long) -> Unit,
    private val onAcceptGroup: (String) -> Unit,
    private val onRejectGroup: (String) -> Unit,
    private val onCancelGroup: (String) -> Unit
) : ListAdapter<FriendRowUi, RecyclerView.ViewHolder>(Diff) {

    private companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
        private const val GROUP_SUFFIX_LEN = 5
        private const val FRIEND_CODE_SUFFIX_LEN = 5
    }

    /**
     * Devuelve el tipo de vista dependiendo del modelo `FriendRowUi` en esa posición.
     */
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is FriendRowUi.Header -> TYPE_HEADER
            else -> TYPE_ITEM
        }
    }

    /**
     * Infla el layout correspondiente:
     * - Cabecera → `item_requests_header`
     * - Item → `item_friend_row`
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val v = inflater.inflate(R.layout.item_requests_header, parent, false)
                HeaderVH(v)
            }
            else -> {
                val v = inflater.inflate(R.layout.item_friend_row, parent, false)
                ItemVH(v)
            }
        }
    }

    /**
     * Bindea el item según su tipo.
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is FriendRowUi.Header -> (holder as HeaderVH).bind(row)
            is FriendRowUi.FriendItem -> (holder as ItemVH).bindFriend(row.friend)
            is FriendRowUi.GroupItem -> (holder as ItemVH).bindGroupInvite(row.invite, row.isOutgoing)
        }
    }

    /**
     * ViewHolder de cabecera de sección.
     */
    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tv: TextView = view.findViewById(R.id.tvHeader)

        /**
         * Bindea el título de la sección usando el recurso `titleRes`.
         */
        fun bind(h: FriendRowUi.Header) {
            tv.setText(h.titleRes)
        }
    }

    /**
     * ViewHolder reutilizable para filas de:
     */
    inner class ItemVH(view: View) : RecyclerView.ViewHolder(view) {

        private val tvTitle: TextView = view.findViewById(R.id.tvFriendTitle)
        private val tvSubtitle: TextView = view.findViewById(R.id.tvFriendSubtitle)

        private val btnAccept: View = view.findViewById(R.id.btnAccept)
        private val btnReject: View = view.findViewById(R.id.btnReject)
        private val btnEdit: View? = view.findViewById(R.id.btnEdit)
        private val btnDelete: View? = view.findViewById(R.id.btnDelete)

        /**
         * Bindea una fila de solicitud de amistad.
         */
        fun bindFriend(item: FriendEntity) {
            val ctx = itemView.context

            val baseName = item.nickname?.takeIf { it.isNotBlank() }
                ?: item.displayName?.takeIf { it.isNotBlank() }
                ?: item.friendCode?.takeIf { it.isNotBlank() }
                ?: ctx.getString(R.string.friends_default_title)

            val codeShort = item.friendCode
                ?.takeIf { it.isNotBlank() }
                ?.trim()
                ?.takeLast(FRIEND_CODE_SUFFIX_LEN)
                ?.let { ctx.getString(R.string.friends_code_suffix_format, it) }
                .orEmpty()

            tvTitle.text = if (codeShort.isBlank()) baseName else "$baseName · $codeShort"

            val isIncoming = item.status == FriendStatus.PENDING_INCOMING
            val isOutgoing = item.status == FriendStatus.PENDING_OUTGOING ||
                    item.status == FriendStatus.PENDING_OUTGOING_LOCAL

            when {
                isIncoming -> {
                    tvSubtitle.setText(R.string.requests_friend_incoming)
                    tvSubtitle.isVisible = true
                }
                isOutgoing -> {
                    tvSubtitle.setText(R.string.requests_friend_outgoing)
                    tvSubtitle.isVisible = true
                }
                else -> tvSubtitle.isVisible = false
            }

            btnAccept.isVisible = isIncoming
            btnReject.isVisible = isIncoming || isOutgoing

            // Este adapter no usa acciones de editar/borrar de item_friend_row
            btnEdit?.isVisible = false
            btnDelete?.isVisible = false

            // Reset de listeners para evitar efectos de reciclado de ViewHolders
            btnAccept.setOnClickListener(null)
            btnReject.setOnClickListener(null)

            if (isIncoming) {
                btnAccept.setOnClickListener { onAcceptFriend(item.id) }
                btnReject.setOnClickListener { onRejectFriend(item.id) }
            } else if (isOutgoing) {
                btnReject.setOnClickListener { onRejectFriend(item.id) }
            }

            // Click en la fila: se delega (por ejemplo para abrir perfil / detalle)
            itemView.setOnClickListener { onFriendClick(item) }
        }

        /**
         * Bindea una invitación de grupo.
         * @param invite Invitación a mostrar.
         * @param isOutgoing Si la invitación fue enviada por el usuario actual.
         */
        fun bindGroupInvite(invite: GroupInviteEntity, isOutgoing: Boolean) {
            val ctx = itemView.context

            val name = invite.groupNameSnapshot.ifBlank {
                ctx.getString(R.string.groups_default_name)
            }
            val suffix = invite.groupId.takeLast(GROUP_SUFFIX_LEN)

            tvTitle.text = ctx.getString(
                R.string.groups_code_suffix_format,
                name,
                suffix
            )

            // Reset de listeners para evitar efectos de reciclado
            btnAccept.setOnClickListener(null)
            btnReject.setOnClickListener(null)

            if (!isOutgoing) {
                // Invitación recibida
                tvSubtitle.setText(R.string.requests_group_incoming)
                tvSubtitle.isVisible = true

                btnAccept.isVisible = true
                btnReject.isVisible = true

                btnAccept.setOnClickListener { onAcceptGroup(invite.inviteId) }
                btnReject.setOnClickListener { onRejectGroup(invite.inviteId) }
            } else {
                // Invitación enviada
                tvSubtitle.setText(R.string.requests_group_outgoing)
                tvSubtitle.isVisible = true

                btnAccept.isVisible = false
                btnReject.isVisible = true

                // Cancelar invitación
                btnReject.setOnClickListener { onCancelGroup(invite.inviteId) }
            }

            // Este adapter no usa editar/borrar
            btnEdit?.isVisible = false
            btnDelete?.isVisible = false

            // En invitaciones no hay click de fila (solo acciones)
            itemView.setOnClickListener(null)
        }
    }

    /**
     * DiffUtil para comparar elementos heterogéneos (Header/FriendItem/GroupItem).
     */
    private object Diff : DiffUtil.ItemCallback<FriendRowUi>() {
        override fun areItemsTheSame(oldItem: FriendRowUi, newItem: FriendRowUi): Boolean {
            return when (oldItem) {
                is FriendRowUi.Header ->
                    newItem is FriendRowUi.Header && oldItem.titleRes == newItem.titleRes

                is FriendRowUi.FriendItem ->
                    newItem is FriendRowUi.FriendItem && oldItem.friend.id == newItem.friend.id

                is FriendRowUi.GroupItem ->
                    newItem is FriendRowUi.GroupItem && oldItem.invite.inviteId == newItem.invite.inviteId
            }
        }

        override fun areContentsTheSame(oldItem: FriendRowUi, newItem: FriendRowUi): Boolean {
            return oldItem == newItem
        }
    }
}