import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ludiary.android.data.local.entity.SessionEntity

class SessionsAdapter(
    private val onClick: (SessionEntity) -> Unit
) : androidx.recyclerview.widget.ListAdapter<SessionEntity, SessionsAdapter.VH>(DIFF) {

    companion object {
        private val DIFF =
            object : androidx.recyclerview.widget.DiffUtil.ItemCallback<SessionEntity>() {
                override fun areItemsTheSame(
                    oldItem: SessionEntity,
                    newItem: SessionEntity
                ): Boolean =
                    oldItem.id == newItem.id

                override fun areContentsTheSame(
                    oldItem: SessionEntity,
                    newItem: SessionEntity
                ): Boolean =
                    oldItem == newItem
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return VH(v, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(itemView: View, private val onClick: (SessionEntity) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        private val text1: TextView = itemView.findViewById(android.R.id.text1)
        private val text2: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(item: SessionEntity) {
            text1.text = item.gameTitle
            val date = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                .format(java.util.Date(item.playedAt))

            text2.text = date

            itemView.setOnClickListener { onClick(item) }
        }
    }
}