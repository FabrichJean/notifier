package com.notifier.client.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notifier.client.R
import com.notifier.client.databinding.ItemNotificationBinding
import com.notifier.client.model.NotificationData

class NotificationListAdapter(
    private val showDeviceName: Boolean,
) : ListAdapter<NotificationData, NotificationListAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), showDeviceName)
    }

    class ViewHolder(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data: NotificationData, showDeviceName: Boolean) {
            val context = binding.root.context
            val isAlarm = data.type == "alarm"

            binding.notifTitle.text = data.title
            binding.notifBody.text = data.body
            binding.urgentBadge.visibility = if (isAlarm) View.VISIBLE else View.GONE

            val iconRes = if (isAlarm) R.drawable.ic_warning else R.drawable.ic_bell
            val bgColorRes = if (isAlarm) R.color.icon_bg_alert else R.color.icon_bg_message
            binding.typeIcon.setImageResource(iconRes)
            binding.iconBackground.background.setTint(ContextCompat.getColor(context, bgColorRes))

            val time = data.createdAt.replace("T", " ").take(19)
            binding.notifTime.text = if (showDeviceName) {
                "${data.targetDeviceName ?: "Tous"} · $time"
            } else {
                time
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NotificationData>() {
            override fun areItemsTheSame(oldItem: NotificationData, newItem: NotificationData) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: NotificationData, newItem: NotificationData) =
                oldItem == newItem
        }
    }
}
