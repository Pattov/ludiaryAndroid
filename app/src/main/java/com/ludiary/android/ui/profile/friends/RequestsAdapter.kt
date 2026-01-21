package com.ludiary.android.ui.profile.friends

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

class RequestsAdapter(
    private val onFriendClick: (FriendEntity) -> Unit,
    private val onAcceptFriend: (Long) -> Unit,
    private val onRejectFriend: (Long) -> Unit,
    private val onAcceptGroup: (String) -> Unit,
    private val onRejectGroup: (String) -> Unit
) : ListAdapter<FriendRowUi, RecyclerView.ViewHolder>(Diff) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is FriendRowUi.Header -> TYPE_HEADER
            else -> TYPE_ITEM
        }
    }

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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is FriendRowUi.Header -> (holder as HeaderVH).bind(row)
            is FriendRowUi.FriendItem -> (holder as ItemVH).bindFriend(row.friend)
            is FriendRowUi.GroupItem -> (holder as ItemVH).bindGroupInvite(row.invite)
        }
    }

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tv: TextView = view.findViewById(R.id.tvHeader)
        fun bind(h: FriendRowUi.Header) {
            tv.text = h.title
        }
    }

    inner class ItemVH(view: View) : RecyclerView.ViewHolder(view) {

        private val tvTitle: TextView = view.findViewById(R.id.tvFriendTitle)
        private val tvSubtitle: TextView = view.findViewById(R.id.tvFriendSubtitle)

        private val btnAccept: View = view.findViewById(R.id.btnAccept)
        private val btnReject: View = view.findViewById(R.id.btnReject)

        // item_friend_row también tiene btnEdit/btnDelete, aquí nunca
        private val btnEdit: View? = view.findViewById(R.id.btnEdit)
        private val btnDelete: View? = view.findViewById(R.id.btnDelete)

        fun bindFriend(item: FriendEntity) {
            val baseName = item.nickname?.takeIf { it.isNotBlank() }
                ?: item.displayName?.takeIf { it.isNotBlank() }
                ?: item.friendCode?.takeIf { it.isNotBlank() }
                ?: "Amigo"

            val codeShort = item.friendCode
                ?.takeIf { it.isNotBlank() }
                ?.let { "#…${it.trim().takeLast(5)}" }
                .orEmpty()

            tvTitle.text = if (codeShort.isBlank()) baseName else "$baseName · $codeShort"

            val isIncoming = item.status == FriendStatus.PENDING_INCOMING
            val isOutgoing = item.status == FriendStatus.PENDING_OUTGOING ||
                    item.status == FriendStatus.PENDING_OUTGOING_LOCAL

            val subtitle = when {
                isIncoming -> "Quiere ser tu amigo"
                isOutgoing -> "Solicitud enviada"
                else -> ""
            }

            tvSubtitle.text = subtitle
            tvSubtitle.isVisible = subtitle.isNotBlank()

            btnAccept.isVisible = isIncoming
            btnReject.isVisible = isIncoming || isOutgoing

            btnEdit?.isVisible = false
            btnDelete?.isVisible = false

            btnAccept.setOnClickListener(null)
            btnReject.setOnClickListener(null)

            if (isIncoming) {
                btnAccept.setOnClickListener { onAcceptFriend(item.id) }
                btnReject.setOnClickListener { onRejectFriend(item.id) } // rechazar
            } else if (isOutgoing) {
                btnReject.setOnClickListener { onRejectFriend(item.id) } // cancelar
            }

            itemView.setOnClickListener { onFriendClick(item) }
        }

        fun bindGroupInvite(invite: GroupInviteEntity) {
            val name = invite.groupNameSnapshot.ifBlank { "Grupo" }

            val suffix = invite.groupId.takeLast(5)
            tvTitle.text = "$name · #…$suffix"

            tvSubtitle.text = "Te han invitado al grupo"
            tvSubtitle.isVisible = true

            // En MVP: solo invitaciones recibidas PENDING
            btnAccept.isVisible = true
            btnReject.isVisible = true

            btnEdit?.isVisible = false
            btnDelete?.isVisible = false

            btnAccept.setOnClickListener(null)
            btnReject.setOnClickListener(null)

            btnAccept.setOnClickListener { onAcceptGroup(invite.inviteId) }
            btnReject.setOnClickListener { onRejectGroup(invite.inviteId) }

            itemView.setOnClickListener(null)
        }

    }

    private object Diff : DiffUtil.ItemCallback<FriendRowUi>() {
        override fun areItemsTheSame(oldItem: FriendRowUi, newItem: FriendRowUi): Boolean {
            return when {
                oldItem is FriendRowUi.Header && newItem is FriendRowUi.Header ->
                    oldItem.title == newItem.title

                oldItem is FriendRowUi.FriendItem && newItem is FriendRowUi.FriendItem ->
                    oldItem.friend.id == newItem.friend.id

                oldItem is FriendRowUi.GroupItem && newItem is FriendRowUi.GroupItem ->
                    oldItem.invite.inviteId == newItem.invite.inviteId

                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: FriendRowUi, newItem: FriendRowUi): Boolean {
            return oldItem == newItem
        }
    }
}