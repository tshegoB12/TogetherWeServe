package com.togetherweserve.app.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.togetherweserve.app.databinding.ItemNotificationBinding
import com.togetherweserve.app.utils.NotificationItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Lists received push alerts; tapping one opens the related event. */
class NotificationAdapter(
    private val onClick: (NotificationItem) -> Unit
) : ListAdapter<NotificationItem, NotificationAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position), onClick)

    class VH(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {
        private val df = SimpleDateFormat("EEE, dd MMM · HH:mm", Locale.getDefault())
        fun bind(item: NotificationItem, onClick: (NotificationItem) -> Unit) {
            binding.tvNotifTitle.text = item.title
            binding.tvNotifBody.text = item.body
            binding.tvNotifTime.text = df.format(Date(item.receivedAt))
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NotificationItem>() {
            override fun areItemsTheSame(a: NotificationItem, b: NotificationItem) = a.id == b.id
            override fun areContentsTheSame(a: NotificationItem, b: NotificationItem) = a == b
        }
    }
}
