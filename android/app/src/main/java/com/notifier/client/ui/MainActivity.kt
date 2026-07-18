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
import com.notifier.client.AppPrefs
import com.notifier.client.R
import com.notifier.client.databinding.ActivityMainBinding
import com.notifier.client.network.ConnectionStatus
import com.notifier.client.service.NotifierWebSocketService
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: AppPrefs

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsetsAsPadding()
        prefs = AppPrefs(this)

        binding.serverUrlInput.setText(prefs.serverUrl)
        binding.deviceTokenInput.setText(prefs.deviceToken)

        binding.connectButton.setOnClickListener { onConnectClicked() }
        binding.disconnectButton.setOnClickListener { onDisconnectClicked() }
        binding.batteryOptimizationButton.setOnClickListener { requestIgnoreBatteryOptimizations(this) }

        requestNotificationPermissionIfNeeded()
        observeConnectionStatus()
    }

    override fun onResume() {
        super.onResume()
        binding.batteryOptimizationButton.visibility =
            if (isIgnoringBatteryOptimizations(this)) View.GONE else View.VISIBLE
    }

    private fun onConnectClicked() {
        val serverUrl = binding.serverUrlInput.text.toString().trim()
        val deviceToken = binding.deviceTokenInput.text.toString().trim()

        if (serverUrl.isEmpty() || deviceToken.isEmpty()) return

        prefs.serverUrl = serverUrl
        prefs.deviceToken = deviceToken
        NotifierWebSocketService.start(this)
    }

    private fun onDisconnectClicked() {
        NotifierWebSocketService.stop(this)
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
                        startActivity(Intent(this@MainActivity, StatusActivity::class.java))
                        finish()
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
