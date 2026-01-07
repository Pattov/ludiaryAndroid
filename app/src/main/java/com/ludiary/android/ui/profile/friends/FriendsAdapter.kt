package com.ludiary.android.ui.profile.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ludiary.android.data.local.entity.FriendEntity
import com.ludiary.android.databinding.ItemFriendRowBinding

class FriendsAdapter(
    private val onClick: (FriendEntity) -> Unit
) : ListAdapter<FriendEntity, FriendsAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        return VH(ItemFriendRowBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(
        private val b: ItemFriendRowBinding
    ) : RecyclerView.ViewHolder(b.root) {

        fun bind(item: FriendEntity) {
            val nickname = item.nickname?.trim().orEmpty()
            val displayName = item.displayName?.trim().orEmpty()
            val email = item.email.trim()

            val title = when {
                nickname.isNotBlank() -> nickname
                displayName.isNotBlank() -> displayName
                else -> email
            }

            val subtitle = if (title.equals(email, ignoreCase = true)) "" else email

            b.tvFriendTitle.text = title
            b.tvFriendSubtitle.text = subtitle

            b.tvFriendSubtitle.visibility = if (subtitle.isBlank()) android.view.View.GONE else android.view.View.VISIBLE

            b.root.setOnClickListener { onClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<FriendEntity>() {
        override fun areItemsTheSame(oldItem: FriendEntity, newItem: FriendEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: FriendEntity, newItem: FriendEntity) =
            oldItem == newItem
    }
}