package com.notifier.client.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.notifier.client.R
import com.notifier.client.alarm.AlarmActionReceiver
import com.notifier.client.alarm.AlarmActivity
import com.notifier.client.alarm.AlarmSoundPlayer
import com.notifier.client.model.NotificationData

object NotificationDispatcher {
    const val CHANNEL_DEFAULT = "notifier_default"
    const val CHANNEL_ALARM = "notifier_alarm"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DEFAULT,
                context.getString(R.string.notification_channel_default_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALARM,
                context.getString(R.string.notification_channel_alarm_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setBypassDnd(true)
                setSound(null, null)
            }
        )
    }

    fun dispatch(context: Context, data: NotificationData) {
        ensureChannels(context)
        if (data.type == "alarm") {
            dispatchAlarm(context, data)
        } else {
            dispatchSimple(context, data)
        }
    }

    private fun dispatchSimple(context: Context, data: NotificationData) {
        val notification = NotificationCompat.Builder(context, CHANNEL_DEFAULT)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(data.title)
            .setContentText(data.body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(data.id.hashCode(), notification)
    }

    private fun dispatchAlarm(context: Context, data: NotificationData) {
        val alarm = data.alarm
        val sound = alarm?.sound ?: "default"
        val loop = alarm?.loop ?: true
        val vibrate = alarm?.vibrate ?: true
        val fullScreen = alarm?.fullScreen ?: true
        val snoozeMinutes = alarm?.snoozeMinutes

        AlarmSoundPlayer.play(context, sound, loop, vibrate)

        val activityIntent = AlarmActivity.buildIntent(context, data)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            data.id.hashCode(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            data.id.hashCode(),
            AlarmActionReceiver.buildDismissIntent(context, data.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setContentTitle(data.title)
            .setContentText(data.body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .addAction(0, context.getString(R.string.alarm_dismiss), dismissPendingIntent)

        if (snoozeMinutes != null && snoozeMinutes > 0) {
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                data.id.hashCode() + 1,
                AlarmActionReceiver.buildSnoozeIntent(context, data, snoozeMinutes),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, context.getString(R.string.alarm_snooze), snoozePendingIntent)
        }

        if (fullScreen) {
            builder.setFullScreenIntent(contentPendingIntent, true)
        }

        NotificationManagerCompat.from(context).notify(data.id.hashCode(), builder.build())
    }

    fun clearAlarmNotification(context: Context, id: String) {
        NotificationManagerCompat.from(context).cancel(id.hashCode())
    }
}
