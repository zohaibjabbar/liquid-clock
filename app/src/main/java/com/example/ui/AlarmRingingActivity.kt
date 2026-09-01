package com.example.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.data.AlarmEntity
import com.example.data.ClockDatabase
import com.example.ui.screens.ActiveAlarmRingingOverlay
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lock screen flags so it wakes the screen and shows on top of keyguard/lock screen
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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Start repeating vibration if setting is enabled
        val prefs = getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
        val shouldVibrate = prefs.getBoolean("alarm_vibrate", true)
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (shouldVibrate) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 800, 400, 800, 400, 800), 0
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 800, 400, 800, 400, 800), 0)
            }
        }

        val alarmId = intent.getLongExtra("ALARM_ID", -1L)
        var alarmState by mutableStateOf<AlarmEntity?>(null)

        lifecycleScope.launch {
            val db = ClockDatabase.getDatabase(this@AlarmRingingActivity)
            val alarm = db.alarmDao().getAlarmById(alarmId)
            alarmState = alarm
        }

        setContent {
            MyApplicationTheme {
                val currentAlarm = alarmState
                if (currentAlarm != null) {
                    ActiveAlarmRingingOverlay(
                        alarm = currentAlarm,
                        onStop = {
                            val stopVibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            stopVibrator.cancel()

                            val stopIntent = Intent(this, AlarmService::class.java).apply {
                                action = AlarmService.ACTION_STOP
                            }
                            startService(stopIntent)
                            finish()
                        },
                        onSnooze = {
                            val stopVibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            stopVibrator.cancel()

                            val snoozeIntent = Intent(this, AlarmService::class.java).apply {
                                action = AlarmService.ACTION_SNOOZE
                                putExtra("ALARM_ID", alarmId)
                            }
                            startService(snoozeIntent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}
