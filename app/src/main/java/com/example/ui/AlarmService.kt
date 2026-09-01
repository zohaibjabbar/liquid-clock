package com.example.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.data.ClockDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.util.Calendar

class AlarmService : Service() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private lateinit var audioManager: AudioManager
    private var gradualVolumeJob: Job? = null

    companion object {
        const val ACTION_TRIGGER_ALARM = "com.example.ACTION_TRIGGER_ALARM"
        const val ACTION_STOP = "com.example.ACTION_STOP"
        const val ACTION_SNOOZE = "com.example.ACTION_SNOOZE"
        const val EXTRA_ALARM_ID = "ALARM_ID"
        const val NOTIFICATION_ID = 2026
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1L) ?: -1L

        when (action) {
            ACTION_TRIGGER_ALARM -> {
                if (alarmId != -1L) {
                    startRinging(alarmId)
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopRinging()
            }
            ACTION_SNOOZE -> {
                if (alarmId != -1L) {
                    snoozeRinging(alarmId)
                } else {
                    stopRinging()
                }
            }
            else -> stopSelf()
        }

        return START_STICKY
    }

    private fun startRinging(alarmId: Long) {
        val db = ClockDatabase.getDatabase(this)
        CoroutineScope(Dispatchers.IO).launch {
            val alarm = db.alarmDao().getAlarmById(alarmId)
            val labelText = alarm?.label ?: "Alarm Ringing"
            val soundName = alarm?.sound ?: "Dewdrop Serenade"

            playAlarmSound(soundName)
            handleGradualVolume()
            startVibrating()

            // Disable single alarm or reschedule repeating
            if (alarm != null) {
                if (alarm.repeatDays.isEmpty()) {
                    db.alarmDao().updateAlarm(alarm.copy(isEnabled = false))
                } else {
                    AlarmScheduler.scheduleAlarm(this@AlarmService, alarm)
                }
            }

            // Post Notification with Fullscreen intent in Main thread
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            handler.post {
                showForegroundNotification(alarmId, labelText)
                
                // Immediately start activity so it shows on top of home screen or background apps
                try {
                    val fullScreenIntent = Intent(this@AlarmService, AlarmRingingActivity::class.java).apply {
                        putExtra(EXTRA_ALARM_ID, alarmId)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    startActivity(fullScreenIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showForegroundNotification(alarmId: Long, labelText: String) {
        val channelId = "alarm_service_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alarm Ringing Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows overlay when an alarm is triggering"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Full Screen Intent pointing to AlarmRingingActivity
        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Actions for stop and snooze directly on notification banner
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            alarmId.toInt() + 100,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            alarmId.toInt() + 200,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(labelText)
            .setContentText("Tap to open alarm overlay")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .addAction(android.R.drawable.ic_menu_today, "Snooze (9m)", snoozePendingIntent)
            .build()

        // Start Foreground Service with correct foreground service type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun playAlarmSound(soundName: String = "Dewdrop Serenade") {
        try {
            stopAlarmSound()
            
            var resolvedSoundName = soundName
            val prefs = getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
            val savedSoundName = prefs.getString("default_alarm_sound_name", "Starlight") ?: "Starlight"
            val savedUriStr = prefs.getString("default_alarm_sound_uri", null)

            if (resolvedSoundName == "Radial" || resolvedSoundName == "Starlight") {
                resolvedSoundName = savedSoundName
            }

            val customKeys = listOf("Dewdrop Serenade", "Bubble Resonance", "Ocean Breeze", "Liquid Echo", "Rainfall Melody")
            if (resolvedSoundName in customKeys) {
                LiquidSoundSynth.startPlaying(resolvedSoundName)
            } else {
                val uri: Uri = if (soundName.startsWith("content://") || soundName.startsWith("file://") || soundName.contains("/")) {
                    Uri.parse(soundName)
                } else if (savedUriStr != null && (soundName == "Radial" || soundName == "Starlight" || soundName == savedSoundName)) {
                    Uri.parse(savedUriStr)
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                }
                ringtone = RingtoneManager.getRingtone(this, uri)
                ringtone?.play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarmSound() {
        try {
            LiquidSoundSynth.stopPlaying()
            ringtone?.stop()
            ringtone = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleGradualVolume() {
        try {
            val prefs = getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
            val isGradual = prefs.getBoolean("gradual_increase_volume", true)
            val targetPercent = prefs.getInt("alarm_volume_percent", 70)
            
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val targetVolume = (targetPercent * maxVol) / 100

            if (isGradual) {
                val startVolume = maxOf(1, targetVolume / 10)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, startVolume, 0)
                
                gradualVolumeJob = CoroutineScope(Dispatchers.Main).launch {
                    for (step in 1..10) {
                        delay(3000)
                        val currentStepVol = startVolume + ((targetVolume - startVolume) * step / 10)
                        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, currentStepVol, 0)
                    }
                }
            } else {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibrating() {
        try {
            val prefs = getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
            val shouldVibrate = prefs.getBoolean("alarm_vibrate", true)
            if (shouldVibrate) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 800, 400, 800, 400, 800), 0
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 800, 400, 800, 400, 800), 0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopVibrating() {
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopRinging() {
        gradualVolumeJob?.cancel()
        stopAlarmSound()
        stopVibrating()
        stopSelf()
    }

    private fun snoozeRinging(alarmId: Long) {
        val prefs = getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
        val snoozeMin = prefs.getInt("snooze_duration_minutes", 9)
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, snoozeMin)
        }
        var hour12 = calendar.get(Calendar.HOUR)
        if (hour12 == 0) hour12 = 12
        val isAm = calendar.get(Calendar.AM_PM) == Calendar.AM

        val db = ClockDatabase.getDatabase(this)
        CoroutineScope(Dispatchers.IO).launch {
            val alarm = db.alarmDao().getAlarmById(alarmId)
            if (alarm != null) {
                val snoozedAlarm = alarm.copy(
                    hour = hour12,
                    minute = calendar.get(Calendar.MINUTE),
                    isAm = isAm,
                    repeatDays = emptySet(),
                    label = if (alarm.label.contains("Snoozed")) alarm.label else "${alarm.label} (Snoozed)",
                    isEnabled = true
                )
                AlarmScheduler.scheduleAlarm(this@AlarmService, snoozedAlarm)
            }
        }

        stopRinging()
    }

    override fun onDestroy() {
        super.onDestroy()
        gradualVolumeJob?.cancel()
        stopAlarmSound()
        stopVibrating()
    }
}
