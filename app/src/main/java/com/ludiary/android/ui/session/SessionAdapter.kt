package com.ludiary.android.ui.session

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ludiary.android.R
import com.ludiary.android.data.local.entity.SessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionsAdapter(
    private val onItemClick: (SessionEntity) -> Unit,
    private val onEditClick: (SessionEntity) -> Unit = {},
    private val onDeleteClick: (SessionEntity) -> Unit = {}
) : ListAdapter<SessionEntity, SessionsAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SessionEntity>() {
            override fun areItemsTheSame(oldItem: SessionEntity, newItem: SessionEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: SessionEntity, newItem: SessionEntity) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_session, parent, false)
        return VH(v, onItemClick, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        itemView: View,
        private val onItemClick: (SessionEntity) -> Unit,
        private val onEditClick: (SessionEntity) -> Unit,
        private val onDeleteClick: (SessionEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        private val textPlayers: TextView = itemView.findViewById(R.id.textPlayers)
        private val textDuration: TextView = itemView.findViewById(R.id.textDuration)
        private val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)

        private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        fun bind(item: SessionEntity) {
            val ctx = itemView.context

            // 1) Título
            textTitle.text = item.gameTitle

            val dateStr = df.format(Date(item.playedAt))
            textPlayers.text = ctx.getString(R.string.session_item_date, dateStr)

            val duration = item.durationMinutes
            val rating = item.overallRating
            textDuration.text = when {
                duration != null && rating != null ->
                    ctx.getString(R.string.session_item_duration_rating, duration, rating)
                duration != null ->
                    ctx.getString(R.string.session_item_duration, duration)
                rating != null ->
                    ctx.getString(R.string.session_item_rating, rating)
                else ->
                    ctx.getString(R.string.session_item_empty_meta)
            }

            itemView.setOnClickListener { onItemClick(item) }
            btnEdit.setOnClickListener { onEditClick(item) }
            btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }
}
