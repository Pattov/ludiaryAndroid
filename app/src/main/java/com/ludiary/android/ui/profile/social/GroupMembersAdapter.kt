package com.ludiary.android.ui.profile.social

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ludiary.android.R

/**
 * Modelo de UI para representar un miembro de un grupo.
 * @property uid Identificador único del usuario.
 * @property label Nombre visible del miembro (nickname, displayName o fallback).
 * @property isFriend Indica si el miembro es amigo del usuario actual.
 */
data class GroupMemberUi(
    val uid: String,
    val label: String,
    val isFriend: Boolean
)

/**
 * Adapter para mostrar la lista de miembros de un grupo.
 */
class GroupMembersAdapter :
    ListAdapter<GroupMemberUi, GroupMembersAdapter.VH>(Diff) {

    /**
     * Crea un nuevo ViewHolder inflando el layout del miembro.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_member_row, parent, false)
        return VH(view)
    }

    /**
     * Vincula un elemento de la lista con su ViewHolder.
     */
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder que representa visualmente a un miembro del grupo.
     * @param view Vista inflada de la fila.
     */
    class VH(view: View) : RecyclerView.ViewHolder(view) {

        private val tvTitle: TextView = view.findViewById(R.id.tvMemberTitle)
        private val tvSubtitle: TextView? = view.findViewById(R.id.tvMemberSubtitle)

        /**
         * Asocia los datos del miembro a la vista.
         *
         * @param item Modelo de UI del miembro.
         */
        fun bind(item: GroupMemberUi) {
            itemView.context

            tvTitle.text = item.label

            tvSubtitle?.setText(
                if (item.isFriend) {
                    R.string.group_member_friend
                } else {
                    R.string.group_member_not_friend
                }
            )
        }
    }

    /**
     * DiffUtil para optimizar las actualizaciones del RecyclerView.
     */
    private object Diff : DiffUtil.ItemCallback<GroupMemberUi>() {

        override fun areItemsTheSame(
            oldItem: GroupMemberUi,
            newItem: GroupMemberUi
        ): Boolean = oldItem.uid == newItem.uid

        override fun areContentsTheSame(
            oldItem: GroupMemberUi,
            newItem: GroupMemberUi
        ): Boolean = oldItem == newItem
    }
}