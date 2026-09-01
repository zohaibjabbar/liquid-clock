package com.example.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimerService : Service() {

    private var ringtone: Ringtone? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var tickerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    companion object {
        const val ACTION_START_TIMER = "com.example.ACTION_START_TIMER"
        const val ACTION_PAUSE_TIMER = "com.example.ACTION_PAUSE_TIMER"
        const val ACTION_RING = "com.example.ACTION_RING"
        const val ACTION_DISMISS = "com.example.ACTION_DISMISS"
        
        const val RUNNING_NOTIFICATION_ID = 2027
        const val RINGING_NOTIFICATION_ID = 2028
        const val RUNNING_CHANNEL_ID = "timer_running_channel"
        const val RINGING_CHANNEL_ID = "timer_ringing_channel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START_TIMER -> {
                val endTime = intent.getLongExtra("EXTRA_END_TIME", 0L)
                val totalMs = intent.getLongExtra("EXTRA_TOTAL_MS", 0L)
                startCountdownTicker(endTime, totalMs)
            }
            ACTION_PAUSE_TIMER -> {
                try {
                    stopCountdownTicker()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    stopAlarmSound()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    stopVibrating()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(RUNNING_NOTIFICATION_ID)
                    notificationManager.cancel(RINGING_NOTIFICATION_ID)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    stopForeground(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    stopSelf()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            ACTION_RING -> {
                stopCountdownTicker()
                startRingingState()
            }
            ACTION_DISMISS -> {
                dismissRinging()
            }
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // 1. Silent, Low-priority channel for running countdown
            val runningChannel = NotificationChannel(
                RUNNING_CHANNEL_ID,
                "Timer Running Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active countdown timer"
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(runningChannel)

            // 2. High-priority channel for ringing alert when finished
            val ringingChannel = NotificationChannel(
                RINGING_CHANNEL_ID,
                "Timer Finished Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when countdown is complete"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(ringingChannel)
        }
    }

    private fun startCountdownTicker(endTime: Long, totalMs: Long) {
        stopCountdownTicker()

        // Show initial running notification and start foreground
        val notification = createRunningNotification(endTime - System.currentTimeMillis())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                RUNNING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(RUNNING_NOTIFICATION_ID, notification)
        }

        tickerJob = serviceScope.launch {
            while (true) {
                val remaining = endTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    // Update SharedPreferences
                    saveTimerStateToPrefs(isRunning = false, endTime = 0L, totalMs = totalMs, remainingMs = 0L)
                    // Transition to Ringing State
                    startRingingState()
                    break
                } else {
                    // Update running notification with live remaining time
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(RUNNING_NOTIFICATION_ID, createRunningNotification(remaining))
                }
                delay(1000)
            }
        }
    }

    private fun stopCountdownTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun createRunningNotification(remainingMs: Long): Notification {
        val totalSecs = remainingMs / 1000
        val hrs = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        val remainingStr = if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, RUNNING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Timer Active")
            .setContentText("Time remaining: $remainingStr")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startRingingState() {
        stopCountdownTicker()
        
        // Cancel the running notification if any
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(RUNNING_NOTIFICATION_ID)

        playAlarmSound()
        startVibrating()

        // Create the Ringing Notification with Full Screen Intent pointing to TimerRingingActivity
        val fullScreenIntent = Intent(this, TimerRingingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            1001,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Add a direct Dismiss action on the notification banner
        val dismissIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            1002,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, RINGING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Timer Finished")
            .setContentText("Your countdown timer has completed!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .build()

        // Transition our foreground service status to the ringing notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                RINGING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(RINGING_NOTIFICATION_ID, notification)
        }

        // Immediately start the TimerRingingActivity so it shows on top of everything
        try {
            startActivity(fullScreenIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playAlarmSound() {
        try {
            stopAlarmSound()
            val prefs = getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
            val customUri = prefs.getString("timer_custom_sound_uri", null)
            
            if (!customUri.isNullOrEmpty()) {
                val mp = MediaPlayer().apply {
                    setDataSource(this@TimerService, Uri.parse(customUri))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        setAudioStreamType(AudioManager.STREAM_ALARM)
                    }
                    isLooping = true
                    prepare()
                    start()
                }
                mediaPlayer = mp
                return
            }
            
            val soundName = prefs.getString("default_timer_sound_name", "Crystal") ?: "Crystal"
            
            val customKeys = listOf("Dewdrop Serenade", "Bubble Resonance", "Ocean Breeze", "Liquid Echo", "Rainfall Melody")
            if (soundName in customKeys) {
                LiquidSoundSynth.startPlaying(soundName)
                return
            }

            val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val rt = RingtoneManager.getRingtone(this, uri)
            if (rt != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    rt.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    rt.streamType = AudioManager.STREAM_ALARM
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    rt.isLooping = true
                }
                ringtone = rt
                rt.play()
            } else {
                val mp = MediaPlayer().apply {
                    setDataSource(this@TimerService, uri)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        setAudioStreamType(AudioManager.STREAM_ALARM)
                    }
                    isLooping = true
                    prepare()
                    start()
                }
                mediaPlayer = mp
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer.create(this, uri)?.apply {
                    isLooping = true
                    start()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun stopAlarmSound() {
        try {
            LiquidSoundSynth.stopPlaying()
            ringtone?.let { rt ->
                try {
                    if (rt.isPlaying) {
                        rt.stop()
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
            ringtone = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            mediaPlayer?.let { mp ->
                try {
                    mp.stop()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                try {
                    mp.release()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibrating() {
        try {
            val prefs = getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
            val globalHapticsEnabled = prefs.getBoolean("haptic_feedback_enabled", true)
            val timerHapticsEnabled = prefs.getBoolean("timer_haptics_enabled", true)
            val legacyEnabled = prefs.getBoolean("timer_vibrate", true)
            if (!globalHapticsEnabled || !timerHapticsEnabled || !legacyEnabled) return

            val intensity = prefs.getInt("vibration_intensity", 70)
            if (intensity <= 0) return

            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            val pattern = longArrayOf(0, 500, 500) // Vibrate 500ms, pause 500ms
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitude = ((intensity / 100.0) * 255).toInt().coerceIn(1, 255)
                val amplitudes = intArrayOf(0, amplitude, 0)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopVibrating() {
        try {
            vibrator?.let { vib ->
                try {
                    vib.cancel()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
            vibrator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dismissRinging() {
        try {
            // Stop the ringtone first
            try {
                ringtone?.let { rt ->
                    if (rt.isPlaying) {
                        rt.stop()
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            ringtone = null

            try {
                mediaPlayer?.let { mp ->
                    mp.stop()
                    mp.release()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            mediaPlayer = null

            // Then cancel vibration
            try {
                vibrator?.let { vib ->
                    vib.cancel()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            vibrator = null

            // Then call stopForeground passing true to remove the notification
            try {
                stopForeground(true)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

            // Cancel notification via NotificationManager just in case
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(RINGING_NOTIFICATION_ID)
                notificationManager.cancel(RUNNING_NOTIFICATION_ID)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

            // Clear timer state in preferences
            try {
                saveTimerStateToPrefs(isRunning = false, endTime = 0L, totalMs = 0L, remainingMs = 0L)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

            // Then call stopSelf on the service
            try {
                stopSelf()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveTimerStateToPrefs(isRunning: Boolean, endTime: Long, totalMs: Long, remainingMs: Long) {
        val prefs = getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("timer_is_running", isRunning)
            putLong("timer_end_time", endTime)
            putLong("timer_total_ms", totalMs)
            putLong("timer_remaining_ms", remainingMs)
            apply()
        }
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            stopCountdownTicker()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            stopAlarmSound()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            stopVibrating()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
