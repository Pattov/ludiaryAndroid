package com.ludiary.android.ui.sessions

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

/**
 * Adaptador para la lista de sesiones.
 * @param onItemClick Callback al hacer clic en una sesión.
 * @param onEditClick Callback al hacer clic en el botón de edición.
 * @param onDeleteClick Callback al hacer clic en el botón de eliminación
 */
class SessionsAdapter(
    private val onItemClick: (SessionEntity) -> Unit,
    private val onEditClick: (SessionEntity) -> Unit = {},
    private val onDeleteClick: (SessionEntity) -> Unit = {}
) : ListAdapter<SessionEntity, SessionsAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {

        /**
         * DiffUtil para optimizar el refresco del RecyclerView.
         * Compara por ID y por igualdad completa de contenido.
         */
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SessionEntity>() {
            override fun areItemsTheSame(
                oldItem: SessionEntity,
                newItem: SessionEntity
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: SessionEntity,
                newItem: SessionEntity
            ): Boolean = oldItem == newItem
        }
    }

    /**
     * Crea una nueva instancia de ViewHolder.
     * @param parent Grupo al que pertenece el nuevo ViewHolder.
     * @param viewType Tipo de vista.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return ViewHolder(view, onItemClick, onEditClick, onDeleteClick)
    }

    /**
     * Vincula los datos de una sesión con el ViewHolder.
     * @param holder ViewHolder que representa una partida.
     * @param position Posición de la partida en la lista.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder que representa una partida individual.
     * @param itemView Vista de la partida.
     * @param onItemClick Callback al hacer clic en la partida.
     * @param onEditClick Callback al hacer clic en el botón de edición.
     * @param onDeleteClick Callback al hacer clic en el botón de eliminación.
     */
    class ViewHolder(
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

        private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        /**
         * Vincula los datos de una sesión con la vista.
         * @param item Datos de la sesión.
         */
        fun bind(item: SessionEntity) {
            val context = itemView.context

            // Título del juego
            textTitle.text = item.gameTitle

            // Fecha de la partida
            val dateText = dateFormatter.format(Date(item.playedAt))
            textPlayers.text = context.getString(R.string.session_item_date, dateText)

            // Duración y valoración
            textDuration.text = when {
                item.durationMinutes != null && item.overallRating != null -> context.getString(R.string.session_item_duration_rating, item.durationMinutes, item.overallRating)

                item.durationMinutes != null -> context.getString(R.string.session_item_duration, item.durationMinutes)

                item.overallRating != null -> context.getString(R.string.session_item_rating, item.overallRating)

                else -> context.getString(R.string.session_item_empty_meta)
            }

            itemView.setOnClickListener { onItemClick(item) }
            btnEdit.setOnClickListener { onEditClick(item) }
            btnDelete.setOnClickListener { onDeleteClick(item) }
        }
    }
}
