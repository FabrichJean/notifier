package com.notifier.client.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.notifier.client.R
import com.notifier.client.model.NotificationData
import com.notifier.client.notification.NotificationDispatcher
import com.notifier.client.ui.applySystemBarInsetsAsPadding

class AlarmActivity : AppCompatActivity() {

    private lateinit var notificationId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowOnLockScreen()

        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val body = intent.getStringExtra(EXTRA_BODY) ?: ""
        notificationId = intent.getStringExtra(EXTRA_ID) ?: ""
        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, -1)

        val root = buildLayout(title, body, snoozeMinutes)
        setContentView(root)
        root.applySystemBarInsetsAsPadding()
    }

    private fun buildLayout(title: String, body: String, snoozeMinutes: Int): LinearLayout {
        val padding = (24 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding * 3, padding, padding)
        }

        root.addView(TextView(this).apply {
            text = title
            textSize = 26f
            setTextColor(getColor(R.color.alarm_text))
        })

        root.addView(TextView(this).apply {
            text = body
            textSize = 18f
            setTextColor(getColor(R.color.alarm_text))
            setPadding(0, padding / 2, 0, padding * 2)
        })

        root.addView(Button(this).apply {
            text = getString(R.string.alarm_dismiss)
            setOnClickListener { dismiss() }
        })

        if (snoozeMinutes > 0) {
            root.addView(Button(this).apply {
                text = getString(R.string.alarm_snooze)
                setOnClickListener { snooze(snoozeMinutes) }
            })
        }

        return root
    }

    private fun dismiss() {
        AlarmSoundPlayer.stop()
        NotificationDispatcher.clearAlarmNotification(this, notificationId)
        finish()
    }

    private fun snooze(minutes: Int) {
        AlarmSoundPlayer.stop()
        AlarmActionReceiver.scheduleSnooze(this, notificationDataFromIntent(), minutes)
        NotificationDispatcher.clearAlarmNotification(this, notificationId)
        finish()
    }

    private fun notificationDataFromIntent(): NotificationData {
        return NotificationData(
            id = notificationId,
            title = intent.getStringExtra(EXTRA_TITLE) ?: "",
            body = intent.getStringExtra(EXTRA_BODY) ?: "",
            type = "alarm",
            alarm = com.notifier.client.model.AlarmConfig(
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

    private fun setShowOnLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_SOUND = "extra_sound"
        const val EXTRA_LOOP = "extra_loop"
        const val EXTRA_VIBRATE = "extra_vibrate"
        const val EXTRA_FULLSCREEN = "extra_fullscreen"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_SOURCE = "extra_source"
        const val EXTRA_CREATED_AT = "extra_created_at"

        fun buildIntent(context: Context, data: NotificationData): Intent {
            return Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
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
    }
}
