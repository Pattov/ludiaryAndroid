package com.ludiary.android.ui.profile.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.ludiary.android.R
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.model.FriendStatus

class FriendsAdapter(
    private val onClick: (FriendEntity) -> Unit,
    private val onAccept: ((FriendEntity) -> Unit)? = null,
    private val onReject: ((FriendEntity) -> Unit)? = null
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

        private val tvTitle: TextView? = itemView.findViewById(R.id.tvFriendTitle)
            ?: itemView.findViewById(R.id.tvTitle)

        private val tvSubtitle: TextView? = itemView.findViewById(R.id.tvFriendSubtitle)
            ?: itemView.findViewById(R.id.tvSubtitle)

        private val btnAccept: MaterialButton? = itemView.findViewById(R.id.btnAccept)
        private val btnReject: MaterialButton? = itemView.findViewById(R.id.btnReject)

        fun bind(item: FriendEntity) {
            val title = item.nickname?.takeIf { it.isNotBlank() }
                ?: item.displayName?.takeIf { it.isNotBlank() }
                ?: "Amigo"

            val subtitle = when (item.status) {
                FriendStatus.PENDING_OUTGOING,
                FriendStatus.PENDING_OUTGOING_LOCAL -> "Solicitud pendiente"
                FriendStatus.PENDING_INCOMING -> "Quiere ser tu amigo"
                FriendStatus.BLOCKED -> "Bloqueado"
                else -> ""
            }

            tvTitle?.text = title
            tvSubtitle?.text = subtitle
            tvSubtitle?.isVisible = subtitle.isNotBlank()

            val showActions = item.status == FriendStatus.PENDING_INCOMING
            btnAccept?.isVisible = showActions
            btnReject?.isVisible = showActions

            btnAccept?.setOnClickListener { onAccept?.invoke(item) }
            btnReject?.setOnClickListener { onReject?.invoke(item) }

            itemView.setOnClickListener { onClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<FriendEntity>() {
        override fun areItemsTheSame(oldItem: FriendEntity, newItem: FriendEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: FriendEntity, newItem: FriendEntity) = oldItem == newItem
    }
}