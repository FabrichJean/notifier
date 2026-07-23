package com.notifier.client.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.notifier.client.AppPrefs
import com.notifier.client.R
import com.notifier.client.databinding.ActivityMainBinding
import com.notifier.client.model.RemoteDevice
import com.notifier.client.network.AdminApi
import com.notifier.client.network.ConnectionStatus
import com.notifier.client.service.NotifierWebSocketService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: AppPrefs
    private lateinit var adapter: DeviceListAdapter

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsetsAsPadding()
        useLightSystemBarIcons()
        prefs = AppPrefs(this)

        adapter = DeviceListAdapter(onClick = { device -> openNotificationsFor(device) })
        binding.deviceList.layoutManager = LinearLayoutManager(this)
        binding.deviceList.adapter = adapter

        binding.batteryOptimizationButton.setOnClickListener { requestIgnoreBatteryOptimizations(this) }
        binding.serverSettingsButton.setOnClickListener {
            startActivity(Intent(this, ServerSettingsActivity::class.java))
        }

        binding.bottomNav.selectedItemId = R.id.nav_devices
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.nav_notifications) {
                startActivity(Intent(this, StatusActivity::class.java))
                finish()
            }
            true
        }

        requestNotificationPermissionIfNeeded()
        observeConnectionStatus()
    }

    override fun onResume() {
        super.onResume()
        if (prefs.isConfigured()) {
            NotifierWebSocketService.start(this)
            refreshDeviceList()
        }
        binding.batteryOptimizationButton.visibility =
            if (isIgnoringBatteryOptimizations(this)) View.GONE else View.VISIBLE
    }

    private fun openNotificationsFor(device: RemoteDevice) {
        val intent = Intent(this, StatusActivity::class.java)
        intent.putExtra(StatusActivity.EXTRA_DEVICE_ID, device.id)
        intent.putExtra(StatusActivity.EXTRA_DEVICE_NAME, device.name)
        startActivity(intent)
    }

    private fun refreshDeviceList() {
        val serverUrl = prefs.serverUrl ?: return
        val adminToken = prefs.adminToken ?: return

        lifecycleScope.launch {
            val devices = withContext(Dispatchers.IO) {
                runCatching { AdminApi.fetchDevices(serverUrl, adminToken) }.getOrDefault(emptyList())
            }
            adapter.submitList(devices)
            binding.emptyLabel.visibility = if (devices.isEmpty()) View.VISIBLE else View.GONE
        }
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
                    if (status == ConnectionStatus.CONNECTED) {
                        refreshDeviceList()
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
