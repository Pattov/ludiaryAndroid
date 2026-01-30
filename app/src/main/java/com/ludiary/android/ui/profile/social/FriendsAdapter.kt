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
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.model.FriendStatus

/**
 * Adapter para mostrar la lista de amigos en el perfil del usuario.
 * @param onClick Acción al pulsar sobre un amigo.
 * @param onEditNickname Acción para editar el alias del amigo.
 * @param onDeleteFriend Acción para eliminar al amigo.
 */
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

    /**
     * Asocia los datos del amigo al ViewHolder.
     */
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder que representa un amigo en la lista.
     * Gestiona la visibilidad de acciones según el estado de la amistad.
     * @param view Vista inflada correspondiente a una fila de amigo.
     * @param onClick Callback ejecutado al pulsar sobre la fila completa. Devuelve la entidad [FriendEntity] asociada.
     * @param onEditNickname Callback ejecutado al pulsar el botón de editar alias.
     * @param onDeleteFriend Callback ejecutado al pulsar el botón de eliminar amigo.
     */
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

        private val btnAccept: View? = view.findViewById(R.id.btnAccept)
        private val btnReject: View? = view.findViewById(R.id.btnReject)


        /**
         * Vincula los datos de un [FriendEntity] con la vista.
         * @param item Amigo a representar.
         */
        fun bind(item: FriendEntity) {
            val ctx = itemView.context
            val isAccepted = item.status == FriendStatus.ACCEPTED

            val title = item.nickname?.takeIf { it.isNotBlank() }
                ?: item.displayName?.takeIf { it.isNotBlank() }
                ?: ctx.getString(R.string.friends_default_title)

            val codeFull = item.friendCode
                ?.takeIf { it.isNotBlank() }
                ?.let { ctx.getString(R.string.friends_code_format, it) }
                .orEmpty()

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
                btnEdit.setOnClickListener { onEditNickname(item.id, item.nickname) }
                btnDelete.setOnClickListener { onDeleteFriend(item.id) }
            }

            itemView.setOnClickListener { onClick(item) }
        }
    }

    /**
     * DiffUtil para optimizar actualizaciones del RecyclerView.
     *
     * Prioriza `friendUid` cuando existe (datos sincronizados), usando `id` local como fallback.
     */
    private object Diff : DiffUtil.ItemCallback<FriendEntity>() {
        override fun areItemsTheSame(oldItem: FriendEntity, newItem: FriendEntity): Boolean {
            val oldKey = oldItem.friendUid?.takeIf { it.isNotBlank() } ?: oldItem.id.toString()
            val newKey = newItem.friendUid?.takeIf { it.isNotBlank() } ?: newItem.id.toString()
            return oldKey == newKey
        }

        override fun areContentsTheSame(oldItem: FriendEntity, newItem: FriendEntity) =
            oldItem == newItem
    }
}