package com.example.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.AlarmEntity
import com.example.MainActivity
import java.util.Calendar

object AlarmScheduler {

    private fun scheduleExactAlarm(context: Context, alarmManager: AlarmManager, triggerMillis: Long, requestCode: Int, alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_TRIGGER_ALARM"
            putExtra("ALARM_ID", alarmId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Also set setAlarmClock so the alarm icon appears in the status bar
        // and OEMs that respect it get a second wakeup signal
        try {
            val showIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.example.ACTION_TRIGGER_ALARM"
                putExtra("ALARM_ID", alarmId)
            }
            val showPendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode + 5000,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Primary wakeup path: setExactAndAllowWhileIdle guarantees CPU wakeup
        // even on aggressive OEM power managers (Transsion/Infinix XOS, MIUI, etc.)
        // that demote setAlarmClock to non-wakeup RTC on non-whitelisted apps.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun scheduleAlarm(context: Context, alarm: AlarmEntity) {
        cancelAlarm(context, alarm)

        if (!alarm.isEnabled) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        var hourOfDay = alarm.hour
        if (alarm.isAm) {
            if (hourOfDay == 12) {
                hourOfDay = 0
            }
        } else {
            if (hourOfDay != 12) {
                hourOfDay += 12
            }
        }

        val calendarDayMap = mapOf(
            0 to Calendar.MONDAY,
            1 to Calendar.TUESDAY,
            2 to Calendar.WEDNESDAY,
            3 to Calendar.THURSDAY,
            4 to Calendar.FRIDAY,
            5 to Calendar.SATURDAY,
            6 to Calendar.SUNDAY
        )

        if (alarm.repeatDays.isNotEmpty()) {
            val now = Calendar.getInstance()
            for (dayIndex in alarm.repeatDays) {
                val dayOfWeek = calendarDayMap[dayIndex] ?: continue
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, alarm.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    set(Calendar.DAY_OF_WEEK, dayOfWeek)
                }
                if (cal.timeInMillis <= now.timeInMillis) {
                    cal.add(Calendar.DAY_OF_YEAR, 7)
                }
                val requestCode = alarm.id.toInt() * 10 + dayIndex
                scheduleExactAlarm(context, alarmManager, cal.timeInMillis, requestCode, alarm.id)
            }
        } else {
            val now = Calendar.getInstance()
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis <= now.timeInMillis) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            scheduleExactAlarm(context, alarmManager, cal.timeInMillis, alarm.id.toInt(), alarm.id)
        }
    }

    fun cancelAlarm(context: Context, alarm: AlarmEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_TRIGGER_ALARM"
            putExtra("ALARM_ID", alarm.id)
        }

        // Cancel once-off schedule
        val oncePendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(oncePendingIntent)
        oncePendingIntent.cancel()

        // Cancel once-off show intent (created by setAlarmClock)
        val onceShowPendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt() + 5000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(onceShowPendingIntent)
        onceShowPendingIntent.cancel()

        // Cancel all 7 repeat day schedules + their show intents
        for (dayIndex in 0..6) {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarm.id.toInt() * 10 + dayIndex,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()

            val showPendingIntent = PendingIntent.getBroadcast(
                context,
                alarm.id.toInt() * 10 + dayIndex + 5000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(showPendingIntent)
            showPendingIntent.cancel()
        }
    }

    fun getNextTriggerMillis(alarm: AlarmEntity): Long {
        val now = Calendar.getInstance()
        
        var hourOfDay = alarm.hour
        if (alarm.isAm) {
            if (hourOfDay == 12) {
                hourOfDay = 0
            }
        } else {
            if (hourOfDay != 12) {
                hourOfDay += 12
            }
        }

        val repeatDays = alarm.repeatDays
        val isRepeating = repeatDays.isNotEmpty()

        if (!isRepeating) {
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        } else {
            val calendarDayMap = mapOf(
                0 to Calendar.MONDAY,
                1 to Calendar.TUESDAY,
                2 to Calendar.WEDNESDAY,
                3 to Calendar.THURSDAY,
                4 to Calendar.FRIDAY,
                5 to Calendar.SATURDAY,
                6 to Calendar.SUNDAY
            )
            var minTime = Long.MAX_VALUE
            for (dayIndex in repeatDays) {
                val dayOfWeek = calendarDayMap[dayIndex] ?: continue
                val target = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, alarm.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    set(Calendar.DAY_OF_WEEK, dayOfWeek)
                }

                if (target.before(now)) {
                    target.add(Calendar.DAY_OF_YEAR, 7)
                }

                if (target.timeInMillis < minTime) {
                    minTime = target.timeInMillis
                }
            }

            if (minTime == Long.MAX_VALUE) {
                val target = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, alarm.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (target.before(now)) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
                return target.timeInMillis
            }

            return minTime
        }
    }
}
