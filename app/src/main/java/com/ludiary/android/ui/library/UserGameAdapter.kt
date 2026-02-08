package com.ludiary.android.ui.library

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ludiary.android.R
import com.ludiary.android.data.model.UserGame
import com.ludiary.android.viewmodel.LibraryItem

/**
 * Adaptador para la lista de juegos del usuarios.
 *
 * @param onEdit Callback para editar un juego.
 * @param onDelete Callback para borrar un juego.
 */
class UserGameAdapter(
    private val onEdit: (gameId: String) -> Unit,
    private val onDelete: (game: UserGame) -> Unit
) : ListAdapter<LibraryItem, UserGameAdapter.ViewHolder>(DiffCallback()) {

    /**
     * ViewHolder para cada elemento de la lista.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.textGameTitle)
        val metaText: TextView = view.findViewById(R.id.textGameMeta)
        val ownerText: TextView = view.findViewById(R.id.textOwnerInfo)
        val btnEdit: Button = view.findViewById(R.id.btnEditGame)
        val btnDelete: Button = view.findViewById(R.id.btnDeleteGame)
    }

    /**
     * Vincula cada elemento del modulo [UserGame] con la vista del ViewHolder.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val game = item.game

        holder.titleText.text = game.titleSnapshot

        val language = game.language ?: "—"
        val condition = game.condition ?: "—"

        holder.metaText.text = holder.itemView.context.getString(
            R.string.game_meta_format,
            language,
            condition
        )

        // Mostrar info de propietario si no soy yo
        if (item.ownerName != "Yo" && (item.ownerName != null || item.groupName != null)) {
            holder.ownerText.visibility = View.VISIBLE
            val info = if (!item.groupName.isNullOrBlank()) {
                "${item.groupName} | ${item.ownerName ?: ""}"
            } else {
                item.ownerName ?: ""
            }
            holder.ownerText.text = info
        } else {
            holder.ownerText.visibility = View.GONE
        }

        if (game.id.isNotBlank()) {
            // Solo permitir editar/borrar si es mío
            val isMine = item.ownerName == "Yo"

            holder.btnEdit.visibility = if (isMine) View.VISIBLE else View.GONE
            holder.btnDelete.visibility = if (isMine) View.VISIBLE else View.GONE

            if (isMine) {
                holder.btnEdit.setOnClickListener { onEdit(game.id) }
                holder.btnDelete.setOnClickListener { onDelete(game) }
            }
        }
    }

    /**
     * Infla el layout de cada elemento de la lista.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_game, parent, false)
        return ViewHolder(view)
    }

    /**
     * Compara los elementos de la lista.
     */
    class DiffCallback : DiffUtil.ItemCallback<LibraryItem>() {
        override fun areItemsTheSame(a: LibraryItem, b: LibraryItem) = a.game.id == b.game.id
        override fun areContentsTheSame(a: LibraryItem, b: LibraryItem) = a == b
    }
}