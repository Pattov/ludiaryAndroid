package com.ludiary.android.ui.profile.friends

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ludiary.android.R
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.data.model.FriendStatus
import com.ludiary.android.viewmodel.FriendRowUi
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

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
        fun bind(h: FriendRowUi.Header) { tv.text = h.title }
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

        fun bind(item: FriendEntity) {
            val title = item.nickname?.takeIf { it.isNotBlank() }
                ?: item.displayName?.takeIf { it.isNotBlank() }
                ?: "Amigo"

            val subtitle = when (item.status) {
                FriendStatus.PENDING_INCOMING -> "Quiere ser tu amigo"
                FriendStatus.PENDING_OUTGOING,
                FriendStatus.PENDING_OUTGOING_LOCAL -> "Solicitud enviada"
                else -> ""
            }

            tvTitle.text = title
            tvSubtitle.text = subtitle
            tvSubtitle.isVisible = subtitle.isNotBlank()

            val showActions = item.status == FriendStatus.PENDING_INCOMING
            btnAccept.isVisible = showActions
            btnReject.isVisible = showActions

            btnAccept.setOnClickListener(null)
            btnReject.setOnClickListener(null)

            if (showActions) {
                btnAccept.setOnClickListener { onAccept(item.id) }
                btnReject.setOnClickListener { onReject(item.id) }
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
