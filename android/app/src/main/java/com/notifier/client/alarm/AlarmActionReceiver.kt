package com.notifier.client.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notifier.client.model.AlarmConfig
import com.notifier.client.model.NotificationData
import com.notifier.client.notification.NotificationDispatcher
import java.util.concurrent.TimeUnit

class AlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return

        when (intent.action) {
            ACTION_DISMISS -> {
                AlarmSoundPlayer.stop()
                NotificationDispatcher.clearAlarmNotification(context, id)
            }

            ACTION_SNOOZE -> {
                val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 5)
                AlarmSoundPlayer.stop()
                NotificationDispatcher.clearAlarmNotification(context, id)
                scheduleSnooze(context, notificationDataFromIntent(intent), minutes)
            }

            ACTION_SNOOZE_FIRE -> {
                NotificationDispatcher.dispatch(context, notificationDataFromIntent(intent))
            }
        }
    }

    private fun notificationDataFromIntent(intent: Intent): NotificationData {
        return NotificationData(
            id = intent.getStringExtra(EXTRA_ID) ?: "",
            title = intent.getStringExtra(EXTRA_TITLE) ?: "",
            body = intent.getStringExtra(EXTRA_BODY) ?: "",
            type = "alarm",
            alarm = AlarmConfig(
                fullScreen = intent.getBooleanExtra(EXTRA_FULLSCREEN, true),
                sound = intent.getStringExtra(EXTRA_SOUND) ?: "default",
                loop = intent.getBooleanExtra(EXTRA_LOOP, true),
                vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true),
                snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, -1).takeIf { it > 0 },
            ),
            source = intent.getStringExtra(EXTRA_SOURCE) ?: "",
            createdAt = intent.getStringExtra(EXTRA_CREATED_AT) ?: "",
        )
    }

    companion object {
        const val ACTION_DISMISS = "com.notifier.client.action.DISMISS"
        const val ACTION_SNOOZE = "com.notifier.client.action.SNOOZE"
        const val ACTION_SNOOZE_FIRE = "com.notifier.client.action.SNOOZE_FIRE"

        private const val EXTRA_ID = "extra_id"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_BODY = "extra_body"
        private const val EXTRA_SOUND = "extra_sound"
        private const val EXTRA_LOOP = "extra_loop"
        private const val EXTRA_VIBRATE = "extra_vibrate"
        private const val EXTRA_FULLSCREEN = "extra_fullscreen"
        private const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        private const val EXTRA_SOURCE = "extra_source"
        private const val EXTRA_CREATED_AT = "extra_created_at"

        private fun baseIntent(context: Context, data: NotificationData, action: String): Intent {
            return Intent(context, AlarmActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_ID, data.id)
                putExtra(EXTRA_TITLE, data.title)
                putExtra(EXTRA_BODY, data.body)
                putExtra(EXTRA_SOUND, data.alarm?.sound ?: "default")
                putExtra(EXTRA_LOOP, data.alarm?.loop ?: true)
                putExtra(EXTRA_VIBRATE, data.alarm?.vibrate ?: true)
                putExtra(EXTRA_FULLSCREEN, data.alarm?.fullScreen ?: true)
                putExtra(EXTRA_SNOOZE_MINUTES, data.alarm?.snoozeMinutes ?: -1)
                putExtra(EXTRA_SOURCE, data.source)
                putExtra(EXTRA_CREATED_AT, data.createdAt)
            }
        }

        fun buildDismissIntent(context: Context, id: String): Intent {
            return Intent(context, AlarmActionReceiver::class.java).apply {
                action = ACTION_DISMISS
                putExtra(EXTRA_ID, id)
            }
        }

        fun buildSnoozeIntent(context: Context, data: NotificationData, minutes: Int): Intent {
            return baseIntent(context, data, ACTION_SNOOZE).apply {
                putExtra(EXTRA_SNOOZE_MINUTES, minutes)
            }
        }

        fun scheduleSnooze(context: Context, data: NotificationData, minutes: Int) {
            val fireIntent = baseIntent(context, data, ACTION_SNOOZE_FIRE)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                data.id.hashCode() + 2,
                fireIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes.toLong())
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }
}
