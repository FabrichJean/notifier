package com.notifier.client.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.notifier.client.model.AckMessage
import com.notifier.client.model.NotificationData
import com.notifier.client.model.NotificationEnvelope
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

enum class ConnectionStatus { DISCONNECTED, CONNECTING, CONNECTED }

class WsClient(
    private val serverUrl: String,
    private val deviceToken: String,
    private val onNotification: (NotificationData) -> Unit,
    private val onStatusChange: (ConnectionStatus) -> Unit,
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val handler = Handler(Looper.getMainLooper())

    private var webSocket: WebSocket? = null
    private var shouldReconnect = false
    private var reconnectAttempt = 0

    fun connect() {
        shouldReconnect = true
        reconnectAttempt = 0
        openSocket()
    }

    fun disconnect() {
        shouldReconnect = false
        handler.removeCallbacksAndMessages(null)
        webSocket?.close(1000, "client disconnect")
        webSocket = null
        onStatusChange(ConnectionStatus.DISCONNECTED)
    }

    private fun openSocket() {
        onStatusChange(ConnectionStatus.CONNECTING)

        val base = serverUrl.trimEnd('/')
        val url = "$base/ws?token=$deviceToken"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempt = 0
                onStatusChange(ConnectionStatus.CONNECTED)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(webSocket, text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onStatusChange(ConnectionStatus.DISCONNECTED)
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "connexion WebSocket échouée: ${t.message}")
                onStatusChange(ConnectionStatus.DISCONNECTED)
                scheduleReconnect()
            }
        })
    }

    private fun handleMessage(webSocket: WebSocket, text: String) {
        try {
            val envelope = json.decodeFromString(NotificationEnvelope.serializer(), text)
            if (envelope.event == "notification") {
                onNotification(envelope.data)
                val ack = json.encodeToString(AckMessage.serializer(), AckMessage(id = envelope.data.id))
                webSocket.send(ack)
            }
        } catch (e: Exception) {
            Log.w(TAG, "message invalide reçu du serveur: ${e.message}")
        }
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect) return
        reconnectAttempt++
        val backoffSeconds = min(30.0, 2.0.pow(min(reconnectAttempt, 5))).toLong()
        handler.postDelayed({ if (shouldReconnect) openSocket() }, backoffSeconds * 1000)
    }

    companion object {
        private const val TAG = "WsClient"
    }
}
