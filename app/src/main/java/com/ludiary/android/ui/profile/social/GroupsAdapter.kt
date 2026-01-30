package com.ludiary.android.ui.profile.social

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

/**
 * Adapter para mostrar la lista de grupos del usuario.
 * @param onOpen Callback al pulsar una fila para abrir el detalle del grupo.
 * @param onInvite Callback para invitar amigos al grupo.
 * @param onDelete Callback para salir/eliminar el grupo.
 */
class GroupsAdapter(
    private val onOpen: (GroupRowUi) -> Unit,
    private val onInvite: (GroupRowUi) -> Unit,
    private val onDelete: (GroupRowUi) -> Unit
) : ListAdapter<GroupRowUi, GroupsAdapter.VH>(Diff) {

    /**
     * Crea y devuelve un [VH] inflando el layout del item de grupo.
     * @param parent ViewGroup padre al que se adjuntará el item.
     * @param viewType Tipo de vista (no utilizado, un único tipo).
     * @return ViewHolder configurado para mostrar un grupo.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_row, parent, false)
        return VH(v, onOpen, onInvite, onDelete)
    }

    /**
     * Vincula los datos de un [GroupRowUi] al [VH] correspondiente.
     *
     * @param holder ViewHolder que representa la fila.
     * @param position Posición del elemento dentro del adapter.
     */
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder de un grupo.
     * @param view Vista inflada del item (layout `item_group_row`).
     * @param onOpen Callback al pulsar la fila para abrir el detalle del grupo.
     * @param onInvite Callback al pulsar el botón de invitar miembros.
     * @param onDelete Callback al pulsar el botón de salir/eliminar grupo.
     */
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

        /**
         * Vincula los datos del grupo con la vista.
         * @param item Modelo de UI que contiene el grupo y metadatos (miembros).
         */
        fun bind(item: GroupRowUi) {
            val group = item.group

            tvTitle.text = group.nameSnapshot

            tvAvatarLetter.text =
                group.nameSnapshot.firstOrNull()?.uppercase() ?: "G"

            val members = item.membersCount
            tvSubtitle.visibility = View.VISIBLE
            tvSubtitle.text = itemView.resources.getQuantityString(
                R.plurals.groups_members_count,
                members,
                members
            )

            // Abrir detalle del grupo
            itemView.setOnClickListener {
                itemView.isEnabled = false
                onOpen(item)
                itemView.postDelayed({ itemView.isEnabled = true }, 500)
            }

            // Invitar amigos al grupo
            btnInvite.setOnClickListener {
                onInvite(item)
            }

            // Salir del grupo
            btnDelete.setOnClickListener {
                onDelete(item)
            }
        }
    }

    /**
     * DiffUtil para optimizar actualizaciones del RecyclerView.
     */
    private object Diff : DiffUtil.ItemCallback<GroupRowUi>() {

        override fun areItemsTheSame(oldItem: GroupRowUi, newItem: GroupRowUi): Boolean =
            oldItem.group.groupId == newItem.group.groupId

        override fun areContentsTheSame(oldItem: GroupRowUi, newItem: GroupRowUi): Boolean =
            oldItem == newItem
    }
}