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
import com.ludiary.android.data.model.FriendStatus

class FriendsAdapter(
    private val onClick: (FriendEntity) -> Unit,
    private val onAccept: (Long) -> Unit,
    private val onReject: (Long) -> Unit,
    private val onEditNickname: (Long) -> Unit,
    private val onDeleteFriend: (Long) -> Unit
) : ListAdapter<FriendEntity, FriendsAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_row, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {

        private val tvTitle: TextView = view.findViewById(R.id.tvFriendTitle)
        private val tvSubtitle: TextView = view.findViewById(R.id.tvFriendSubtitle)

        private val btnAccept: View = view.findViewById(R.id.btnAccept)
        private val btnReject: View = view.findViewById(R.id.btnReject)
        private val btnEdit: View = view.findViewById(R.id.btnEdit)
        private val btnDelete: View = view.findViewById(R.id.btnDelete)

        private val groupRequestActions: View = view.findViewById(R.id.groupRequestActions)
        private val groupFriendActions: View = view.findViewById(R.id.groupFriendActions)

        fun bind(item: FriendEntity) {
            val title = item.nickname?.takeIf { it.isNotBlank() }
                ?: item.displayName?.takeIf { it.isNotBlank() }
                ?: "Amigo"

            val subtitle = when (item.status) {
                FriendStatus.ACCEPTED -> item.friendCode?.let { "#$it" } ?: ""
                FriendStatus.PENDING_INCOMING -> "Quiere ser tu amigo"
                FriendStatus.PENDING_OUTGOING,
                FriendStatus.PENDING_OUTGOING_LOCAL -> "Solicitud pendiente"
                FriendStatus.BLOCKED -> "Bloqueado"
            }

            tvTitle.text = title
            tvSubtitle.text = subtitle
            tvSubtitle.isVisible = subtitle.isNotBlank()

            val isAccepted = item.status == FriendStatus.ACCEPTED
            val isIncoming = item.status == FriendStatus.PENDING_INCOMING
            val isOutgoing = item.status == FriendStatus.PENDING_OUTGOING ||
                    item.status == FriendStatus.PENDING_OUTGOING_LOCAL

            // Grupos (asegúrate de tenerlos como views)
            groupRequestActions.isVisible = isIncoming || isOutgoing
            groupFriendActions.isVisible = isAccepted

            // Dentro del grupo de solicitud:
            btnAccept.isVisible = isIncoming
            btnReject.isVisible = isIncoming || isOutgoing

            // Limpieza de clicks
            btnAccept.setOnClickListener(null)
            btnReject.setOnClickListener(null)
            btnEdit.setOnClickListener(null)
            btnDelete.setOnClickListener(null)

            if (isIncoming) {
                btnAccept.setOnClickListener { onAccept(item.id) }
                btnReject.setOnClickListener { onReject(item.id) }
            } else if (isOutgoing) {
                btnReject.setOnClickListener { onReject(item.id) } // cancelar
            } else if (isAccepted) {
                btnEdit.setOnClickListener { onEditNickname(item.id) }
                btnDelete.setOnClickListener { onDeleteFriend(item.id) }
            }

            itemView.setOnClickListener { onClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<FriendEntity>() {
        override fun areItemsTheSame(oldItem: FriendEntity, newItem: FriendEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: FriendEntity, newItem: FriendEntity) =
            oldItem == newItem
    }
}