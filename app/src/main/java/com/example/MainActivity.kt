package com.example

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.ClockViewModel
import com.example.ui.screens.AlarmTab
import com.example.ui.screens.AtmosphericLiquidBg
import com.example.ui.screens.LiquidGlassBottomNav
import com.example.ui.screens.StopwatchTab
import com.example.ui.screens.TimerTab
import com.example.ui.screens.WorldClockTab
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.TimerSoundPickerDialog
import androidx.compose.foundation.background

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

data class PermissionDetails(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val onGrant: (Context) -> Unit
)

class MainActivity : ComponentActivity() {
    private var viewModelRef: ClockViewModel? = null
    private val permissionsList = mutableStateOf<List<PermissionDetails>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate
        installSplashScreen()

        // Global uncaught exception handler to prevent stale state on crashes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("TOGGLE_CRASH", "=== CRASH CAUGHT ===")
            android.util.Log.e("TOGGLE_CRASH", "Thread: ${thread.name}")
            android.util.Log.e("TOGGLE_CRASH", "Exception: ${throwable.javaClass.name}")
            android.util.Log.e("TOGGLE_CRASH", "Message: ${throwable.message}")
            throwable.stackTrace.forEach { element ->
                android.util.Log.e("TOGGLE_CRASH", "  at $element")
            }
            try {
                // Safely reset timer state in prefs before crashing
                val prefs = getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putBoolean("timer_is_running", false)
                    putLong("timer_end_time", 0L)
                    putLong("timer_remaining_ms", 0L)
                    apply()
                }
                
                // Stop service to prevent stuck state
                val intent = Intent(this, com.example.ui.TimerService::class.java)
                stopService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        super.onCreate(savedInstanceState)

        // Lock screen flags only needed when launched by an alarm ring intent
        val isAlarmLaunch = intent?.getLongExtra("RING_ALARM_ID", -1L) != -1L
        if (isAlarmLaunch) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
                keyguardManager?.requestDismissKeyguard(this, null)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                )
            }
        }

        // Defer permission check to after first frame to avoid blocking startup
        enableEdgeToEdge()

        val viewModel = androidx.lifecycle.ViewModelProvider(this)[ClockViewModel::class.java]
        viewModelRef = viewModel

        // Verify timer service state on startup
        verifyTimerServiceState(this, viewModel)

        // Process any extras in starting intent
        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                val prefs = getSharedPreferences("app_init", Context.MODE_PRIVATE)
                val onboardingDone = prefs.getBoolean("onboarding_done", false)
                val needsOverlay = !android.provider.Settings.canDrawOverlays(this)
                val needsNotification = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        this, android.Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                } else false

                var showOnboarding by remember {
                    mutableStateOf(!onboardingDone || needsOverlay || needsNotification)
                }

                if (showOnboarding) {
                    OnboardingScreen(
                        onFinished = {
                            prefs.edit().putBoolean("onboarding_done", true).apply()
                            showOnboarding = false
                        }
                    )
                } else {
                    val currentTab by viewModel.currentTab.collectAsState()
                    val isChooseCityVisible by viewModel.isChooseCityVisible.collectAsState()
                    val isShowingAlarmSetup by viewModel.isShowingAlarmSetup.collectAsState()
                    val editingAlarm by viewModel.editingAlarm.collectAsState()
                    val ringingAlarm by viewModel.ringingAlarm.collectAsState()
                    val isShowingSettings by viewModel.isShowingSettings.collectAsState()
                    val isShowingTimerSoundPicker by viewModel.isShowingTimerSoundPicker.collectAsState()
                    val selectedTimerSound by viewModel.selectedTimerSound.collectAsState()

                    val permissions by permissionsList
                    val missingPermissions = permissions.filter { !it.isGranted }
                    val showPermissionDialog = missingPermissions.isNotEmpty()

                    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent // Allow underlay layout gradients to bleed in
                    ) { innerPadding ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Ambient blurred drift background underlay
                            AtmosphericLiquidBg()

                            // Solid black status bar scrim behind system icons
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .windowInsetsTopHeight(WindowInsets.statusBars)
                                    .background(Color.Black)
                            )

                            // Active View matching current bottom nav tab selected with swipe-transition animation
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (isLandscape)
                                            Modifier.padding(start = 104.dp) // rail width 80dp + 12dp margin + 12dp gap
                                        else
                                            Modifier.padding(bottom = innerPadding.calculateBottomPadding() / 4)
                                    )
                            ) {
                                AnimatedContent(
                                    targetState = currentTab,
                                    transitionSpec = {
                                        val direction = if (targetState > initialState) 1 else -1
                                        (fadeIn(animationSpec = tween(350)) + slideInHorizontally(animationSpec = tween(400)) { width -> (width * 0.15f * direction).toInt() })
                                            .togetherWith(fadeOut(animationSpec = tween(250)) + slideOutHorizontally(animationSpec = tween(350)) { width -> (-width * 0.15f * direction).toInt() })
                                    },
                                    label = "tab_transition",
                                    modifier = Modifier.fillMaxSize()
                                 ) { targetTab ->
                                    when (targetTab) {
                                        0 -> WorldClockTab(viewModel = viewModel)
                                        1 -> AlarmTab(viewModel = viewModel)
                                        2 -> StopwatchTab(viewModel = viewModel)
                                        3 -> TimerTab(viewModel = viewModel)
                                    }
                                }
                            }

                            // Shared global Nav — bottom bar in portrait, side rail in landscape
                            val navVisible = !isChooseCityVisible && !isShowingAlarmSetup && editingAlarm == null && ringingAlarm == null && !showPermissionDialog && !isShowingTimerSoundPicker && !isShowingSettings

                            if (isLandscape) {
                                AnimatedVisibility(
                                    visible = navVisible,
                                    enter = fadeIn(tween(250)) + slideInHorizontally(animationSpec = tween(250)) { -it / 2 },
                                    exit = fadeOut(tween(200)) + slideOutHorizontally(animationSpec = tween(200)) { -it / 2 },
                                    modifier = Modifier.align(Alignment.CenterStart)
                                ) {
                                    // ===== LANDSCAPE RAIL POSITION — EDIT THESE 4 VALUES =====
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .padding(
                                                start = 12.dp,
                                                top = 32.dp,
                                                bottom = 8.dp
                                            ),
                                        contentAlignment = Alignment.TopStart
                                    ) {
                                        com.example.ui.screens.LiquidGlassNavRail(
                                            activeTab = currentTab,
                                            onTabSelected = {
                                                viewModel.selectTab(it)
                                                viewModel.showSettings(false)
                                            }
                                        )
                                    }
                                }
                            } else {
                                AnimatedVisibility(
                                    visible = navVisible,
                                    enter = fadeIn(tween(250)) + slideInVertically(animationSpec = tween(250)) { it / 2 },
                                    exit = fadeOut(tween(200)) + slideOutVertically(animationSpec = tween(200)) { it / 2 },
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                ) {
                                    LiquidGlassBottomNav(
                                        activeTab = currentTab,
                                        onTabSelected = {
                                            viewModel.selectTab(it)
                                            viewModel.showSettings(false)
                                        }
                                    )
                                }
                            }

                            // Immersive active settings screen (Global Overlay)
                            AnimatedVisibility(
                                visible = isShowingSettings,
                                enter = slideInVertically(
                                    initialOffsetY = { fullHeight -> fullHeight },
                                    animationSpec = tween(durationMillis = 320)
                                ) + fadeIn(
                                    animationSpec = tween(durationMillis = 280)
                                ),
                                exit = slideOutVertically(
                                    targetOffsetY = { fullHeight -> fullHeight },
                                    animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing)
                                ) + fadeOut(
                                    animationSpec = tween(durationMillis = 180)
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                ) {
                                    SettingsScreen(viewModel = viewModel)
                                }
                            }

                            // Global Timer Sound Picker Overlay
                            AnimatedVisibility(
                                visible = isShowingTimerSoundPicker,
                                enter = slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = tween(280)
                                ) + fadeIn(animationSpec = tween(280)),
                                exit = slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(220)
                                ) + fadeOut(animationSpec = tween(180))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black)
                                ) {
                                    TimerSoundPickerDialog(
                                        currentSoundValue = selectedTimerSound,
                                        onDismiss = { viewModel.showTimerSoundPicker(false) },
                                        onConfirm = { selected ->
                                            viewModel.selectTimerSound(selected)
                                            val prefs = getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
                                            prefs.edit().remove("timer_custom_sound_uri").apply()
                                            viewModel.showTimerSoundPicker(false)
                                        },
                                        onSoundSelected = { name, uri ->
                                            viewModel.selectTimerSound(name)
                                            val prefs = getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
                                            prefs.edit().putString("timer_custom_sound_uri", uri).apply()
                                            viewModel.showTimerSoundPicker(false)
                                        }
                                    )
                                }
                            }

                            // Immersive active ringing screen modal (Global Overlay)
                            AnimatedVisibility(
                                visible = ringingAlarm != null,
                                enter = fadeIn(tween(400)),
                                exit = fadeOut(tween(300)),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                ringingAlarm?.let { activeAlarm ->
                                    com.example.ui.screens.ActiveAlarmRingingOverlay(
                                        alarm = activeAlarm,
                                        onStop = { 
                                            viewModel.stopRinging()
                                            this@MainActivity.finish()
                                        },
                                        onSnooze = { 
                                            viewModel.snoozeRinging()
                                            this@MainActivity.finish()
                                        }
                                    )
                                }
                            }

                            // Permissions Checklist Dialog overlay
                            if (showPermissionDialog) {
                                AlertDialog(
                                    onDismissRequest = { /* Must satisfy critical permissions */ },
                                    title = {
                                        Text(
                                            text = "Reliable Alarm Setup",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    },
                                    text = {
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            item {
                                                Text(
                                                    text = "To ensure alarms wake you up exactly on time and work while your phone is locked or asleep, please configure these settings:",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            items(missingPermissions) { item ->
                                                Card(
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                    )
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(12.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Text(
                                                                text = item.title,
                                                                style = MaterialTheme.typography.titleMedium,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            Button(
                                                                onClick = { item.onGrant(this@MainActivity) },
                                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                                modifier = Modifier.height(32.dp)
                                                            ) {
                                                                Text(
                                                                    text = "Configure",
                                                                    style = MaterialTheme.typography.labelMedium
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text(
                                                            text = item.description,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                            item {
                                                Text(
                                                    text = "System authorized: Wake Lock, Boot Reschedule, and Exact Use are active.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                permissionsList.value = checkAllRequiredPermissions()
                                            }
                                        ) {
                                            Text("I have configured them")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        try {
            val iconDrawable = packageManager.getApplicationIcon(applicationInfo)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
                if (iconDrawable is android.graphics.drawable.AnimatedVectorDrawable) {
                    iconDrawable.start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        handleIntent(intent)

        // Now check permissions after UI is up (avoids binder calls blocking first frame)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            permissionsList.value = checkAllRequiredPermissions()
        }, 300)
    }

    override fun onResume() {
        super.onResume()
        permissionsList.value = checkAllRequiredPermissions()
        viewModelRef?.onResume()
    }

    private fun checkAllRequiredPermissions(): List<PermissionDetails> {
        val list = mutableListOf<PermissionDetails>()

        // 1. SCHEDULE_EXACT_ALARM (Android 12+)
        val exactAlarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        list.add(
            PermissionDetails(
                id = "SCHEDULE_EXACT_ALARM",
                title = "Exact Alarms Settings",
                description = "Required to schedule exact clock alarms. Directs you to the Special App Access settings page of your device to authorize 'Alarms & reminders'.",
                isGranted = exactAlarmGranted,
                onGrant = { context ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )
        )

        // 2. USE_FULL_SCREEN_INTENT (Android 14+)
        val fullScreenGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.canUseFullScreenIntent()
        } else {
            true
        }
        list.add(
            PermissionDetails(
                id = "USE_FULL_SCREEN_INTENT",
                title = "Full-Screen Notifications",
                description = "Required to display alarms immediately over locked screens. Directs you to the settings page to authorize full screen overlay.",
                isGranted = fullScreenGranted,
                onGrant = { context ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        try {
                            val intent = Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT").apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val fallbackIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(fallbackIntent)
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                    }
                }
            )
        )

        // 3. POST_NOTIFICATIONS (Android 13+)
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        list.add(
            PermissionDetails(
                id = "POST_NOTIFICATIONS",
                title = "Push notifications",
                description = "Required to post heads-up alerts and ringing banners when the app executes in background.",
                isGranted = notificationsGranted,
                onGrant = { context ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        try {
                            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )
        )

        // 4. SYSTEM_ALERT_WINDOW (Android 10+ Draw over other apps)
        val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(this)
        } else {
            true
        }
        list.add(
            PermissionDetails(
                id = "SYSTEM_ALERT_WINDOW",
                title = "Display over other apps",
                description = "Required on Android 10 and above to instantly launch the full-screen ringing UI on top of other apps or the home screen when an alarm triggers.",
                isGranted = overlayGranted,
                onGrant = { context ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val intent = Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:" + context.packageName)
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )
        )

        // 5. IGNORE_BATTERY_OPTIMIZATIONS — prevents OS from killing alarms set far in the future
        val batteryOptimizationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            pm.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
        list.add(
            PermissionDetails(
                id = "IGNORE_BATTERY_OPTIMIZATIONS",
                title = "Unrestricted Battery Access",
                description = "Prevents the OS from killing alarms set hours or days in the future. Without this, alarms set for tomorrow or later may not ring.",
                isGranted = batteryOptimizationGranted,
                onGrant = { context ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val fallback = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(fallback)
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                    }
                }
            )
        )

        return list
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val viewModel = viewModelRef ?: return
        if (intent == null) return

        val alarmId = intent.getLongExtra("RING_ALARM_ID", -1L)
        if (alarmId != -1L) {
            intent.removeExtra("RING_ALARM_ID")
            viewModel.triggerRingingById(alarmId)
        }

        if (intent.hasExtra("SELECT_TAB")) {
            val tab = intent.getIntExtra("SELECT_TAB", 0)
            viewModel.selectTab(tab)
        }
        if (intent.getBooleanExtra("RESET_TIMER_TO_ZERO", false)) {
            viewModel.resetTimerToZero()
        }
    }

    private fun verifyTimerServiceState(context: Context, viewModel: ClockViewModel) {
        val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        val isSavedRunning = prefs.getBoolean("timer_is_running", false)
        if (isSavedRunning) {
            val isServiceActuallyRunning = isServiceRunning(context, com.example.ui.TimerService::class.java)
            if (!isServiceActuallyRunning) {
                try {
                    val stopServiceIntent = Intent(context, com.example.ui.TimerService::class.java)
                    context.stopService(stopServiceIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                prefs.edit().clear().apply()
                viewModel.resetTimerToZero()
            }
        }
    }

    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        // getRunningServices() is deprecated and unreliable on Android 8+.
        // We use the SharedPreferences flag written by TimerService as the source of truth.
        if (serviceClass == com.example.ui.TimerService::class.java) {
            val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
            return prefs.getBoolean("timer_is_running", false)
        }
        return false
    }
}
