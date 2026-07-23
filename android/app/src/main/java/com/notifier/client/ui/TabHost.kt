package com.notifier.client.ui

/** Implemented by the hosting activity so fragments can request a tab switch without a direct reference to it. */
interface TabHost {
    fun showNotificationsTab(deviceId: String? = null, deviceName: String? = null)
}
