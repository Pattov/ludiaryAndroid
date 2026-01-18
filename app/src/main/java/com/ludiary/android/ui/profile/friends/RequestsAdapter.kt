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
import com.ludiary.android.viewmodel.FriendRowUi

class RequestsAdapter(
    private val onClick: (FriendEntity) -> Unit,
    private val onAccept: (Long) -> Unit,
    private val onReject: (Long) -> Unit
) : ListAdapter<FriendRowUi, RecyclerView.ViewHolder>(Diff) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is FriendRowUi.Header -> TYPE_HEADER
            is FriendRowUi.Item -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val v = inflater.inflate(R.layout.item_requests_header, parent, false)
                HeaderVH(v)
            }
            else -> {
                val v = inflater.inflate(R.layout.item_friend_row, parent, false)
                ItemVH(v, onClick, onAccept, onReject)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is FriendRowUi.Header -> (holder as HeaderVH).bind(row)
            is FriendRowUi.Item -> (holder as ItemVH).bind(row.friend)
        }
    }

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tv: TextView = view.findViewById(R.id.tvHeader)
        fun bind(h: FriendRowUi.Header) {
            tv.text = h.title
        }
    }

    class ItemVH(
        view: View,
        private val onClick: (FriendEntity) -> Unit,
        private val onAccept: (Long) -> Unit,
        private val onReject: (Long) -> Unit
    ) : RecyclerView.ViewHolder(view) {

        private val tvTitle: TextView = view.findViewById(R.id.tvFriendTitle)
        private val tvSubtitle: TextView = view.findViewById(R.id.tvFriendSubtitle)

        private val btnAccept: View = view.findViewById(R.id.btnAccept)
        private val btnReject: View = view.findViewById(R.id.btnReject)

        // OJO: item_friend_row también tiene btnEdit/btnDelete.
        // En solicitudes NO deben aparecer nunca.
        private val btnEdit: View? = view.findViewById(R.id.btnEdit)
        private val btnDelete: View? = view.findViewById(R.id.btnDelete)

        fun bind(item: FriendEntity) {
            val baseName = item.nickname?.takeIf { it.isNotBlank() }
                ?: item.displayName?.takeIf { it.isNotBlank() }
                ?: "Amigo"

            // En solicitudes: código abreviado para que no se coma el espacio de los iconos
            val codeShort = item.friendCode
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    val last = it.trim().takeLast(5)
                    "#…$last"
                }
                .orEmpty()

            tvTitle.text = if (codeShort.isBlank()) baseName else "$baseName · $codeShort"

            val isIncoming = item.status == FriendStatus.PENDING_INCOMING
            val isOutgoing = item.status == FriendStatus.PENDING_OUTGOING ||
                    item.status == FriendStatus.PENDING_OUTGOING_LOCAL

            val subtitle = when {
                isIncoming -> "Quiere ser tu amigo"
                isOutgoing -> "Solicitud enviada"
                else -> ""
            }

            tvSubtitle.text = subtitle
            tvSubtitle.isVisible = subtitle.isNotBlank()

            // ✅ Reglas exactas:
            // - Recibidas: Aceptar + Rechazar
            // - Enviadas: solo Cancelar (Rechazar reutilizado)
            btnAccept.isVisible = isIncoming
            btnReject.isVisible = isIncoming || isOutgoing

            // Nunca mostrar acciones de amigo en solicitudes
            btnEdit?.isVisible = false
            btnDelete?.isVisible = false

            // Limpieza listeners
            btnAccept.setOnClickListener(null)
            btnReject.setOnClickListener(null)

            if (isIncoming) {
                btnAccept.setOnClickListener { onAccept(item.id) }
                btnReject.setOnClickListener { onReject(item.id) } // rechazar
            } else if (isOutgoing) {
                btnReject.setOnClickListener { onReject(item.id) } // cancelar
            }

            itemView.setOnClickListener { onClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<FriendRowUi>() {
        override fun areItemsTheSame(oldItem: FriendRowUi, newItem: FriendRowUi): Boolean {
            return when {
                oldItem is FriendRowUi.Header && newItem is FriendRowUi.Header ->
                    oldItem.title == newItem.title
                oldItem is FriendRowUi.Item && newItem is FriendRowUi.Item ->
                    oldItem.friend.id == newItem.friend.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: FriendRowUi, newItem: FriendRowUi): Boolean {
            return oldItem == newItem
        }
    }
}