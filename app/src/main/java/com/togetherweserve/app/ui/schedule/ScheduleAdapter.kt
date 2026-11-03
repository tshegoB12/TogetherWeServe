package com.togetherweserve.app.ui.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.togetherweserve.app.R
import com.togetherweserve.app.data.local.RegistrationEntity
import com.togetherweserve.app.databinding.ItemScheduleBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ScheduleAdapter : ListAdapter<RegistrationEntity, ScheduleAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class VH(private val binding: ItemScheduleBinding) : RecyclerView.ViewHolder(binding.root) {
        private val df = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
        fun bind(item: RegistrationEntity) {
            binding.tvEventTitle.text = item.eventTitle
            val ctx = binding.root.context
            val statusLabel = ctx.getString(
                if (item.status == "confirmed") R.string.status_confirmed else R.string.status_pending_sync
            )
            binding.tvDateStatus.text = "${df.format(item.eventDateTime)} · $statusLabel"
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RegistrationEntity>() {
            override fun areItemsTheSame(a: RegistrationEntity, b: RegistrationEntity) = a.registrationId == b.registrationId
            override fun areContentsTheSame(a: RegistrationEntity, b: RegistrationEntity) = a == b
        }
    }
}
