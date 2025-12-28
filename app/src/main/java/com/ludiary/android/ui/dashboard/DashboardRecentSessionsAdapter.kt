package com.ludiary.android.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ludiary.android.R
import com.ludiary.android.data.local.entity.SessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardRecentSessionsAdapter :
    ListAdapter<SessionEntity, DashboardRecentSessionsAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SessionEntity>() {
            override fun areItemsTheSame(oldItem: SessionEntity, newItem: SessionEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: SessionEntity, newItem: SessionEntity) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dashboard_recent_session, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
        private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        fun bind(s: SessionEntity) {
            tvTitle.text = s.gameTitle

            val dateStr = df.format(Date(s.playedAt))
            val durationStr = s.durationMinutes?.let { " · ${it} min" }.orEmpty()
            val ratingStr = s.overallRating?.let { " · ⭐ $it" }.orEmpty()

            tvSubtitle.text = "$dateStr$durationStr$ratingStr"
        }
    }
}