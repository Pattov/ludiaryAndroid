package com.ludiary.android.ui.profile.social

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ludiary.android.R

/**
 * Adapter para la lista de amigos a los que se puede invitar a un grupo.
 * @param onClick Callback al pulsar un item habilitado (invitar).
 * @param onLongClickPending Callback al hacer long press sobre un item pendiente (cancelar invitación).
 */
class InviteFriendsAdapter(
    private val onClick: (InviteFriendRow) -> Unit,
    private val onLongClickPending: (InviteFriendRow) -> Unit
) : ListAdapter<InviteFriendRow, InviteFriendsAdapter.VH>(Diff) {

    /**
     * Infla el layout de fila y crea el ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invite_friend_row, parent, false)
        return VH(v, onClick, onLongClickPending)
    }

    /**
     * Bindea el item actual al ViewHolder.
     */
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder de fila: label + estado.
     *
     * @param view View inflada de [R.layout.item_invite_friend_row].
     * @param onClick Acción de invitar (solo si el item está habilitado).
     * @param onLongClickPending Acción de cancelar invitación (solo si isPending).
     */
    class VH(
        view: View,
        private val onClick: (InviteFriendRow) -> Unit,
        private val onLongClickPending: (InviteFriendRow) -> Unit
    ) : RecyclerView.ViewHolder(view) {

        private val tvLabel: TextView = view.findViewById(R.id.tvInviteFriendLabel)
        private val tvState: TextView = view.findViewById(R.id.tvInviteFriendState)

        /**
         * Aplica el estado visual de la fila y configura listeners
         * @param item Fila a pintar.
         */
        fun bind(item: InviteFriendRow) {
            itemView.context

            tvLabel.text = item.label

            val stateResId = when {
                item.isMember -> R.string.invite_friend_state_member
                item.isPending -> R.string.invite_friend_state_pending
                else -> null
            }

            tvState.isVisible = stateResId != null
            if (stateResId != null) {
                tvState.setText(stateResId)
            } else {
                tvState.text = ""
            }

            val enabled = !item.isMember && !item.isPending
            itemView.isEnabled = enabled
            itemView.alpha = if (enabled) 1f else 0.45f

            itemView.setOnClickListener(null)
            itemView.setOnLongClickListener(null)

            // Click: solo si está habilitado (invitable).
            itemView.setOnClickListener {
                if (enabled) onClick(item)
            }

            // Long press: solo para cancelar invitaciones pendientes.
            itemView.setOnLongClickListener {
                if (item.isPending) {
                    onLongClickPending(item)
                    true
                } else {
                    false
                }
            }
        }
    }

    /**
     * DiffUtil para optimizar updates del RecyclerView.
     */
    private object Diff : DiffUtil.ItemCallback<InviteFriendRow>() {
        override fun areItemsTheSame(oldItem: InviteFriendRow, newItem: InviteFriendRow): Boolean =
            oldItem.friendUid == newItem.friendUid

        override fun areContentsTheSame(oldItem: InviteFriendRow, newItem: InviteFriendRow): Boolean =
            oldItem == newItem
    }
}