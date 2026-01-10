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
    private val onReject: (Long) -> Unit
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

        // IDs reales de tu item_friend_row.xml
        private val tvTitle: TextView = view.findViewById(R.id.tvFriendTitle)
        private val tvSubtitle: TextView = view.findViewById(R.id.tvFriendSubtitle)

        // Botones (existen en tu layout)
        private val btnAccept: View = view.findViewById(R.id.btnAccept)
        private val btnReject: View = view.findViewById(R.id.btnReject)

        fun bind(item: FriendEntity) {
            val title = item.nickname?.takeIf { it.isNotBlank() }
                ?: item.displayName?.takeIf { it.isNotBlank() }
                ?: "Amigo"

            // Subtítulo neutro (sin email)
            val subtitle = when (item.status) {
                FriendStatus.PENDING_OUTGOING,
                FriendStatus.PENDING_OUTGOING_LOCAL -> "Solicitud pendiente"
                FriendStatus.PENDING_INCOMING -> "Quiere ser tu amigo"
                FriendStatus.BLOCKED -> "Bloqueado"
                FriendStatus.ACCEPTED -> ""
            }

            tvTitle.text = title
            tvSubtitle.text = subtitle
            tvSubtitle.isVisible = subtitle.isNotBlank()

            // ✅ Botones SOLO para solicitudes entrantes
            val showActions = item.status == FriendStatus.PENDING_INCOMING
            btnAccept.isVisible = showActions
            btnReject.isVisible = showActions

            btnAccept.setOnClickListener { onAccept(item.id) }
            btnReject.setOnClickListener { onReject(item.id) }

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