package com.notifier.client.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.notifier.client.AppPrefs
import com.notifier.client.databinding.ActivityServerSettingsBinding
import com.notifier.client.service.NotifierWebSocketService

class ServerSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServerSettingsBinding
    private lateinit var prefs: AppPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServerSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsetsAsPadding()
        prefs = AppPrefs(this)

        binding.serverUrlInput.setText(prefs.serverUrl)
        binding.adminTokenInput.setText(prefs.adminToken)

        binding.saveButton.setOnClickListener { onSaveClicked() }
    }

    private fun onSaveClicked() {
        val serverUrl = binding.serverUrlInput.text.toString().trim()
        val adminToken = binding.adminTokenInput.text.toString().trim()

        if (serverUrl.isEmpty() || adminToken.isEmpty()) return

        prefs.serverUrl = serverUrl
        prefs.adminToken = adminToken

        NotifierWebSocketService.start(this)
        finish()
    }
}
