package com.example

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.example.ui.TimerService
import com.example.ui.SoundHapticHelper

class ClockApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Preload sound players off the main thread to avoid blocking startup
        val appContext = this
        Thread {
            SoundHapticHelper.preload(appContext)
        }.start()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // 1. Stop Foreground Service cleanly
                val intent = Intent(this, TimerService::class.java)
                try {
                    stopService(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 2. Clear all timer state from SharedPreferences
                try {
                    val prefs = getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
                    prefs.edit().clear().apply()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 3. Cancel all timer notifications
                try {
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(TimerService.RUNNING_NOTIFICATION_ID)
                    notificationManager.cancel(TimerService.RINGING_NOTIFICATION_ID)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Let the app crash normally
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
