package com.ludiary.android.ui.profile.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.ludiary.android.R
import com.ludiary.android.data.local.entity.GroupEntity
import com.ludiary.android.viewmodel.GroupRowUi

class GroupsAdapter(
    private val onOpen: (GroupEntity) -> Unit,
    private val onInvite: (GroupEntity) -> Unit,
    private val onDelete: (GroupEntity) -> Unit
) : ListAdapter<GroupRowUi, GroupsAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_row, parent, false)
        return VH(v, onOpen, onInvite, onDelete)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        view: View,
        private val onOpen: (GroupEntity) -> Unit,
        private val onInvite: (GroupEntity) -> Unit,
        private val onDelete: (GroupEntity) -> Unit
    ) : RecyclerView.ViewHolder(view) {

        private val tvAvatarLetter: TextView = view.findViewById(R.id.tvAvatarLetter)
        private val tvTitle: TextView = view.findViewById(R.id.tvGroupTitle)
        private val tvSubtitle: TextView = view.findViewById(R.id.tvGroupSubtitle)
        private val btnInvite: MaterialButton = view.findViewById(R.id.btnInvite)
        private val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)

        fun bind(row: GroupRowUi) {
            val item = row.group

            tvTitle.text = item.nameSnapshot
            tvAvatarLetter.text = item.nameSnapshot.firstOrNull()?.uppercase() ?: "G"

            val n = row.membersCount
            tvSubtitle.text = if (n == 1) "1 miembro" else "$n miembros"
            tvSubtitle.visibility = View.VISIBLE

            itemView.setOnClickListener {
                itemView.isEnabled = false
                onOpen(item)
                itemView.postDelayed({ itemView.isEnabled = true }, 500)
            }

            btnInvite.setOnClickListener { onInvite(item) }
            btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<GroupRowUi>() {
        override fun areItemsTheSame(oldItem: GroupRowUi, newItem: GroupRowUi) =
            oldItem.group.groupId == newItem.group.groupId

        override fun areContentsTheSame(oldItem: GroupRowUi, newItem: GroupRowUi) =
            oldItem == newItem
    }
}