package com.notifier.client.model

import kotlinx.serialization.Serializable

@Serializable
data class RemoteDevice(
    val id: String,
    val name: String,
    val createdAt: String,
    val revokedAt: String?,
)

@Serializable
data class RemoteDevicesResponse(
    val devices: List<RemoteDevice>,
)
