package com.notifier.client.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.notifier.client.AppPrefs
import com.notifier.client.databinding.FragmentServerSettingsBinding
import com.notifier.client.service.NotifierWebSocketService

class ServerSettingsFragment : Fragment() {

    private var _binding: FragmentServerSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: AppPrefs

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentServerSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = AppPrefs(requireContext())

        binding.serverUrlInput.setText(prefs.serverUrl)
        binding.adminTokenInput.setText(prefs.adminToken)

        binding.saveButton.setOnClickListener { onSaveClicked() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onSaveClicked() {
        val serverUrl = binding.serverUrlInput.text.toString().trim()
        val adminToken = binding.adminTokenInput.text.toString().trim()

        if (serverUrl.isEmpty() || adminToken.isEmpty()) return

        prefs.serverUrl = serverUrl
        prefs.adminToken = adminToken

        NotifierWebSocketService.start(requireContext())
    }
}
