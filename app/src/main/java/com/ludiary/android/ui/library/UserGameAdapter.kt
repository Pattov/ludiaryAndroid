package com.ludiary.android.ui.library

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ludiary.android.R
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.ludiary.android.data.model.UserGame

/**
 * Adaptador para la lista de juegos del usuarios.
 *
 * @param onEdit Callback para editar un juego.
 * @param onDelete Callback para borrar un juego.
 */
class UserGameAdapter(
    private val onEdit: (gameId: String) -> Unit,
    private val onDelete: (gameId: UserGame) -> Unit
) : ListAdapter <UserGame, UserGameAdapter.ViewHolder>(DiffCallback()){

    /**
     * ViewHolder para cada elemento de la lista.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val titleText: TextView = view.findViewById(R.id.textGameTitle)
        val metaText: TextView = view.findViewById(R.id.textGameMeta)
        val btnEdit: Button = view.findViewById(R.id.btnEditGame)
        val btnDelete: Button = view.findViewById(R.id.btnDeleteGame)
    }

    /**
     * Vincula cada elemento del modulo [UserGame] con la vista del ViewHolder.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = getItem(position)

        holder.titleText.text = game.titleSnapshot

        val language = game.language ?: "—"
        val condition = game.condition ?: "—"

        holder.metaText.text = holder.itemView.context.getString(
            R.string.game_meta_format,
            language,
            condition
        )

        if (game.id.isNotBlank()) {
            holder.btnEdit.setOnClickListener { onEdit(game.id) }
            holder.btnDelete.setOnClickListener { onDelete(game) }
        }
    }

    /**
     * Infla el layout de cada elemento de la lista.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder{
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_game, parent, false)
        return ViewHolder(view)
    }

    /**
     * Compara los elementos de la lista.
     * Esto permite animaciones eficientes y evita redibujar tarjetas cuando no ha cambiado nada.
     */
    class DiffCallback : DiffUtil.ItemCallback<UserGame>(){
        override fun areItemsTheSame(a: UserGame, b: UserGame) = a.id == b.id
        override fun areContentsTheSame(a: UserGame, b: UserGame) = a == b
    }
}