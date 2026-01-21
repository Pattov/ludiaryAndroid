package com.ludiary.android.ui.profile.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ludiary.android.R

class InviteFriendsAdapter(
    private val onClick: (InviteFriendRow) -> Unit
) : ListAdapter<InviteFriendRow, InviteFriendsAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_invite_friend_row, parent, false)
        return VH(v, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class VH(view: View, private val onClick: (InviteFriendRow) -> Unit) : RecyclerView.ViewHolder(view) {
        private val tvLabel: TextView = view.findViewById(R.id.tvInviteFriendLabel)
        private val tvState: TextView = view.findViewById(R.id.tvInviteFriendState)

        fun bind(item: InviteFriendRow) {
            tvLabel.text = item.label
            tvState.text = if (item.isMember) "Ya es miembro" else ""

            val enabled = !item.isMember
            itemView.isEnabled = enabled
            itemView.alpha = if (enabled) 1f else 0.45f

            itemView.setOnClickListener {
                if (enabled) onClick(item)
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<InviteFriendRow>() {
        override fun areItemsTheSame(oldItem: InviteFriendRow, newItem: InviteFriendRow) =
            oldItem.friendUid == newItem.friendUid

        override fun areContentsTheSame(oldItem: InviteFriendRow, newItem: InviteFriendRow) =
            oldItem == newItem
    }
}
