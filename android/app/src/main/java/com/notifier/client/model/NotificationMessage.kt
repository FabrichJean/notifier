package com.notifier.client.model

import kotlinx.serialization.Serializable

@Serializable
data class AlarmConfig(
    val fullScreen: Boolean = true,
    val sound: String = "default",
    val loop: Boolean = true,
    val vibrate: Boolean = true,
    val snoozeMinutes: Int? = null,
)

@Serializable
data class NotificationData(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val alarm: AlarmConfig? = null,
    val source: String,
    val targetDeviceId: String? = null,
    val targetDeviceName: String? = null,
    val createdAt: String,
)

@Serializable
data class NotificationEnvelope(
    val event: String,
    val data: NotificationData,
)

@Serializable
data class AckMessage(
    val event: String = "ack",
    val id: String,
)
