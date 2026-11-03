package com.togetherweserve.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.togetherweserve.app.data.local.EventEntity
import com.togetherweserve.app.databinding.ItemEventBinding
import java.text.SimpleDateFormat
import java.util.Locale

class EventAdapter(
    private val onJoinClick: (EventEntity) -> Unit,
    private val onItemClick: (EventEntity) -> Unit
) : ListAdapter<EventEntity, EventAdapter.EventViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(private val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("EEE HH:mm", Locale.getDefault())

        fun bind(event: EventEntity) {
            binding.tvCause.text = event.cause.uppercase(Locale.getDefault())
            binding.tvTitle.text = event.title
            binding.tvDateGoing.text = "${dateFormat.format(event.dateTime)} · ${event.volunteersGoing} going"
            binding.btnJoin.setOnClickListener { onJoinClick(event) }
            binding.root.setOnClickListener { onItemClick(event) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<EventEntity>() {
            override fun areItemsTheSame(a: EventEntity, b: EventEntity) = a.eventId == b.eventId
            override fun areContentsTheSame(a: EventEntity, b: EventEntity) = a == b
        }
    }
}
