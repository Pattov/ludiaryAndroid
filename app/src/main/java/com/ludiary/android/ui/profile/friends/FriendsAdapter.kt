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
    private val onEditNickname: (Long, String?) -> Unit,
    private val onDeleteFriend: (Long) -> Unit
) : ListAdapter<FriendEntity, FriendsAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_row, parent, false)
        return VH(view, onClick, onEditNickname, onDeleteFriend)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        view: View,
        private val onClick: (FriendEntity) -> Unit,
        private val onEditNickname: (Long, String?) -> Unit,
        private val onDeleteFriend: (Long) -> Unit
    ) : RecyclerView.ViewHolder(view) {

        private val tvTitle: TextView = view.findViewById(R.id.tvFriendTitle)
        private val tvSubtitle: TextView = view.findViewById(R.id.tvFriendSubtitle)

        private val btnEdit: View = view.findViewById(R.id.btnEdit)
        private val btnDelete: View = view.findViewById(R.id.btnDelete)

        // Si existen en tu layout, los ocultamos por seguridad en tab Amigos
        private val btnAccept: View? = view.findViewById(R.id.btnAccept)
        private val btnReject: View? = view.findViewById(R.id.btnReject)

        fun bind(item: FriendEntity) {
            val isAccepted = item.status == FriendStatus.ACCEPTED

            val title = item.nickname?.takeIf { it.isNotBlank() }
                ?: item.displayName?.takeIf { it.isNotBlank() }
                ?: "Amigo"

            val codeFull = item.friendCode?.takeIf { it.isNotBlank() }?.let { "#$it" }.orEmpty()

            tvTitle.text = title
            tvSubtitle.text = codeFull
            tvSubtitle.isVisible = codeFull.isNotBlank()

            // Solo amigos aceptados pueden editar / eliminar
            btnEdit.isVisible = isAccepted
            btnDelete.isVisible = isAccepted

            // Nunca acciones de solicitud en Amigos
            btnAccept?.isVisible = false
            btnReject?.isVisible = false

            btnEdit.setOnClickListener(null)
            btnDelete.setOnClickListener(null)

            if (isAccepted) {
                btnEdit.setOnClickListener {
                    onEditNickname(item.id, item.nickname)
                }
                btnDelete.setOnClickListener {
                    onDeleteFriend(item.id)
                }
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