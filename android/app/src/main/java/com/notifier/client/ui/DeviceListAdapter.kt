package com.notifier.client.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notifier.client.R
import com.notifier.client.databinding.ItemDeviceBinding
import com.notifier.client.model.RemoteDevice

class DeviceListAdapter(
    private val onClick: (RemoteDevice) -> Unit,
) : ListAdapter<RemoteDevice, DeviceListAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class ViewHolder(private val binding: ItemDeviceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(device: RemoteDevice, onClick: (RemoteDevice) -> Unit) {
            binding.deviceName.text = device.name

            val isRevoked = device.revokedAt != null
            val colorRes = if (isRevoked) R.color.status_disconnected else R.color.status_connected
            val labelRes = if (isRevoked) R.string.device_revoked else R.string.device_active
            binding.deviceStatusLabel.setText(labelRes)
            binding.statusDot.background.setTint(ContextCompat.getColor(binding.root.context, colorRes))

            binding.root.setOnClickListener { onClick(device) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RemoteDevice>() {
            override fun areItemsTheSame(oldItem: RemoteDevice, newItem: RemoteDevice) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: RemoteDevice, newItem: RemoteDevice) =
                oldItem == newItem
        }
    }
}
