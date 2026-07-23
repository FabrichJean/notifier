package com.notifier.client.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.notifier.client.R
import com.notifier.client.databinding.ActivityStatusBinding
import com.notifier.client.service.NotifierWebSocketService
import kotlinx.coroutines.launch

class StatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatusBinding
    private lateinit var adapter: NotificationListAdapter
    private var filterDeviceId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsetsAsPadding()
        useLightSystemBarIcons()

        filterDeviceId = intent.getStringExtra(EXTRA_DEVICE_ID)
        intent.getStringExtra(EXTRA_DEVICE_NAME)?.let { name ->
            binding.screenTitle.text = name
        }

        adapter = NotificationListAdapter(showDeviceName = filterDeviceId == null)
        binding.notificationsList.layoutManager = LinearLayoutManager(this)
        binding.notificationsList.adapter = adapter

        binding.bottomNav.selectedItemId = R.id.nav_notifications
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.nav_devices) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            true
        }

        observeReceivedNotifications()
    }

    private fun observeReceivedNotifications() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                NotifierWebSocketService.receivedNotifications.collect { list ->
                    val filtered = filterDeviceId?.let { id ->
                        list.filter { it.targetDeviceId == id }
                    } ?: list
                    binding.emptyLabel.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
                    adapter.submitList(filtered)
                }
            }
        }
    }

    companion object {
        const val EXTRA_DEVICE_ID = "extra_device_id"
        const val EXTRA_DEVICE_NAME = "extra_device_name"
    }
}
