package com.ludiary.android.ui.profile.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ludiary.android.R

data class GroupMemberUi(
    val uid: String,
    val label: String,
    val isFriend: Boolean
)

class GroupMembersAdapter : ListAdapter<GroupMemberUi, GroupMembersAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_group_member_row, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvMemberTitle)
        private val tvSubtitle: TextView? = view.findViewById(R.id.tvMemberSubtitle)

        fun bind(item: GroupMemberUi) {
            tvTitle.text = item.label
            tvSubtitle?.text = if (item.isFriend) "Amigo" else "No amigo"
        }
    }

    private object Diff : DiffUtil.ItemCallback<GroupMemberUi>() {
        override fun areItemsTheSame(oldItem: GroupMemberUi, newItem: GroupMemberUi) = oldItem.uid == newItem.uid
        override fun areContentsTheSame(oldItem: GroupMemberUi, newItem: GroupMemberUi) = oldItem == newItem
    }
}