# Readme Audit

## 1. Every .kt source file (app/src/main/java)

```
app\src\main\java\com\example\MainActivity.kt
app\src\main\java\com\example\ClockApplication.kt
app\src\main\java\com\example\HapticManager.kt
app\src\main\java\com\example\data\ClockDatabase.kt
app\src\main\java\com\example\data\ClockRepository.kt
app\src\main\java\com\example\ui\AlarmReceiver.kt
app\src\main\java\com\example\ui\AlarmRingingActivity.kt
app\src\main\java\com\example\ui\AlarmScheduler.kt
app\src\main\java\com\example\ui\AlarmService.kt
app\src\main\java\com\example\ui\ClockViewModel.kt
app\src\main\java\com\example\ui\LiquidSoundSynth.kt
app\src\main\java\com\example\ui\SoundHapticHelper.kt
app\src\main\java\com\example\ui\TimerRingingActivity.kt
app\src\main\java\com\example\ui\TimerService.kt
app\src\main\java\com\example\ui\screens\AlarmTab.kt
app\src\main\java\com\example\ui\screens\LiquidGlassToggle.kt
app\src\main\java\com\example\ui\screens\OnboardingScreen.kt
app\src\main\java\com\example\ui\screens\SettingsScreen.kt
app\src\main\java\com\example\ui\screens\SnoozeDurationPickerDialog.kt
app\src\main\java\com\example\ui\screens\SoundPickerDialog.kt
app\src\main\java\com\example\ui\screens\StopwatchTab.kt
app\src\main\java\com\example\ui\screens\TimerTab.kt
app\src\main\java\com\example\ui\screens\UiComponents.kt
app\src\main\java\com\example\ui\screens\WorldClockTab.kt
app\src\main\java\com\example\ui\theme\Color.kt
app\src\main\java\com\example\ui\theme\GlassModifiers.kt
app\src\main\java\com\example\ui\theme\Theme.kt
app\src\main\java\com\example\ui\theme\Type.kt
```

## 2. ClockViewModel.kt

**Path:** `app/src/main/java/com/example/ui/ClockViewModel.kt`

```kotlin
package com.example.ui

import android.app.Application
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AlarmEntity
import com.example.data.ClockDatabase
import com.example.data.ClockRepository
import com.example.data.WorldClockEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

data class AvailableCity(
    val id: String,
    val name: String,
    val timezoneId: String,
    val offsetText: String
)

class ClockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ClockRepository
    private val togglingAlarms = mutableSetOf<Long>()
    val alarms: StateFlow<List<AlarmEntity>>
    val worldClocks: StateFlow<List<WorldClockEntity>>

    private var lastTriggeredAlarmTime: String? = null
    private var ringtone: Ringtone? = null

    // TIMER STATE
    private val _timerHrs = MutableStateFlow(0)
    val timerHrs = _timerHrs.asStateFlow()

    private val _timerMin = MutableStateFlow(15)
    val timerMin = _timerMin.asStateFlow()

    private val _timerSec = MutableStateFlow(0)
    val timerSec = _timerSec.asStateFlow()

    private val _timerIsRunning = MutableStateFlow(false)
    val timerIsRunning = _timerIsRunning.asStateFlow()

    private val _timerRemainingMs = MutableStateFlow(15 * 60 * 1000L)
    val timerRemainingMs = _timerRemainingMs.asStateFlow()

    private val _timerTotalMs = MutableStateFlow(15 * 60 * 1000L)
    val timerTotalMs = _timerTotalMs.asStateFlow()

    private val _selectedPresetMin = MutableStateFlow(3)
    val selectedPresetMin = _selectedPresetMin.asStateFlow()

    val timerSounds = listOf("Dewdrop Serenade", "Bubble Resonance", "Ocean Breeze", "Liquid Echo", "Rainfall Melody")

    private val _selectedTimerSound = MutableStateFlow(
        application.getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
            .getString("default_timer_sound_name", "Dewdrop Serenade") ?: "Dewdrop Serenade"
    )
    val selectedTimerSound = _selectedTimerSound.asStateFlow()

    private var timerJob: Job? = null

    private val prefs = application.getSharedPreferences("world_clock_order", Context.MODE_PRIVATE)
    private val _worldClockOrder = MutableStateFlow<List<String>>(loadWorldClockOrder())

    private fun loadWorldClockOrder(): List<String> {
        val orderStr = prefs.getString("order_csv", "") ?: ""
        return if (orderStr.isEmpty()) emptyList() else orderStr.split(",")
    }

    fun saveWorldClockOrder(order: List<String>) {
        _worldClockOrder.value = order
        prefs.edit().putString("order_csv", order.joinToString(",")).apply()
    }

    init {
        val db = ClockDatabase.getDatabase(application)
        repository = ClockRepository(db)

        val firstLaunchPrefs = application.getSharedPreferences("app_launch_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = firstLaunchPrefs.getBoolean("is_first_launch", true)
        if (isFirstLaunch) {
            viewModelScope.launch {
                try {
                    repository.deleteAllAlarms()
                    repository.deleteAllWorldClocks()
                    firstLaunchPrefs.edit().putBoolean("is_first_launch", false).apply()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        alarms = repository.allAlarms.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        worldClocks = repository.allWorldClocks.combine(_worldClockOrder) { clocks, order ->
            if (order.isEmpty()) {
                clocks
            } else {
                clocks.sortedBy { clock ->
                    val index = order.indexOf(clock.cityId)
                    if (index == -1) Int.MAX_VALUE else index
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Reschedule alarms lazily after UI is ready — does not block startup
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                kotlinx.coroutines.delay(1500)
                val list = repository.allAlarms.first()
                list.forEach { alarm ->
                    if (alarm.isEnabled) {
                        AlarmScheduler.scheduleAlarm(application, alarm)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        checkTimerBackgroundState()
    }

    private fun checkAndTriggerAlarms() {
        val calendar = Calendar.getInstance()
        val currentHour12 = calendar.get(Calendar.HOUR)
        val currentHour = if (currentHour12 == 0) 12 else currentHour12
        val currentMinute = calendar.get(Calendar.MINUTE)
        val isAmToday = calendar.get(Calendar.AM_PM) == Calendar.AM
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val activeAlarms = alarms.value.filter { it.isEnabled }
        for (alarm in activeAlarms) {
            if (alarm.hour == currentHour && alarm.minute == currentMinute && alarm.isAm == isAmToday) {
                if (isAlarmScheduledForDay(alarm.repeatDays, dayOfWeek)) {
                    val triggerKey = "${alarm.id}-$dayOfWeek-$currentHour-$currentMinute"
                    if (lastTriggeredAlarmTime != triggerKey) {
                        lastTriggeredAlarmTime = triggerKey
                        triggerRinging(alarm)
                        
                        // Disable single-trigger alarm ("None" or blank) so it doesn't trigger tomorrow
                        if (alarm.repeatDays.isEmpty()) {
                            viewModelScope.launch {
                                repository.updateAlarm(alarm.copy(isEnabled = false))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isAlarmScheduledForDay(repeatDays: Set<Int>, dayOfWeek: Int): Boolean {
        if (repeatDays.isEmpty()) {
            return true
        }
        val idx = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> -1
        }
        return idx != -1 && repeatDays.contains(idx)
    }

    // Tab Navigation State
    private val _currentTab = MutableStateFlow(0)
    val currentTab = _currentTab.asStateFlow()

    private val _isShowingSettings = MutableStateFlow(false)
    val isShowingSettings = _isShowingSettings.asStateFlow()

    private val _isShowingTimerSoundPicker = MutableStateFlow(false)
    val isShowingTimerSoundPicker = _isShowingTimerSoundPicker.asStateFlow()

    fun showTimerSoundPicker(show: Boolean) {
        _isShowingTimerSoundPicker.value = show
    }

    fun selectTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    fun showSettings(show: Boolean) {
        _isShowingSettings.value = show
    }

    // World Clock - City database for searching
    val availableCities = listOf(
        AvailableCity("new_york", "New York", "America/New_York", "-5HRS"),
        AvailableCity("london", "London", "Europe/London", "+0HRS"),
        AvailableCity("tokyo", "Tokyo", "Asia/Tokyo", "+9HRS"),
        AvailableCity("paris", "Paris", "Europe/Paris", "+1HRS"),
        AvailableCity("sydney", "Sydney", "Australia/Sydney", "+10HRS"),
        AvailableCity("dubai", "Dubai", "Asia/Dubai", "+4HRS"),
        AvailableCity("los_angeles", "Los Angeles", "America/Los_Angeles", "-8HRS"),
        AvailableCity("hong_kong", "Hong Kong", "Asia/Hong_Kong", "+8HRS"),
        AvailableCity("cairo", "Cairo", "Africa/Cairo", "+2HRS")
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addWorldClock(city: AvailableCity) {
        viewModelScope.launch {
            val tz = TimeZone.getTimeZone(city.timezoneId)
            val rawOffset = tz.rawOffset
            val offsetHours = rawOffset / (1000 * 60 * 60)
            val offsetMinutes = (rawOffset / (1000 * 60)) % 60
            repository.insertWorldClock(
                WorldClockEntity(
                    cityId = city.id,
                    cityName = city.name,
                    timezoneId = city.timezoneId,
                    offsetHours = offsetHours,
                    offsetMinutes = offsetMinutes
                )
            )
        }
    }

    fun removeWorldClock(entity: WorldClockEntity) {
        viewModelScope.launch {
            repository.deleteWorldClock(entity)
        }
    }

    // Alarm edit/create flow states
    private val _isShowingAlarmSetup = MutableStateFlow(false)
    val isShowingAlarmSetup = _isShowingAlarmSetup.asStateFlow()

    private val _editingAlarm = MutableStateFlow<AlarmEntity?>(null)
    val editingAlarm = _editingAlarm.asStateFlow()

    private val _isChooseCityVisible = MutableStateFlow(false)
    val isChooseCityVisible = _isChooseCityVisible.asStateFlow()

    fun showAlarmSetup(show: Boolean) {
        _isShowingAlarmSetup.value = show
    }

    fun showEditingAlarm(alarm: AlarmEntity?) {
        _editingAlarm.value = alarm
    }

    private fun playToggleFeedback() {
        // Handled in Compose UI click listeners directly
    }

    fun saveEditedAlarm(id: Long, hour: Int, minute: Int, isAm: Boolean, repeatDays: Set<Int>, label: String, sound: String, isEnabled: Boolean) {
        playToggleFeedback()
        viewModelScope.launch {
            val alarm = AlarmEntity(
                id = id,
                hour = hour,
                minute = minute,
                isAm = isAm,
                repeatDays = repeatDays,
                label = label,
                sound = sound,
                isEnabled = isEnabled
            )
            repository.updateAlarm(alarm)
            AlarmScheduler.scheduleAlarm(getApplication(), alarm)
            _editingAlarm.value = null
        }
    }

    fun showChooseCity(show: Boolean) {
        _isChooseCityVisible.value = show
    }

    fun addNewAlarm(hour: Int, minute: Int, isAm: Boolean, repeatDays: Set<Int>, label: String, sound: String) {
        playToggleFeedback()
        viewModelScope.launch {
            val newId = repository.insertAlarm(
                AlarmEntity(
                    hour = hour,
                    minute = minute,
                    isAm = isAm,
                    repeatDays = repeatDays,
                    label = label,
                    sound = sound,
                    isEnabled = true
                )
            )
            val newAlarm = AlarmEntity(
                id = newId,
                hour = hour,
                minute = minute,
                isAm = isAm,
                repeatDays = repeatDays,
                label = label,
                sound = sound,
                isEnabled = true
            )
            AlarmScheduler.scheduleAlarm(getApplication(), newAlarm)
            _isShowingAlarmSetup.value = false
        }
    }

    fun toggleAlarm(alarm: AlarmEntity) {
        if (togglingAlarms.contains(alarm.id)) return
        togglingAlarms.add(alarm.id)
        viewModelScope.launch {
            try {
                try {
                    val updated = alarm.copy(isEnabled = !alarm.isEnabled)
                    repository.updateAlarm(updated)
                    AlarmScheduler.scheduleAlarm(getApplication(), updated)
                } catch (e: Exception) {
                    android.util.Log.e("TOGGLE_FIX", "scheduleAlarm failed: ${e.message}")
                }
            } finally {
                togglingAlarms.remove(alarm.id)
            }
        }
    }

    fun toggleAlarm(id: Long, isEnabled: Boolean) {
        android.util.Log.d("TOGGLE_CRASH", "toggleAlarm called — id=$id isEnabled=$isEnabled togglingAlarms=$togglingAlarms")
        if (togglingAlarms.contains(id)) return
        togglingAlarms.add(id)
        viewModelScope.launch {
            try {
                val existingList = repository.allAlarms.first()
                val alarm = existingList.find { it.id == id }
                if (alarm != null) {
                    try {
                        val updated = alarm.copy(isEnabled = isEnabled)
                        android.util.Log.d("TOGGLE_CRASH", "About to updateAlarm — alarm=$alarm")
                        repository.updateAlarm(updated)
                        AlarmScheduler.scheduleAlarm(getApplication(), updated)
                    } catch (e: Exception) {
                        android.util.Log.e("TOGGLE_FIX", "scheduleAlarm failed: ${e.message}")
                    }
                }
            } finally {
                togglingAlarms.remove(id)
            }
        }
    }

    fun updateAlarmDetail(alarm: AlarmEntity) {
        viewModelScope.launch {
            repository.updateAlarm(alarm)
            AlarmScheduler.scheduleAlarm(getApplication(), alarm)
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
            AlarmScheduler.cancelAlarm(getApplication(), alarm)
        }
    }

    // Ringing state for testing and demonstration
    private val _ringingAlarm = MutableStateFlow<AlarmEntity?>(null)
    val ringingAlarm = _ringingAlarm.asStateFlow()

    fun triggerRinging(alarm: AlarmEntity) {
        _ringingAlarm.value = alarm
        playAlarmSound(alarm.sound)
    }

    fun triggerRingingById(alarmId: Long) {
        viewModelScope.launch {
            try {
                val list = repository.allAlarms.first()
                val alarm = list.find { it.id == alarmId }
                if (alarm != null) {
                    _ringingAlarm.value = alarm
                    playAlarmSound(alarm.sound)
                    
                    // Show heads-up notification fallback
                    AlarmReceiver.showRingingNotification(getApplication(), alarmId)
                    
                    // Disable single-trigger alarm ("None" or blank) so it doesn't trigger tomorrow
                    if (alarm.repeatDays.isEmpty()) {
                        repository.updateAlarm(alarm.copy(isEnabled = false))
                    } else {
                        // Reschedule repeating alarms for next weekday
                        AlarmScheduler.scheduleAlarm(getApplication(), alarm)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopRinging() {
        val alarm = _ringingAlarm.value
        _ringingAlarm.value = null
        stopAlarmSound()
        alarm?.let {
            val notificationManager = getApplication<Application>().getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(it.id.toInt())
        }
    }

    fun snoozeRinging() {
        val alarm = _ringingAlarm.value
        if (alarm != null) {
            val context = getApplication<Application>()
            val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(alarm.id.toInt())
            
            val snoozePrefs = getApplication<Application>().getSharedPreferences("clock_settings", android.content.Context.MODE_PRIVATE)
            val snoozeMin = snoozePrefs.getInt("snooze_duration_minutes", 9)
            val calendar = Calendar.getInstance().apply {
                add(Calendar.MINUTE, snoozeMin)
            }
            
            var hour12 = calendar.get(Calendar.HOUR)
            if (hour12 == 0) hour12 = 12
            val isAm = calendar.get(Calendar.AM_PM) == Calendar.AM
            
            val snoozedAlarm = alarm.copy(
                hour = hour12,
                minute = calendar.get(Calendar.MINUTE),
                isAm = isAm,
                repeatDays = emptySet(),
                label = if (alarm.label.contains("Snoozed")) alarm.label else "${alarm.label} (Snoozed)",
                isEnabled = true
            )
            AlarmScheduler.scheduleAlarm(context, snoozedAlarm)
        }
        _ringingAlarm.value = null
        stopAlarmSound()
    }

    private fun playAlarmSound(soundName: String = "Dewdrop Serenade") {
        try {
            stopAlarmSound()
            val customKeys = listOf("Dewdrop Serenade", "Bubble Resonance", "Ocean Breeze", "Liquid Echo", "Rainfall Melody")
            if (soundName in customKeys) {
                LiquidSoundSynth.startPlaying(soundName)
            } else {
                val uri: Uri = if (soundName.startsWith("content://") || soundName.startsWith("file://") || soundName.contains("/")) {
                    Uri.parse(soundName)
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                }
                ringtone = RingtoneManager.getRingtone(getApplication(), uri)
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

    override fun onCleared() {
        super.onCleared()
        stopAlarmSound()
    }

    fun selectTimerSound(sound: String) {
        _selectedTimerSound.value = sound
        val prefs = getApplication<Application>().getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("default_timer_sound_name", sound).apply()
    }

    fun setTimerHrs(hrs: Int) {
        if (!_timerIsRunning.value) {
            _timerHrs.value = hrs.coerceIn(0, 23)
            updateRemainingFromInputs()
        }
    }

    fun setTimerMin(min: Int) {
        if (!_timerIsRunning.value) {
            _timerMin.value = min.coerceIn(0, 59)
            updateRemainingFromInputs()
        }
    }

    fun setTimerSec(sec: Int) {
        if (!_timerIsRunning.value) {
            _timerSec.value = sec.coerceIn(0, 59)
            updateRemainingFromInputs()
        }
    }

    fun resetTimerUIOnStaleState() {
        _timerIsRunning.value = false
        timerJob?.cancel()
        timerJob = null
        val totalMs = ((_timerHrs.value * 3600L) + (_timerMin.value * 60L) + _timerSec.value) * 1000L
        _timerRemainingMs.value = totalMs
        _timerTotalMs.value = totalMs
    }

    fun resetTimerToZero() {
        _timerIsRunning.value = false
        timerJob?.cancel()
        timerJob = null
        _timerHrs.value = 0
        _timerMin.value = 0
        _timerSec.value = 0
        _timerRemainingMs.value = 0L
        _timerTotalMs.value = 0L
        saveTimerStateToPrefs(isRunning = false, endTime = 0L, totalMs = 0L, remainingMs = 0L)
    }

    private fun saveTimerStateToPrefs(isRunning: Boolean, endTime: Long, totalMs: Long, remainingMs: Long) {
        val prefs = getApplication<Application>().getSharedPreferences("timer_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("timer_is_running", isRunning)
            putLong("timer_end_time", endTime)
            putLong("timer_total_ms", totalMs)
            putLong("timer_remaining_ms", remainingMs)
            apply()
        }
    }

    fun checkTimerBackgroundState() {
        val prefs = getApplication<Application>().getSharedPreferences("timer_prefs", android.content.Context.MODE_PRIVATE)
        val isRunning = prefs.getBoolean("timer_is_running", false)
        val totalMs = prefs.getLong("timer_total_ms", 15 * 60 * 1000L)
        
        if (isRunning) {
            val endTime = prefs.getLong("timer_end_time", 0L)
            val remaining = endTime - System.currentTimeMillis()
            
            _timerTotalMs.value = totalMs
            if (remaining <= 0) {
                _timerIsRunning.value = false
                _timerRemainingMs.value = 0
                
                saveTimerStateToPrefs(isRunning = false, endTime = 0L, totalMs = totalMs, remainingMs = 0L)
                
                // Trigger TimerService ringing
                val ringIntent = android.content.Intent(getApplication(), TimerService::class.java).apply {
                    action = TimerService.ACTION_RING
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    getApplication<Application>().startForegroundService(ringIntent)
                } else {
                    getApplication<Application>().startService(ringIntent)
                }
            } else {
                _timerRemainingMs.value = remaining
                _timerIsRunning.value = true
                resumeTimerCountdown(endTime)
            }
        } else {
            val savedRemaining = prefs.getLong("timer_remaining_ms", -1L)
            if (savedRemaining >= 0) {
                _timerRemainingMs.value = savedRemaining
                _timerTotalMs.value = totalMs
                if (savedRemaining == 0L) {
                    _timerIsRunning.value = false
                }
            }
        }
    }

    fun onResume() {
        checkTimerBackgroundState()
    }

    private fun resumeTimerCountdown(endTime: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var lastVibratedSecond = 0L
            while (_timerIsRunning.value && _timerRemainingMs.value > 0) {
                val remaining = endTime - System.currentTimeMillis()
                _timerRemainingMs.value = remaining.coerceAtLeast(0)
                
                // Final 5 seconds haptic feedback ticking
                if (remaining in 1..5050L) {
                    val currentSec = kotlin.math.ceil(remaining.toDouble() / 1000.0).toLong()
                    if (currentSec in 1..5 && currentSec != lastVibratedSecond) {
                        lastVibratedSecond = currentSec
                        triggerSubtleHapticTick(currentSec)
                    }
                }

                if (_timerRemainingMs.value <= 0) {
                    _timerIsRunning.value = false
                    saveTimerStateToPrefs(isRunning = false, endTime = 0L, totalMs = _timerTotalMs.value, remainingMs = 0L)
                    
                    // Trigger TimerService ringing
                    val ringIntent = android.content.Intent(getApplication(), TimerService::class.java).apply {
                        action = TimerService.ACTION_RING
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        getApplication<Application>().startForegroundService(ringIntent)
                    } else {
                        getApplication<Application>().startService(ringIntent)
                    }
                }
                delay(100)
            }
        }
    }

    private fun updateRemainingFromInputs() {
        val totalMs = ((_timerHrs.value * 3600L) + (_timerMin.value * 60L) + _timerSec.value) * 1000L
        _timerRemainingMs.value = totalMs
        _timerTotalMs.value = totalMs
        saveTimerStateToPrefs(isRunning = false, endTime = 0L, totalMs = totalMs, remainingMs = totalMs)
    }

    fun selectPreset(minutes: Int) {
        _selectedPresetMin.value = minutes
        _timerHrs.value = 0
        _timerMin.value = minutes
        _timerSec.value = 0
        updateRemainingFromInputs()
    }

    private fun triggerSubtleHapticTick(second: Long) {
        try {
            val vibrator = getApplication<Application>().getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator ?: return
            if (!vibrator.hasVibrator()) return

            val prefs = getApplication<Application>().getSharedPreferences("clock_settings", android.content.Context.MODE_PRIVATE)
            val globalHapticsEnabled = prefs.getBoolean("haptic_feedback_enabled", true)
            if (!globalHapticsEnabled) return

            val intensity = prefs.getInt("vibration_intensity", 70)
            if (intensity <= 0) return

            val intensityFactor = intensity / 100.0

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val effect = when (second) {
                    5L -> android.os.VibrationEffect.createOneShot(30, ((40 * intensityFactor).toInt().coerceIn(1, 255))) // softest tick
                    4L -> android.os.VibrationEffect.createOneShot(45, ((80 * intensityFactor).toInt().coerceIn(1, 255))) // light tick
                    3L -> android.os.VibrationEffect.createOneShot(60, ((120 * intensityFactor).toInt().coerceIn(1, 255))) // medium tick
                    2L -> android.os.VibrationEffect.createOneShot(80, ((180 * intensityFactor).toInt().coerceIn(1, 255))) // strong tick
                    1L -> {
                        // Double beat for intense feedback on final second
                        android.os.VibrationEffect.createWaveform(
                            longArrayOf(0, 50, 45, 110),
                            intArrayOf(0, ((110 * intensityFactor).toInt().coerceIn(1, 255)), 0, ((255 * intensityFactor).toInt().coerceIn(1, 255))),
                            -1
                        )
                    }
                    else -> null
                }
                if (effect != null) {
                    vibrator.vibrate(effect)
                }
            } else {
                val duration = when (second) {
                    5L -> (30L * intensityFactor).toLong()
                    4L -> (45L * intensityFactor).toLong()
                    3L -> (60L * intensityFactor).toLong()
                    2L -> (80L * intensityFactor).toLong()
                    1L -> (200L * intensityFactor).toLong()
                    else -> 0L
                }
                if (duration > 0) {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(duration)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startTimer() {
        if (_timerIsRunning.value) {
            pauseTimer()
            return
        }

        val totalMs = _timerRemainingMs.value
        if (totalMs <= 0) return

        _timerIsRunning.value = true
        _timerTotalMs.value = totalMs

        val endTime = System.currentTimeMillis() + totalMs
        saveTimerStateToPrefs(isRunning = true, endTime = endTime, totalMs = totalMs, remainingMs = totalMs)

        // Start Foreground Service
        val intent = android.content.Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_START_TIMER
            putExtra("EXTRA_END_TIME", endTime)
            putExtra("EXTRA_TOTAL_MS", totalMs)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }

        resumeTimerCountdown(endTime)
    }

    fun pauseTimer() {
        _timerIsRunning.value = false
        timerJob?.cancel()
        saveTimerStateToPrefs(isRunning = false, endTime = 0L, totalMs = _timerTotalMs.value, remainingMs = _timerRemainingMs.value)

        // Pause Foreground Service
        val intent = android.content.Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE_TIMER
        }
        getApplication<Application>().startService(intent)
    }

    fun resetTimer() {
        pauseTimer()
        selectPreset(_selectedPresetMin.value)
        saveTimerStateToPrefs(isRunning = false, endTime = 0L, totalMs = _timerTotalMs.value, remainingMs = _timerRemainingMs.value)
    }

    // STOPWATCH STATE
    private val _stopwatchTimeMs = MutableStateFlow(0L)
    val stopwatchTimeMs = _stopwatchTimeMs.asStateFlow()

    private val _stopwatchIsRunning = MutableStateFlow(false)
    val stopwatchIsRunning = _stopwatchIsRunning.asStateFlow()

    private val _stopwatchLaps = MutableStateFlow<List<Long>>(emptyList())
    val stopwatchLaps = _stopwatchLaps.asStateFlow()

    private var stopwatchJob: Job? = null
    private var lastRecordedTime = System.currentTimeMillis()

    fun startStopwatch() {
        if (_stopwatchIsRunning.value) {
            pauseStopwatch()
            return
        }

        _stopwatchIsRunning.value = true
        lastRecordedTime = System.currentTimeMillis()

        stopwatchJob = viewModelScope.launch {
            while (_stopwatchIsRunning.value) {
                val now = System.currentTimeMillis()
                val delta = now - lastRecordedTime
                _stopwatchTimeMs.value += delta
                lastRecordedTime = now
                delay(10)
            }
        }
    }

    fun pauseStopwatch() {
        _stopwatchIsRunning.value = false
        stopwatchJob?.cancel()
    }

    fun lapStopwatch() {
        if (!_stopwatchIsRunning.value) return

        val totalTime = _stopwatchTimeMs.value
        val sumOfPriorLaps = _stopwatchLaps.value.sum()
        val currentLapTime = totalTime - sumOfPriorLaps

        val newList = ArrayList<Long>()
        newList.add(currentLapTime)
        newList.addAll(_stopwatchLaps.value)
        _stopwatchLaps.value = newList
    }

    fun resetStopwatch() {
        pauseStopwatch()
        _stopwatchTimeMs.value = 0L
        _stopwatchLaps.value = emptyList()
    }
}
```

## 3. strings.xml

**Path:** `app/src/main/res/values/strings.xml`

```xml
<resources>
    <string name="app_name">Liquid Clock</string>
</resources>
```

**app_name:** `Liquid Clock`

## 4. SettingsScreen.kt — Setting row labels

**ALARMS section:**
- Default Alarm Sound
- Alarm Volume (slider)
- Snooze Duration
- Gradually Increase Volume (toggle)
- Vibrate (toggle)

**TIMER section:**
- Default Timer Sound
- Timer Volume (slider)
- Keep Screen On During Timer (toggle)
- Vibrate When Timer Ends (toggle)

**HAPTICS section:**
- Vibration Intensity (slider)
- Haptic Feedback (toggle)
- Timer Haptics (toggle)
- Alarm Haptics (toggle)

**BATTERY & RELIABILITY section:**
- Unrestricted Battery Access (with status text)

**AUTO-START PERMISSION section (conditional — aggressive OEM devices only):**
- Enable Auto-Start (with description text)

**ABOUT section:**
- Version (display only)
- Rate the App
- Privacy Policy

## 5. git remote -v

```
origin	https://github.com/zohaibjabbar/liquid-clock.git (fetch)
origin	https://github.com/zohaibjabbar/liquid-clock.git (push)
```

## 6. LICENSE file in project root

No
