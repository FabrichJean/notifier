package com.notifier.client.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.notifier.client.AppPrefs
import com.notifier.client.R
import com.notifier.client.model.NotificationData
import com.notifier.client.network.ConnectionStatus
import com.notifier.client.network.WsClient
import com.notifier.client.notification.NotificationDispatcher
import com.notifier.client.ui.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotifierWebSocketService : Service() {

    private var wsClient: WsClient? = null

    override fun onCreate() {
        super.onCreate()
        ensureServiceChannel()
        NotificationDispatcher.ensureChannels(this)
        startForeground(SERVICE_NOTIFICATION_ID, buildServiceNotification(ConnectionStatus.CONNECTING))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = AppPrefs(this)
        val serverUrl = prefs.serverUrl
        val deviceToken = prefs.deviceToken

        if (serverUrl.isNullOrBlank() || deviceToken.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (wsClient == null) {
            wsClient = WsClient(
                serverUrl = serverUrl,
                deviceToken = deviceToken,
                onNotification = { data -> recordAndDispatch(data) },
                onStatusChange = ::handleStatusChange,
            )
            wsClient?.connect()
        }

        return START_STICKY
    }

    private fun recordAndDispatch(data: NotificationData) {
        _receivedNotifications.value = (listOf(data) + _receivedNotifications.value).take(MAX_HISTORY)
        NotificationDispatcher.dispatch(applicationContext, data)
    }

    private fun handleStatusChange(status: ConnectionStatus) {
        _connectionStatus.value = status
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(SERVICE_NOTIFICATION_ID, buildServiceNotification(status))
    }

    override fun onDestroy() {
        wsClient?.disconnect()
        wsClient = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureServiceChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                SERVICE_CHANNEL_ID,
                getString(R.string.notification_channel_service_name),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    private fun buildServiceNotification(status: ConnectionStatus): Notification {
        val statusText = when (status) {
            ConnectionStatus.CONNECTED -> getString(R.string.status_connected)
            ConnectionStatus.CONNECTING -> getString(R.string.status_connecting)
            ConnectionStatus.DISCONNECTED -> getString(R.string.status_disconnected)
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(statusText)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val SERVICE_CHANNEL_ID = "notifier_service"
        private const val SERVICE_NOTIFICATION_ID = 1
        private const val MAX_HISTORY = 50

        private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
        val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

        private val _receivedNotifications = MutableStateFlow<List<NotificationData>>(emptyList())
        val receivedNotifications: StateFlow<List<NotificationData>> = _receivedNotifications.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, NotifierWebSocketService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NotifierWebSocketService::class.java))
        }
    }
}
