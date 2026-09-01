package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.ui.SoundHapticHelper
import com.example.HapticManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ClockViewModel
import com.example.ui.theme.BodyLg
import com.example.ui.theme.BodySm
import com.example.ui.theme.GlassBgCard
import com.example.ui.theme.LabelCaps
import com.example.ui.theme.OnSurfaceLight
import com.example.ui.theme.OnSurfaceMuted
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SecondaryRed
import com.example.ui.theme.glassCard
import com.example.ui.theme.glassStrongCard
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import android.content.Context

@Composable
fun WorldClockTab(viewModel: ClockViewModel) {
    val context = LocalContext.current
    val worldClocks by viewModel.worldClocks.collectAsState()
    val isChooseCityVisible by viewModel.isChooseCityVisible.collectAsState()

    // Live clock ticker
    var tickTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            tickTrigger++
        }
    }

    var reorderableClocks by remember(worldClocks) { mutableStateOf(worldClocks) }
    var activeDraggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragAccumulatedOffset by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    var itemHeightPx by remember { mutableStateOf(0f) }
    LaunchedEffect(density) {
        itemHeightPx = with(density) { 112.dp.toPx() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val blurRadius by animateDpAsState(
            targetValue = if (isChooseCityVisible) 16.dp else 0.dp,
            animationSpec = tween(durationMillis = 300),
            label = "world_clock_blur"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius)
                .padding(horizontal = 16.dp)
                .padding(top = 56.dp, bottom = 100.dp)
        ) {
            // Header Group (Matches title and '+' exact placement from screenshots)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "World Clock",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight,
                    letterSpacing = (-0.5).sp
                )

                // Plus Add Button matching exact circle styling
                GlassIconBtn(
                    icon = Icons.Default.Add,
                    onClick = { viewModel.showChooseCity(true) },
                    tint = PrimaryGreen
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (worldClocks.isEmpty()) {
                // Empty state matching Screenshot 1 visual layout exactly
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 0.8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(25000 / 10, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "p_alpha"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.alpha(pulseAlpha)
                    ) {
                        // Globe inside a clear 40% glass sphere
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .glassCard(shape = CircleShape, bgColor = Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "",
                                tint = OnSurfaceMuted.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "No World Clocks",
                            style = BodyLg,
                            color = OnSurfaceMuted.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = reorderableClocks,
                        key = { _, entity -> entity.cityId }
                    ) { index, entity ->
                        val temp = tickTrigger // trigger recompose every second
                        val liveTime = getLocalTimeForTimeZone(entity.timezoneId, context)
                        val relativeDayText = getDayAndOffset(entity.timezoneId, getOffsetString(entity.offsetHours))

                        val isDragged = index == activeDraggedIndex
                        val dragOffset = if (isDragged) dragAccumulatedOffset else 0f
                        val currentIndexState = rememberUpdatedState(index)
                        val spacingPx = with(density) { 12.dp.toPx() }
                        val swapThreshold = itemHeightPx + spacingPx

                        SwipeToRevealItem(
                            onDelete = { viewModel.removeWorldClock(entity) },
                            swipeEnabled = (activeDraggedIndex == null),
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(0, dragOffset.roundToInt()) }
                                .zIndex(if (isDragged) 10f else 1f)
                                .animateItem()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { size ->
                                        if (itemHeightPx != size.height.toFloat()) {
                                            itemHeightPx = size.height.toFloat()
                                        }
                                    }
                                    .glassCard(shape = RoundedCornerShape(20.dp))
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    // Reorder Drag Handle Icon
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Drag to reorder",
                                        tint = OnSurfaceMuted.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .padding(end = 12.dp)
                                            .size(24.dp)
                                            .pointerInput(Unit) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        activeDraggedIndex = currentIndexState.value
                                                        dragAccumulatedOffset = 0f
                                                        HapticManager.light(context.applicationContext)
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragAccumulatedOffset += dragAmount.y
                                                        val currentIndex = activeDraggedIndex ?: return@detectDragGesturesAfterLongPress
                                                        val nextIndex = currentIndex + 1
                                                        val prevIndex = currentIndex - 1

                                                        if (dragAccumulatedOffset > swapThreshold && nextIndex < reorderableClocks.size) {
                                                            val newList = reorderableClocks.toMutableList()
                                                            val item = newList.removeAt(currentIndex)
                                                            newList.add(nextIndex, item)
                                                            reorderableClocks = newList
                                                            activeDraggedIndex = nextIndex
                                                            dragAccumulatedOffset -= swapThreshold
                                                            HapticManager.light(context.applicationContext)
                                                        } else if (dragAccumulatedOffset < -swapThreshold && prevIndex >= 0) {
                                                            val newList = reorderableClocks.toMutableList()
                                                            val item = newList.removeAt(currentIndex)
                                                            newList.add(prevIndex, item)
                                                            reorderableClocks = newList
                                                            activeDraggedIndex = prevIndex
                                                            dragAccumulatedOffset += swapThreshold
                                                            HapticManager.light(context.applicationContext)
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        if (activeDraggedIndex != null) {
                                                            viewModel.saveWorldClockOrder(reorderableClocks.map { it.cityId })
                                                            HapticManager.medium(context.applicationContext)
                                                        }
                                                        activeDraggedIndex = null
                                                        dragAccumulatedOffset = 0f
                                                    },
                                                    onDragCancel = {
                                                        activeDraggedIndex = null
                                                        dragAccumulatedOffset = 0f
                                                    }
                                                )
                                            }
                                    )

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = entity.cityName,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = OnSurfaceLight
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            val tz = TimeZone.getTimeZone(entity.timezoneId)
                                            val currentOffsetMs = tz.getOffset(System.currentTimeMillis())
                                            val offsetMinutesTotal = currentOffsetMs / (1000 * 60)
                                            val hours = Math.abs(offsetMinutesTotal / 60)
                                            val minutes = Math.abs(offsetMinutesTotal % 60)
                                            val sign = if (offsetMinutesTotal >= 0) "+" else "-"
                                            val offsetFormatted = if (minutes == 0) {
                                                "GMT$sign$hours"
                                            } else {
                                                "GMT$sign$hours:${String.format("%02d", minutes)}"
                                            }
                                            Text(
                                                text = "($offsetFormatted)",
                                                fontSize = 14.sp,
                                                color = OnSurfaceMuted
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = relativeDayText,
                                            style = BodySm,
                                            color = OnSurfaceMuted
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = liveTime,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = OnSurfaceLight
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Search Slide Up Overlay (Choose a City)
        AnimatedVisibility(
            visible = isChooseCityVisible,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(200))
        ) {
            ChooseCityOverlay(
                viewModel = viewModel,
                onDismiss = { viewModel.showChooseCity(false) }
            )
        }
    }
}

data class CityEntry(val name: String, val timezone: String, val countryCode: String)

val allCities = listOf(
    // ASIA
    CityEntry("Karachi", "Asia/Karachi", "PK"),
    CityEntry("Lahore", "Asia/Karachi", "PK"),
    CityEntry("Islamabad", "Asia/Karachi", "PK"),
    CityEntry("Mumbai", "Asia/Kolkata", "IN"),
    CityEntry("Delhi", "Asia/Kolkata", "IN"),
    CityEntry("Bangalore", "Asia/Kolkata", "IN"),
    CityEntry("Chennai", "Asia/Kolkata", "IN"),
    CityEntry("Kolkata", "Asia/Kolkata", "IN"),
    CityEntry("Hyderabad", "Asia/Kolkata", "IN"),
    CityEntry("Dhaka", "Asia/Dhaka", "BD"),
    CityEntry("Colombo", "Asia/Colombo", "LK"),
    CityEntry("Kathmandu", "Asia/Kathmandu", "NP"),
    CityEntry("Kabul", "Asia/Kabul", "AF"),
    CityEntry("Tashkent", "Asia/Tashkent", "UZ"),
    CityEntry("Almaty", "Asia/Almaty", "KZ"),
    CityEntry("Bishkek", "Asia/Bishkek", "KG"),
    CityEntry("Dushanbe", "Asia/Dushanbe", "TJ"),
    CityEntry("Ashgabat", "Asia/Ashgabat", "TM"),
    CityEntry("Baku", "Asia/Baku", "AZ"),
    CityEntry("Yerevan", "Asia/Yerevan", "AM"),
    CityEntry("Tbilisi", "Asia/Tbilisi", "GE"),
    CityEntry("Tehran", "Asia/Tehran", "IR"),
    CityEntry("Baghdad", "Asia/Baghdad", "IQ"),
    CityEntry("Riyadh", "Asia/Riyadh", "SA"),
    CityEntry("Dubai", "Asia/Dubai", "AE"),
    CityEntry("Abu Dhabi", "Asia/Dubai", "AE"),
    CityEntry("Doha", "Asia/Qatar", "QA"),
    CityEntry("Kuwait City", "Asia/Kuwait", "KW"),
    CityEntry("Muscat", "Asia/Muscat", "OM"),
    CityEntry("Amman", "Asia/Amman", "JO"),
    CityEntry("Beirut", "Asia/Beirut", "LB"),
    CityEntry("Damascus", "Asia/Damascus", "SY"),
    CityEntry("Jerusalem", "Asia/Jerusalem", "IL"),
    CityEntry("Tel Aviv", "Asia/Jerusalem", "IL"),
    CityEntry("Ankara", "Europe/Istanbul", "TR"),
    CityEntry("Istanbul", "Europe/Istanbul", "TR"),
    CityEntry("Nicosia", "Asia/Nicosia", "CY"),
    CityEntry("Singapore", "Asia/Singapore", "SG"),
    CityEntry("Kuala Lumpur", "Asia/Kuala_Lumpur", "MY"),
    CityEntry("Bangkok", "Asia/Bangkok", "TH"),
    CityEntry("Jakarta", "Asia/Jakarta", "ID"),
    CityEntry("Bali", "Asia/Makassar", "ID"),
    CityEntry("Manila", "Asia/Manila", "PH"),
    CityEntry("Ho Chi Minh City", "Asia/Ho_Chi_Minh", "VN"),
    CityEntry("Hanoi", "Asia/Ho_Chi_Minh", "VN"),
    CityEntry("Phnom Penh", "Asia/Phnom_Penh", "KH"),
    CityEntry("Vientiane", "Asia/Vientiane", "LA"),
    CityEntry("Yangon", "Asia/Yangon", "MM"),
    CityEntry("Male", "Indian/Maldives", "MV"),
    CityEntry("Thimphu", "Asia/Thimphu", "BT"),
    CityEntry("Ulaanbaatar", "Asia/Ulaanbaatar", "MN"),
    CityEntry("Beijing", "Asia/Shanghai", "CN"),
    CityEntry("Shanghai", "Asia/Shanghai", "CN"),
    CityEntry("Hong Kong", "Asia/Hong_Kong", "HK"),
    CityEntry("Taipei", "Asia/Taipei", "TW"),
    CityEntry("Seoul", "Asia/Seoul", "KR"),
    CityEntry("Tokyo", "Asia/Tokyo", "JP"),
    CityEntry("Osaka", "Asia/Tokyo", "JP"),

    // EUROPE
    CityEntry("London", "Europe/London", "GB"),
    CityEntry("Dublin", "Europe/Dublin", "IE"),
    CityEntry("Lisbon", "Europe/Lisbon", "PT"),
    CityEntry("Madrid", "Europe/Madrid", "ES"),
    CityEntry("Barcelona", "Europe/Madrid", "ES"),
    CityEntry("Paris", "Europe/Paris", "FR"),
    CityEntry("Brussels", "Europe/Brussels", "BE"),
    CityEntry("Amsterdam", "Europe/Amsterdam", "NL"),
    CityEntry("Luxembourg", "Europe/Luxembourg", "LU"),
    CityEntry("Zurich", "Europe/Zurich", "CH"),
    CityEntry("Geneva", "Europe/Zurich", "CH"),
    CityEntry("Bern", "Europe/Zurich", "CH"),
    CityEntry("Rome", "Europe/Rome", "IT"),
    CityEntry("Milan", "Europe/Rome", "IT"),
    CityEntry("Vatican City", "Europe/Vatican", "VA"),
    CityEntry("Vienna", "Europe/Vienna", "AT"),
    CityEntry("Berlin", "Europe/Berlin", "DE"),
    CityEntry("Munich", "Europe/Berlin", "DE"),
    CityEntry("Hamburg", "Europe/Berlin", "DE"),
    CityEntry("Frankfurt", "Europe/Berlin", "DE"),
    CityEntry("Prague", "Europe/Prague", "CZ"),
    CityEntry("Warsaw", "Europe/Warsaw", "PL"),
    CityEntry("Budapest", "Europe/Budapest", "HU"),
    CityEntry("Bratislava", "Europe/Bratislava", "SK"),
    CityEntry("Ljubljana", "Europe/Ljubljana", "SI"),
    CityEntry("Zagreb", "Europe/Zagreb", "HR"),
    CityEntry("Belgrade", "Europe/Belgrade", "RS"),
    CityEntry("Sarajevo", "Europe/Sarajevo", "BA"),
    CityEntry("Skopje", "Europe/Skopje", "MK"),
    CityEntry("Tirana", "Europe/Tirana", "AL"),
    CityEntry("Sofia", "Europe/Sofia", "BG"),
    CityEntry("Bucharest", "Europe/Bucharest", "RO"),
    CityEntry("Athens", "Europe/Athens", "GR"),
    CityEntry("Nicosia", "Asia/Nicosia", "CY"),
    CityEntry("Valletta", "Europe/Valletta", "MT"),
    CityEntry("Stockholm", "Europe/Stockholm", "SE"),
    CityEntry("Oslo", "Europe/Oslo", "NO"),
    CityEntry("Copenhagen", "Europe/Copenhagen", "DK"),
    CityEntry("Helsinki", "Europe/Helsinki", "FI"),
    CityEntry("Tallinn", "Europe/Tallinn", "EE"),
    CityEntry("Riga", "Europe/Riga", "LV"),
    CityEntry("Vilnius", "Europe/Vilnius", "LT"),
    CityEntry("Minsk", "Europe/Minsk", "BY"),
    CityEntry("Kyiv", "Europe/Kyiv", "UA"),
    CityEntry("Chisinau", "Europe/Chisinau", "MD"),
    CityEntry("Reykjavik", "Atlantic/Reykjavik", "IS"),
    CityEntry("Monaco", "Europe/Monaco", "MC"),
    CityEntry("Andorra", "Europe/Andorra", "AD"),
    CityEntry("San Marino", "Europe/San_Marino", "SM"),
    CityEntry("Podgorica", "Europe/Podgorica", "ME"),
    CityEntry("Moscow", "Europe/Moscow", "RU"),
    CityEntry("Saint Petersburg", "Europe/Moscow", "RU"),

    // AFRICA
    CityEntry("Cairo", "Africa/Cairo", "EG"),
    CityEntry("Alexandria", "Africa/Cairo", "EG"),
    CityEntry("Casablanca", "Africa/Casablanca", "MA"),
    CityEntry("Rabat", "Africa/Casablanca", "MA"),
    CityEntry("Tunis", "Africa/Tunis", "TN"),
    CityEntry("Algiers", "Africa/Algiers", "DZ"),
    CityEntry("Tripoli", "Africa/Tripoli", "LY"),
    CityEntry("Khartoum", "Africa/Khartoum", "SD"),
    CityEntry("Addis Ababa", "Africa/Addis_Ababa", "ET"),
    CityEntry("Nairobi", "Africa/Nairobi", "KE"),
    CityEntry("Kampala", "Africa/Kampala", "UG"),
    CityEntry("Dar es Salaam", "Africa/Dar_es_Salaam", "TZ"),
    CityEntry("Dakar", "Africa/Dakar", "SN"),
    CityEntry("Accra", "Africa/Accra", "GH"),
    CityEntry("Lagos", "Africa/Lagos", "NG"),
    CityEntry("Abuja", "Africa/Lagos", "NG"),
    CityEntry("Douala", "Africa/Douala", "CM"),
    CityEntry("Kinshasa", "Africa/Kinshasa", "CD"),
    CityEntry("Luanda", "Africa/Luanda", "AO"),
    CityEntry("Lusaka", "Africa/Lusaka", "ZM"),
    CityEntry("Harare", "Africa/Harare", "ZW"),
    CityEntry("Johannesburg", "Africa/Johannesburg", "ZA"),
    CityEntry("Cape Town", "Africa/Johannesburg", "ZA"),
    CityEntry("Durban", "Africa/Johannesburg", "ZA"),
    CityEntry("Pretoria", "Africa/Johannesburg", "ZA"),
    CityEntry("Maputo", "Africa/Maputo", "MZ"),
    CityEntry("Antananarivo", "Indian/Antananarivo", "MG"),
    CityEntry("Mauritius", "Indian/Mauritius", "MU"),
    CityEntry("Reunion", "Indian/Reunion", "RE"),
    CityEntry("Kigali", "Africa/Kigali", "RW"),
    CityEntry("Bujumbura", "Africa/Bujumbura", "BI"),
    CityEntry("Mogadishu", "Africa/Mogadishu", "SO"),
    CityEntry("Djibouti", "Africa/Djibouti", "DJ"),
    CityEntry("Asmara", "Africa/Asmara", "ER"),

    // AMERICAS
    CityEntry("New York", "America/New_York", "US"),
    CityEntry("Los Angeles", "America/Los_Angeles", "US"),
    CityEntry("Chicago", "America/Chicago", "US"),
    CityEntry("Houston", "America/Chicago", "US"),
    CityEntry("Phoenix", "America/Phoenix", "US"),
    CityEntry("Philadelphia", "America/New_York", "US"),
    CityEntry("San Antonio", "America/Chicago", "US"),
    CityEntry("San Diego", "America/Los_Angeles", "US"),
    CityEntry("Dallas", "America/Chicago", "US"),
    CityEntry("San Jose", "America/Los_Angeles", "US"),
    CityEntry("Austin", "America/Chicago", "US"),
    CityEntry("Jacksonville", "America/New_York", "US"),
    CityEntry("Miami", "America/New_York", "US"),
    CityEntry("Seattle", "America/Los_Angeles", "US"),
    CityEntry("Denver", "America/Denver", "US"),
    CityEntry("Boston", "America/New_York", "US"),
    CityEntry("Nashville", "America/Chicago", "US"),
    CityEntry("Las Vegas", "America/Los_Angeles", "US"),
    CityEntry("Portland", "America/Los_Angeles", "US"),
    CityEntry("Washington DC", "America/New_York", "US"),
    CityEntry("Toronto", "America/Toronto", "CA"),
    CityEntry("Montreal", "America/Toronto", "CA"),
    CityEntry("Vancouver", "America/Vancouver", "CA"),
    CityEntry("Calgary", "America/Calgary", "CA"),
    CityEntry("Ottawa", "America/Toronto", "CA"),
    CityEntry("Mexico City", "America/Mexico_City", "MX"),
    CityEntry("Guadalajara", "America/Mexico_City", "MX"),
    CityEntry("Monterrey", "America/Monterrey", "MX"),
    CityEntry("Guatemala City", "America/Guatemala", "GT"),
    CityEntry("San Salvador", "America/El_Salvador", "SV"),
    CityEntry("Tegucigalpa", "America/Tegucigalpa", "HN"),
    CityEntry("Managua", "America/Managua", "NI"),
    CityEntry("San Jose Costa Rica", "America/Costa_Rica", "CR"),
    CityEntry("Panama City", "America/Panama", "PA"),
    CityEntry("Havana", "America/Havana", "CU"),
    CityEntry("Kingston", "America/Jamaica", "JM"),
    CityEntry("Santo Domingo", "America/Santo_Domingo", "DO"),
    CityEntry("Port au Prince", "America/Port-au-Prince", "HT"),
    CityEntry("Bogota", "America/Bogota", "CO"),
    CityEntry("Caracas", "America/Caracas", "VE"),
    CityEntry("Lima", "America/Lima", "PE"),
    CityEntry("Quito", "America/Quito", "EC"),
    CityEntry("La Paz", "America/La_Paz", "BO"),
    CityEntry("Santiago", "America/Santiago", "CL"),
    CityEntry("Buenos Aires", "America/Argentina/Buenos_Aires", "AR"),
    CityEntry("Montevideo", "America/Montevideo", "UY"),
    CityEntry("Asuncion", "America/Asuncion", "PY"),
    CityEntry("Sao Paulo", "America/Sao_Paulo", "BR"),
    CityEntry("Rio de Janeiro", "America/Sao_Paulo", "BR"),
    CityEntry("Brasilia", "America/Sao_Paulo", "BR"),
    CityEntry("Manaus", "America/Manaus", "BR"),
    CityEntry("Recife", "America/Recife", "BR"),
    CityEntry("Fortaleza", "America/Fortaleza", "BR"),
    CityEntry("Georgetown", "America/Guyana", "GY"),
    CityEntry("Paramaribo", "America/Paramaribo", "SR"),
    CityEntry("Cayenne", "America/Cayenne", "GF"),

    // OCEANIA
    CityEntry("Sydney", "Australia/Sydney", "AU"),
    CityEntry("Melbourne", "Australia/Melbourne", "AU"),
    CityEntry("Brisbane", "Australia/Brisbane", "AU"),
    CityEntry("Perth", "Australia/Perth", "AU"),
    CityEntry("Adelaide", "Australia/Adelaide", "AU"),
    CityEntry("Auckland", "Pacific/Auckland", "NZ"),
    CityEntry("Wellington", "Pacific/Auckland", "NZ"),
    CityEntry("Christchurch", "Pacific/Auckland", "NZ"),
    CityEntry("Fiji", "Pacific/Fiji", "FJ"),
    CityEntry("Port Moresby", "Pacific/Port_Moresby", "PG"),
    CityEntry("Honolulu", "Pacific/Honolulu", "US"),
    CityEntry("Guam", "Pacific/Guam", "GU"),
    CityEntry("Suva", "Pacific/Fiji", "FJ"),
    CityEntry("Apia", "Pacific/Apia", "WS"),
    CityEntry("Nuku alofa", "Pacific/Tongatapu", "TO")
)

fun CityEntry.toAvailableCity(): com.example.ui.AvailableCity {
    val id = this.name.lowercase().replace(" ", "_").replace(",", "")
    val tz = TimeZone.getTimeZone(this.timezone)
    val rawOffset = tz.rawOffset
    val offsetHours = rawOffset / (1000 * 60 * 60)
    val sign = if (offsetHours >= 0) "+" else ""
    val offsetText = "$sign${offsetHours}HRS"
    return com.example.ui.AvailableCity(
        id = id,
        name = this.name,
        timezoneId = this.timezone,
        offsetText = offsetText
    )
}

fun getFlagEmoji(countryCode: String): String {
    if (countryCode.length != 2) return ""
    val firstChar = countryCode[0].uppercaseChar().code - 0x41 + 0x1F1E6
    val secondChar = countryCode[1].uppercaseChar().code - 0x41 + 0x1F1E6
    return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
}

fun getFlagEmojiForCity(cityName: String): String {
    val entry = allCities.find { it.name.equals(cityName, ignoreCase = true) }
    return if (entry != null) getFlagEmoji(entry.countryCode) else ""
}

private fun getRecentCities(context: Context): List<String> {
    val prefs = context.getSharedPreferences("world_clock_recents", Context.MODE_PRIVATE)
    val csv = prefs.getString("recent_city_names", "") ?: ""
    if (csv.isEmpty()) return emptyList()
    return csv.split(",")
}

private fun addRecentCity(context: Context, cityName: String) {
    val current = getRecentCities(context).toMutableList()
    current.remove(cityName)
    current.add(0, cityName)
    val limited = current.take(5)
    val csv = limited.joinToString(",")
    context.getSharedPreferences("world_clock_recents", Context.MODE_PRIVATE)
        .edit()
        .putString("recent_city_names", csv)
        .apply()
}

@Composable
fun CityRow(
    city: com.example.ui.AvailableCity,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(shape = RoundedCornerShape(16.dp))
            .clickable {
                android.util.Log.d("HAPTIC_TEST", "triggered")
                HapticManager.light(context)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                SoundHapticHelper.playSound269(context)
                onClick()
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val flag = getFlagEmojiForCity(city.name)
                if (flag.isNotEmpty()) {
                    Text(
                        text = flag,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(
                    text = city.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceLight
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Today, ${city.offsetText}",
                style = BodySm,
                color = OnSurfaceMuted
            )
        }

        val liveCityTime = getLocalTimeForTimeZone(city.timezoneId, context)
        val meridianToken = if (liveCityTime.contains("AM")) "AM" else if (liveCityTime.contains("PM")) "PM" else ""
        val timePart = liveCityTime.replace("AM", "").replace("PM", "").trim()

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = timePart,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurfaceLight
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = meridianToken,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnSurfaceMuted
            )
        }
    }
}

@Composable
fun ChooseCityOverlay(
    viewModel: ClockViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val searchQuery by viewModel.searchQuery.collectAsState()

    val popularNames = setOf(
        "New York",
        "London",
        "Tokyo",
        "Paris",
        "Sydney",
        "Dubai",
        "Los Angeles",
        "Hong Kong",
        "Cairo"
    )

    var recentCityNames by remember {
        mutableStateOf(getRecentCities(context))
    }

    val filteredRecentCities = remember(recentCityNames, searchQuery) {
        val recents = recentCityNames.mapNotNull { name ->
            allCities.find { it.name.equals(name, ignoreCase = true) }?.toAvailableCity()
        }
        if (searchQuery.isBlank()) {
            recents
        } else {
            recents.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredPopularCities = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            viewModel.availableCities
        } else {
            viewModel.availableCities.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredAllCities = remember(searchQuery) {
        val nonPopularSorted = allCities.filter { it.name !in popularNames }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        if (searchQuery.isBlank()) {
            nonPopularSorted.map { it.toAvailableCity() }
        } else {
            nonPopularSorted.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }.map { it.toAvailableCity() }
        }
    }

    val onCitySelected: (com.example.ui.AvailableCity) -> Unit = { city ->
        addRecentCity(context, city.name)
        recentCityNames = getRecentCities(context)
        viewModel.addWorldClock(city)
        viewModel.showChooseCity(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .padding(top = 40.dp)
        ) {
            // Choose a City top group
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Choose a City",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )

                Text(
                    text = "Cancel",
                    style = BodyLg,
                    color = OnSurfaceMuted,
                    modifier = Modifier.clickable {
                        android.util.Log.d("HAPTIC_TEST", "triggered")
                        HapticManager.heavy(context)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundHapticHelper.playSound269(context)
                        onDismiss()
                    }
                )
            }

            // Glass styled Input Box with exact magnifier search parameters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(shape = RoundedCornerShape(20.dp), bgColor = Color.White.copy(alpha = 0.04f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = OnSurfaceMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                // Custom seamless query holder
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search for a city or airport",
                            color = OnSurfaceMuted.copy(alpha = 0.5f),
                            style = BodySm
                        )
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        textStyle = BodySm.copy(color = OnSurfaceLight),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = OnSurfaceMuted,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                HapticManager.light(context)
                                viewModel.setSearchQuery("")
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Scroll listing alongside Alphabet selection bar on the right side
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                // Dynamic City list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredRecentCities.isNotEmpty()) {
                        item {
                            Text(
                                text = "RECENT CITIES",
                                style = LabelCaps,
                                color = OnSurfaceMuted.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        items(filteredRecentCities) { city ->
                            CityRow(city = city, onClick = { onCitySelected(city) })
                        }
                    }

                    if (filteredPopularCities.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(if (filteredRecentCities.isNotEmpty()) 16.dp else 0.dp))
                            Text(
                                text = "POPULAR CITIES",
                                style = LabelCaps,
                                color = OnSurfaceMuted.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        items(filteredPopularCities) { city ->
                            CityRow(city = city, onClick = { onCitySelected(city) })
                        }
                    }

                    if (filteredAllCities.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "ALL CITIES",
                                style = LabelCaps,
                                color = OnSurfaceMuted.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        items(filteredAllCities) { city ->
                            CityRow(city = city, onClick = { onCitySelected(city) })
                        }
                    }
                }
            }
        }
    }
}

// Helpers
private fun getLocalTimeForTimeZone(timezoneId: String, context: Context): String {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone(timezoneId))
    val minute = calendar.get(Calendar.MINUTE)
    val minStr = String.format("%02d", minute)
    
    val prefs = context.getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
    val is24Hour = prefs.getBoolean("is_24_hour_format", false)
    
    if (is24Hour) {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val hourStr = String.format("%02d", hour)
        return "$hourStr:$minStr"
    } else {
        val hour = calendar.get(Calendar.HOUR)
        val amPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
        val hourStr = if (hour == 0) "12" else hour.toString()
        return "$hourStr:$minStr $amPm"
    }
}

private fun getDayAndOffset(timezoneId: String, offsetText: String): String {
    val localCalendar = Calendar.getInstance()
    val targetCalendar = Calendar.getInstance(TimeZone.getTimeZone(timezoneId))

    val localDay = localCalendar.get(Calendar.DAY_OF_YEAR)
    val targetDay = targetCalendar.get(Calendar.DAY_OF_YEAR)
    val relativeDay = when {
        targetDay > localDay -> "Tomorrow"
        targetDay < localDay -> "Yesterday"
        else -> "Today"
    }
    return "$relativeDay, $offsetText"
}

private fun getOffsetString(offsetHours: Int): String {
    val sign = if (offsetHours >= 0) "+" else ""
    return "$sign${offsetHours}HRS"
}

@Composable
fun SwipeToRevealItem(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    swipeEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val maxRevealWidth = 80.dp
    val maxRevealPx = with(density) { -maxRevealWidth.toPx() }
    val thresholdPx = with(density) { -50.dp.toPx() }

    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var hapticTriggered by remember { mutableStateOf(false) }
    var isThresholdMet by remember { mutableStateOf(false) }

    val scaleAnim = remember { Animatable(0f) }

    LaunchedEffect(isThresholdMet) {
        if (isThresholdMet) {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else {
            scaleAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(swipeEnabled) {
                if (swipeEnabled) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val newOffset = (offsetX.value + dragAmount).coerceIn(maxRevealPx, 0f)
                                offsetX.snapTo(newOffset)

                                val isMet = newOffset <= thresholdPx
                                isThresholdMet = isMet
                                if (isMet) {
                                    if (!hapticTriggered) {
                                        HapticManager.light(context.applicationContext)
                                        hapticTriggered = true
                                    }
                                } else {
                                    hapticTriggered = false
                                }
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value < thresholdPx) {
                                    offsetX.animateTo(
                                        targetValue = maxRevealPx,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                    hapticTriggered = false
                                } else {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                    hapticTriggered = false
                                    isThresholdMet = false
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                                hapticTriggered = false
                                isThresholdMet = false
                            }
                        }
                    )
                }
            }
    ) {
        // Subtle hint/indicator that fades in as drag starts
        val instructionAlpha = if (offsetX.value < 0f && scaleAnim.value < 1f) {
            (offsetX.value / thresholdPx).coerceIn(0f, 1f) * (1f - scaleAnim.value)
        } else {
            0f
        }

        if (instructionAlpha > 0f) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(end = 24.dp)
                    .alpha(instructionAlpha),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = OnSurfaceMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Swipe left to delete",
                    style = BodySm,
                    color = OnSurfaceMuted.copy(alpha = 0.5f)
                )
            }
        }

        if (scaleAnim.value > 0f) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .alpha(scaleAnim.value),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(48.dp)
                        .scale(scaleAnim.value)
                        .clip(CircleShape)
                        .glassCard(shape = CircleShape)
                        .align(Alignment.CenterVertically)
                        .clickable {
                            android.util.Log.d("HAPTIC_TEST", "triggered")
                            HapticManager.heavy(context)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundHapticHelper.playSound269(context)
                            onDelete()
                            scope.launch {
                                offsetX.animateTo(0f, tween(100))
                                hapticTriggered = false
                                isThresholdMet = false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
        ) {
            content()
        }
    }
}
