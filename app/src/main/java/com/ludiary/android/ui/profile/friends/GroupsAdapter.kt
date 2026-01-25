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
import com.ludiary.android.viewmodel.GroupRowUi

class GroupsAdapter(
    private val onOpen: (GroupRowUi) -> Unit,
    private val onInvite: (GroupRowUi) -> Unit,
    private val onDelete: (GroupRowUi) -> Unit
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
        private val onOpen: (GroupRowUi) -> Unit,
        private val onInvite: (GroupRowUi) -> Unit,
        private val onDelete: (GroupRowUi) -> Unit
    ) : RecyclerView.ViewHolder(view) {

        private val tvAvatarLetter: TextView = view.findViewById(R.id.tvAvatarLetter)
        private val tvTitle: TextView = view.findViewById(R.id.tvGroupTitle)
        private val tvSubtitle: TextView = view.findViewById(R.id.tvGroupSubtitle)

        private val btnInvite: MaterialButton = view.findViewById(R.id.btnInvite)
        private val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)

        fun bind(item: GroupRowUi) {
            val g = item.group
            tvTitle.text = g.nameSnapshot

            tvAvatarLetter.text =
                g.nameSnapshot.firstOrNull()?.uppercase() ?: "G"

            // Subtítulo: X miembros
            tvSubtitle.visibility = View.VISIBLE
            val n = item.membersCount
            tvSubtitle.text = if (n == 1) "1 miembro" else "$n miembros"

            // Abrir detalle (anti doble click)
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