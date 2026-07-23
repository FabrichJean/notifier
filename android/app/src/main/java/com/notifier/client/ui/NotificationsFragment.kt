package com.notifier.client.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.notifier.client.databinding.FragmentNotificationsBinding
import com.notifier.client.service.NotifierWebSocketService
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: NotificationListAdapter
    private var filterDeviceId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        filterDeviceId = arguments?.getString(ARG_DEVICE_ID)
        arguments?.getString(ARG_DEVICE_NAME)?.let { name ->
            binding.screenTitle.text = name
        }

        adapter = NotificationListAdapter(showDeviceName = filterDeviceId == null)
        binding.notificationsList.layoutManager = LinearLayoutManager(requireContext())
        binding.notificationsList.adapter = adapter

        observeReceivedNotifications()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeReceivedNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
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
        private const val ARG_DEVICE_ID = "arg_device_id"
        private const val ARG_DEVICE_NAME = "arg_device_name"

        fun newInstance(deviceId: String? = null, deviceName: String? = null): NotificationsFragment {
            return NotificationsFragment().apply {
                arguments = bundleOf(ARG_DEVICE_ID to deviceId, ARG_DEVICE_NAME to deviceName)
            }
        }
    }
}
