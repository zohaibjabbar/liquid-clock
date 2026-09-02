package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.ui.zIndex
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.HapticManager
import com.example.ui.ClockViewModel
import com.example.ui.theme.*

@Composable
fun SettingsScreen(viewModel: ClockViewModel) {
    val context = LocalContext.current
    val prefs = remember { 
        context.getSharedPreferences("clock_settings", Context.MODE_PRIVATE) 
    }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    // Read all prefs once synchronously — fast, no network
    var alarmVolume by remember { mutableStateOf(prefs.getInt("alarm_volume_percent", 70)) }
    var snoozeDuration by remember { mutableStateOf(prefs.getInt("snooze_duration_minutes", 9)) }
    var gradualVolume by remember { mutableStateOf(prefs.getBoolean("gradually_increase_volume", true)) }
    var alarmVibrate by remember { mutableStateOf(prefs.getBoolean("alarm_vibrate", true)) }
    var timerVolume by remember { mutableStateOf(prefs.getInt("timer_volume_percent", 85)) }
    var keepScreenOn by remember { mutableStateOf(prefs.getBoolean("timer_keep_screen_on", true)) }
    var timerVibrate by remember { mutableStateOf(prefs.getBoolean("timer_vibrate", true)) }
    var vibrationIntensity by remember { mutableStateOf(prefs.getInt("vibration_intensity", 70)) }
    var hapticFeedback by remember { mutableStateOf(prefs.getBoolean("haptic_feedback_enabled", true)) }
    var timerHaptics by remember { mutableStateOf(prefs.getBoolean("timer_haptics_enabled", true)) }
    var alarmHaptics by remember { mutableStateOf(prefs.getBoolean("alarm_haptics_enabled", true)) }
    var alarmSoundName by remember { mutableStateOf(prefs.getString("default_alarm_sound_name", "Starlight") ?: "Starlight") }
    var timerSoundName by remember { mutableStateOf(prefs.getString("default_timer_sound_name", "Crystal") ?: "Crystal") }

    var showAlarmSoundPicker by remember { mutableStateOf(false) }
    var showSnoozeDurationPicker by remember { mutableStateOf(false) }
    var showTimerSoundPicker by remember { mutableStateOf(false) }

    val versionName = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    BackHandler { viewModel.showSettings(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { 
                HapticManager.light(context.applicationContext)
                viewModel.showSettings(false) 
            }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // ALARMS section
        SettingsSectionHeader("ALARMS")
        SettingsCard {
            SettingsRowChevron("Default Alarm Sound", alarmSoundName) { showAlarmSoundPicker = true }
            SettingsDivider()
            SettingsRowSlider("Alarm Volume", alarmVolume) { value ->
                alarmVolume = value
                prefs.edit().putInt("alarm_volume_percent", value).apply()
                try {
                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    val mappedVol = (value * maxVol) / 100
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, mappedVol, 0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            SettingsDivider()
            SettingsRowChevron("Snooze Duration", "$snoozeDuration Minutes") { showSnoozeDurationPicker = true }
            SettingsDivider()
            SettingsRowToggle("Gradually Increase Volume", gradualVolume) { value ->
                gradualVolume = value
                prefs.edit().putBoolean("gradually_increase_volume", value).apply()
            }
            SettingsDivider()
            SettingsRowToggle("Vibrate", alarmVibrate) { value ->
                alarmVibrate = value
                prefs.edit().putBoolean("alarm_vibrate", value).apply()
            }
        }

        // TIMER section
        SettingsSectionHeader("TIMER")
        SettingsCard {
            val rawName = timerSoundName
            val displayedTimerSound = when {
                rawName.length > 25 -> rawName.take(22) + "..."
                rawName.contains("content://") -> "Custom Sound"
                rawName.any { it == '-' } && rawName.length > 20 -> "Custom Sound"
                else -> rawName
            }
            SettingsRowChevron("Default Timer Sound", displayedTimerSound) { showTimerSoundPicker = true }
            SettingsDivider()
            SettingsRowSlider("Timer Volume", timerVolume) { value ->
                timerVolume = value
                prefs.edit().putInt("timer_volume_percent", value).apply()
                try {
                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    val mappedVol = (value * maxVol) / 100
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, mappedVol, 0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            SettingsDivider()
            SettingsRowToggle("Keep Screen On During Timer", keepScreenOn) { value ->
                keepScreenOn = value
                prefs.edit().putBoolean("timer_keep_screen_on", value).apply()
            }
            SettingsDivider()
            SettingsRowToggle("Vibrate When Timer Ends", timerVibrate) { value ->
                timerVibrate = value
                prefs.edit().putBoolean("timer_vibrate", value).apply()
            }
        }

        // HAPTICS section
        SettingsSectionHeader("HAPTICS")
        SettingsCard {
            SettingsRowSlider("Vibration Intensity", vibrationIntensity) { value ->
                vibrationIntensity = value
                prefs.edit().putInt("vibration_intensity", value).apply()
            }
            SettingsDivider()
            SettingsRowToggle("Haptic Feedback", hapticFeedback) { value ->
                hapticFeedback = value
                prefs.edit().putBoolean("haptic_feedback_enabled", value).apply()
            }
            SettingsDivider()
            SettingsRowToggle("Timer Haptics", timerHaptics) { value ->
                timerHaptics = value
                prefs.edit().putBoolean("timer_haptics_enabled", value).apply()
            }
            SettingsDivider()
            SettingsRowToggle("Alarm Haptics", alarmHaptics) { value ->
                alarmHaptics = value
                prefs.edit().putBoolean("alarm_haptics_enabled", value).apply()
            }
        }

        // BATTERY & RELIABILITY section
        SettingsSectionHeader("BATTERY & RELIABILITY")
        SettingsCard {
            val pm = remember { context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager }
            val isIgnoring = remember { if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) pm.isIgnoringBatteryOptimizations(context.packageName) else true }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        HapticManager.light(context.applicationContext)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            try {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = android.net.Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val fallback = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(fallback)
                                } catch (ex: Exception) { ex.printStackTrace() }
                            }
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unrestricted Battery Access",
                        color = OnSurfaceLight,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = if (isIgnoring) "Active — alarms will fire on time" else "Tap to fix — alarms set for later may not ring",
                        color = if (isIgnoring) Color(0xFF4CAF50) else Color(0xFFF5A623),
                        fontSize = 12.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = OnSurfaceMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // OEM AUTOSTART section — shown only on devices that need it
        val oemBrand = remember { android.os.Build.BRAND.lowercase() }
        val oemManufacturer = remember { android.os.Build.MANUFACTURER.lowercase() }
        val isAggressiveOem = remember {
            listOf("infinix", "tecno", "itel", "transsion", "xiaomi", "redmi", "poco",
                   "oppo", "realme", "vivo", "oneplus", "huawei", "honor", "samsung")
                .any { oemBrand.contains(it) || oemManufacturer.contains(it) }
        }
        if (isAggressiveOem) {
            SettingsSectionHeader("AUTO-START PERMISSION")
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            HapticManager.light(context.applicationContext)
                            val intentsToTry = when {
                                oemBrand.contains("infinix") || oemBrand.contains("tecno") ||
                                oemBrand.contains("itel") || oemManufacturer.contains("transsion") -> listOf(
                                    "com.transsion.phonemaster/com.cyin.himgr.autostart.AutoStartActivity",
                                    "com.transsion.powercenter/.view.PowerManagerActivity",
                                    "com.infinix.security/.SecurityActivity"
                                )
                                oemBrand.contains("xiaomi") || oemBrand.contains("redmi") ||
                                oemBrand.contains("poco") -> listOf(
                                    "com.miui.securitycenter/.MainActivity",
                                    "com.miui.permcenter.autostart/.AutoStartManagementActivity"
                                )
                                oemBrand.contains("oppo") || oemBrand.contains("realme") -> listOf(
                                    "com.coloros.oppoguardelf/.powersave.PowerUsageModelActivity",
                                    "com.oppo.safe/.MainActivity"
                                )
                                oemBrand.contains("vivo") -> listOf(
                                    "com.vivo.permissionmanager/.activity.BgStartUpManagerActivity"
                                )
                                oemBrand.contains("huawei") || oemBrand.contains("honor") -> listOf(
                                    "com.huawei.systemmanager/.startupmgr.ui.StartupNormalAppListActivity",
                                    "com.huawei.systemmanager/.optimize.process.ProtectActivity"
                                )
                                oemBrand.contains("samsung") -> listOf(
                                    "com.samsung.android.lool/.ShortcutPickerActivity"
                                )
                                else -> emptyList()
                            }
                            var launched = false
                            for (target in intentsToTry) {
                                if (launched) break
                                try {
                                    val parts = target.split("/")
                                    val intent = android.content.Intent().apply {
                                        component = android.content.ComponentName(parts[0], parts[0] + parts[1])
                                    }
                                    context.startActivity(intent)
                                    launched = true
                                } catch (e: Exception) { /* try next */ }
                            }
                            if (!launched) {
                                // Fallback: open app's own battery settings page
                                try {
                                    val intent = android.content.Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    ).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Auto-Start",
                            color = OnSurfaceLight,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = "Required on your device for alarms to wake the phone from sleep. Tap to open your phone's security settings and enable auto-start for this app.",
                            color = Color(0xFFF5A623),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = OnSurfaceMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ABOUT section
        SettingsSectionHeader("ABOUT")
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Version",
                    color = OnSurfaceLight,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = versionName,
                    color = OnSurfaceMuted,
                    fontSize = 14.sp
                )
            }
            SettingsDivider()
            SettingsRowChevron("Rate the App", "") {
                val uri = Uri.parse("market://details?id=${context.packageName}")
                val playStoreIntent = Intent(Intent.ACTION_VIEW, uri)
                try {
                    context.startActivity(playStoreIntent)
                } catch (e: ActivityNotFoundException) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")))
                }
            }
            SettingsDivider()
            SettingsRowChevron("Privacy Policy", "") {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://zohaibjabbar.github.io/liquid-clock-privacy/"))
                context.startActivity(intent)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    AnimatedVisibility(
        visible = showAlarmSoundPicker,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(280)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(220)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .zIndex(10f)
        ) {
            val currentSoundUri = prefs.getString("default_alarm_sound_uri", "") ?: ""
            val currentSoundName = prefs.getString("default_alarm_sound_name", "Starlight") ?: "Starlight"
            val currentSoundValue = if (currentSoundUri.isNotEmpty()) currentSoundUri else currentSoundName
            
            SoundPickerDialog(
                currentSoundValue = currentSoundValue,
                onDismiss = { showAlarmSoundPicker = false },
                onConfirm = { selected ->
                    prefs.edit()
                        .putString("default_alarm_sound_name", selected.name)
                        .putString("default_alarm_sound_uri", if (selected.isCustom) selected.value else "")
                        .apply()
                    alarmSoundName = selected.name
                    showAlarmSoundPicker = false
                },
                onCustomSoundsChanged = {
                    // No-op for settings overview counts
                }
            )
        }
    }

    AnimatedVisibility(
        visible = showSnoozeDurationPicker,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(280)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(220)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .zIndex(10f)
        ) {
            val currentSnoozeMinutes = prefs.getInt("snooze_duration_minutes", 9)
            SnoozeDurationPickerDialog(
                currentMinutes = currentSnoozeMinutes,
                onDismiss = { showSnoozeDurationPicker = false },
                onConfirm = { minutes, label ->
                    prefs.edit().putInt("snooze_duration_minutes", minutes).apply()
                    snoozeDuration = minutes
                    showSnoozeDurationPicker = false
                }
            )
        }
    }

    AnimatedVisibility(
        visible = showTimerSoundPicker,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(280)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(220)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .zIndex(10f)
        ) {
            val currentSoundName = prefs.getString("default_timer_sound_name", "Crystal") ?: "Crystal"
            TimerSoundPickerDialog(
                currentSoundValue = currentSoundName,
                onDismiss = { showTimerSoundPicker = false },
                onConfirm = { selected ->
                    prefs.edit()
                        .putString("default_timer_sound_name", selected)
                        .remove("timer_custom_sound_uri")
                        .apply()
                    timerSoundName = selected
                    viewModel.selectTimerSound(selected)
                    showTimerSoundPicker = false
                },
                onSoundSelected = { name, uri ->
                    prefs.edit()
                        .putString("default_timer_sound_name", name)
                        .putString("timer_custom_sound_uri", uri)
                        .apply()
                    timerSoundName = name
                    viewModel.selectTimerSound(name)
                    showTimerSoundPicker = false
                }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = OnSurfaceMuted,
        fontSize = 12.sp,
        style = LabelCaps,
        modifier = Modifier
            .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .glassCard(shape = RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRowChevron(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                HapticManager.light(context.applicationContext)
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = OnSurfaceLight,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    color = OnSurfaceMuted,
                    fontSize = 14.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = OnSurfaceMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsRowToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp
        )
        LiquidGlassToggle(
            checked = checked,
            onCheckedChange = { isChecked ->
                HapticManager.light(context.applicationContext)
                onCheckedChange(isChecked)
            }
        )
    }
}

@Composable
private fun SettingsRowSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = OnSurfaceLight,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
            val displayVal = if (label == "Vibration Intensity") {
                if (value < 34) "Low" else if (value > 66) "High" else "Medium"
            } else {
                "$value%"
            }
            Text(
                text = displayVal,
                color = LiquidOrange,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { newVal ->
                onValueChange(newVal.toInt())
            },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = LiquidOrange,
                activeTrackColor = LiquidOrange,
                inactiveTrackColor = SurfaceGray
            )
        )
        if (label == "Vibration Intensity") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Low", color = OnSurfaceMuted, fontSize = 11.sp)
                Text("High", color = OnSurfaceMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = Color.White.copy(alpha = 0.08f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}
