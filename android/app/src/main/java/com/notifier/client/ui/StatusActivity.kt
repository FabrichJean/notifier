package com.notifier.client.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.notifier.client.R
import com.notifier.client.databinding.ActivityStatusBinding
import com.notifier.client.model.NotificationData
import com.notifier.client.network.ConnectionStatus
import com.notifier.client.service.NotifierWebSocketService
import kotlinx.coroutines.launch

class StatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatusBinding
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsetsAsPadding()

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        binding.notificationsList.adapter = adapter

        binding.editConfigButton.setOnClickListener { goToConfig() }
        binding.disconnectButton.setOnClickListener {
            NotifierWebSocketService.stop(this)
            goToConfig()
        }

        observeConnectionStatus()
        observeReceivedNotifications()
    }

    private fun goToConfig() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun observeConnectionStatus() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                NotifierWebSocketService.connectionStatus.collect { status ->
                    binding.statusLabel.text = when (status) {
                        ConnectionStatus.CONNECTED -> getString(R.string.status_connected)
                        ConnectionStatus.CONNECTING -> getString(R.string.status_connecting)
                        ConnectionStatus.DISCONNECTED -> getString(R.string.status_disconnected)
                    }
                }
            }
        }
    }

    private fun observeReceivedNotifications() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                NotifierWebSocketService.receivedNotifications.collect { list ->
                    binding.emptyLabel.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    adapter.clear()
                    adapter.addAll(list.map(::formatEntry))
                }
            }
        }
    }

    private fun formatEntry(data: NotificationData): String {
        val time = data.createdAt.replace("T", " ").take(19)
        val typeLabel = if (data.type == "alarm") "[ALARME]" else "[NOTIF]"
        return "$typeLabel $time\n${data.title} — ${data.body}"
    }
}
