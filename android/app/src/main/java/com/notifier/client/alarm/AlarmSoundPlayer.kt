package com.notifier.client.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object AlarmSoundPlayer {
    private const val TAG = "AlarmSoundPlayer"

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    fun play(context: Context, sound: String, loop: Boolean, vibrate: Boolean) {
        stop()

        try {
            val uri = resolveSoundUri(context, sound)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                isLooping = loop
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "impossible de jouer le son d'alarme: ${e.message}")
        }

        if (vibrate) {
            val vib = getVibrator(context)
            vibrator = vib
            val pattern = longArrayOf(0, 800, 400)
            val repeatIndex = if (loop) 0 else -1
            vib.vibrate(VibrationEffect.createWaveform(pattern, repeatIndex))
        }
    }

    fun stop() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: Exception) {
            }
            it.release()
        }
        mediaPlayer = null

        vibrator?.cancel()
        vibrator = null
    }

    private fun resolveSoundUri(context: Context, sound: String) = when (sound) {
        "gentle" -> RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        else -> RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    }

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
}
