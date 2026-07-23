package com.notifier.client.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.notifier.client.AppPrefs
import com.notifier.client.R
import com.notifier.client.databinding.FragmentDevicesBinding
import com.notifier.client.model.RemoteDevice
import com.notifier.client.network.AdminApi
import com.notifier.client.network.ConnectionStatus
import com.notifier.client.service.NotifierWebSocketService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DevicesFragment : Fragment() {

    private var _binding: FragmentDevicesBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: AppPrefs
    private lateinit var adapter: DeviceListAdapter

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDevicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = AppPrefs(requireContext())

        adapter = DeviceListAdapter(onClick = { device -> openNotificationsFor(device) })
        binding.deviceList.layoutManager = LinearLayoutManager(requireContext())
        binding.deviceList.adapter = adapter

        binding.batteryOptimizationButton.setOnClickListener { requestIgnoreBatteryOptimizations(requireActivity()) }

        requestNotificationPermissionIfNeeded()
        observeConnectionStatus()
        observeNotificationStats()
    }

    override fun onResume() {
        super.onResume()
        if (prefs.isConfigured()) {
            NotifierWebSocketService.start(requireContext())
            refreshDeviceList()
        }
        binding.batteryOptimizationButton.visibility =
            if (isIgnoringBatteryOptimizations(requireContext())) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun openNotificationsFor(device: RemoteDevice) {
        (activity as? TabHost)?.showNotificationsTab(device.id, device.name)
    }

    private fun refreshDeviceList() {
        val serverUrl = prefs.serverUrl ?: return
        val adminToken = prefs.adminToken ?: return

        lifecycleScope.launch {
            val devices = withContext(Dispatchers.IO) {
                runCatching { AdminApi.fetchDevices(serverUrl, adminToken) }.getOrDefault(emptyList())
            }
            if (_binding == null) return@launch
            adapter.submitList(devices)
            binding.emptyLabel.visibility = if (devices.isEmpty()) View.VISIBLE else View.GONE
            binding.statDevicesValue.text = devices.count { it.revokedAt == null }.toString()
        }
    }

    private fun observeNotificationStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                NotifierWebSocketService.receivedNotifications.collect { list ->
                    val stats = computeNotificationStats(list)
                    binding.statNotificationsValue.text = stats.totalCount.toString()
                    binding.statAlarmsValue.text = stats.alarmCount.toString()
                    binding.barChart.submitData(stats.last7Days)
                }
            }
        }
    }

    private fun observeConnectionStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
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
            requireContext(),
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
