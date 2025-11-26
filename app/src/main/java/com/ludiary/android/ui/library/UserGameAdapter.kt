package com.ludiary.android.ui.library

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ludiary.android.R
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.ludiary.android.data.model.UserGame

class UserGameAdapter(
    private val onEdit: (gameId: String) -> Unit,
    private val onDelete: (gameId: String) -> Unit
) : ListAdapter <UserGame, UserGameAdapter.ViewHolder>(DiffCallback()){

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val title: TextView = view.findViewById(R.id.textTitle)
        val players: TextView = view.findViewById(R.id.textPlayers)
        val duration : TextView = view.findViewById(R.id.textDuration)
        val image : ImageView = view.findViewById(R.id.imageCover)
        val btnEdit: Button = view.findViewById(R.id.btnEdit)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = getItem(position)
        holder.title.text = game.titleSnapshot
        holder.players.text = game.language ?: ""
        holder.duration.text = game.condition ?: ""

        holder.btnEdit.setOnClickListener { onEdit(game.id) }
        holder.btnDelete.setOnClickListener { onDelete(game.id) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder{
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_game, parent, false)
        return ViewHolder(view)
    }

    class DiffCallback : DiffUtil.ItemCallback<UserGame>(){
        override fun areItemsTheSame(a: UserGame, b: UserGame) = a.id == b.id
        override fun areContentsTheSame(a: UserGame, b: UserGame) = a == b
    }
}