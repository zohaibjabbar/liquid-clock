package com.example

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

object HapticManager {

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            val v = context.applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            android.util.Log.d("HAPTIC_DEBUG", "Vibrator obtained: $v, hasVibrator: ${v?.hasVibrator()}")
            v
        } catch (e: Exception) {
            null
        }
    }

    fun light(context: Context) {
        try {
            val v = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(60, 180))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(60)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun medium(context: Context) {
        try {
            val v = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(100, 220))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(100)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun heavy(context: Context) {
        try {
            val v = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(150, 255))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(150)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun doubleClick(context: Context) {
        try {
            val v = getVibrator(context) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 80, 80, 80), -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(longArrayOf(0, 80, 80, 80), -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
