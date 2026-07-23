package com.notifier.client.network

import com.notifier.client.model.RemoteDevice
import com.notifier.client.model.RemoteDevicesResponse
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/** Fetches the read-only device list from the backend's admin API using the admin token. */
object AdminApi {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    fun fetchDevices(serverUrl: String, adminToken: String): List<RemoteDevice> {
        val request = Request.Builder()
            .url("${toHttpUrl(serverUrl)}/admin/api/devices")
            .header("X-Admin-Token", adminToken)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            return json.decodeFromString(RemoteDevicesResponse.serializer(), body).devices
        }
    }

    private fun toHttpUrl(serverUrl: String): String {
        return serverUrl
            .replaceFirst("wss://", "https://")
            .replaceFirst("ws://", "http://")
            .trimEnd('/')
    }
}
