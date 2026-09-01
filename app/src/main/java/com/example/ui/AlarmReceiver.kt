package com.example.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.ClockDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.BOOT_COMPLETED") {
            val db = ClockDatabase.getDatabase(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val list = db.alarmDao().getAllAlarms().first()
                    list.forEach { alarm ->
                        if (alarm.isEnabled) {
                            AlarmScheduler.scheduleAlarm(context, alarm)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return
        }

        if (action == "com.example.ACTION_TRIGGER_ALARM") {
            // Take a wake lock of maximum 10s to guarantee CPU is active
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val wakeLock = powerManager.newWakeLock(
                android.os.PowerManager.PARTIAL_WAKE_LOCK,
                "LiquidClock:AlarmWakeLock"
            )
            wakeLock.acquire(60 * 1000L)

            val alarmId = intent.getLongExtra("ALARM_ID", -1L)
            if (alarmId != -1L) {
                try {
                    val serviceIntent = Intent(context, AlarmService::class.java).apply {
                        this.action = AlarmService.ACTION_TRIGGER_ALARM
                        putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        fun showRingingNotification(context: Context, alarmId: Long) {
            try {
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    this.action = AlarmService.ACTION_TRIGGER_ALARM
                    putExtra(AlarmService.EXTRA_ALARM_ID, alarmId)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
