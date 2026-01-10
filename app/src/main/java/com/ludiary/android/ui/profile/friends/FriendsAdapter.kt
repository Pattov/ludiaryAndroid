package com.ludiary.android.ui.profile.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ludiary.android.R
import com.ludiary.android.data.local.entity.FriendEntity

class FriendsAdapter(
    private val onClick: (FriendEntity) -> Unit
) : ListAdapter<FriendEntity, FriendsAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_row, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)

        fun bind(item: FriendEntity) {
            val title = item.nickname?.takeIf { it.isNotBlank() }
                ?: item.displayName?.takeIf { it.isNotBlank() }
                ?: "Amigo"

            // Subtítulo neutro (sin datos personales)
            val subtitle = when (item.status) {
                com.ludiary.android.data.model.FriendStatus.PENDING_OUTGOING,
                com.ludiary.android.data.model.FriendStatus.PENDING_OUTGOING_LOCAL -> "Solicitud pendiente"
                com.ludiary.android.data.model.FriendStatus.PENDING_INCOMING -> "Quiere ser tu amigo"
                com.ludiary.android.data.model.FriendStatus.BLOCKED -> "Bloqueado"
                else -> ""
            }

            tvTitle.text = title
            tvSubtitle.text = subtitle
            tvSubtitle.visibility = if (subtitle.isBlank()) View.GONE else View.VISIBLE

            itemView.setOnClickListener { onClick(item) }
        }

    }

    private object Diff : DiffUtil.ItemCallback<FriendEntity>() {
        override fun areItemsTheSame(oldItem: FriendEntity, newItem: FriendEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: FriendEntity, newItem: FriendEntity) = oldItem == newItem
    }
}