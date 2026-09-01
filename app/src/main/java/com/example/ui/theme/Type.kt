package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Define the custom typography styles
val DisplayTimer = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Light,
    fontSize = 88.sp,
    lineHeight = 100.sp,
    letterSpacing = (-2).sp
)

val DisplayTimerMobile = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Light,
    fontSize = 72.sp,
    lineHeight = 80.sp,
    letterSpacing = (-2).sp
)

val HeadlineMd = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 24.sp
)

val BodyLg = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 17.sp,
    lineHeight = 22.sp
)

val BodySm = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 20.sp
)

val LabelCaps = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 1.sp
)

val ButtonText = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Medium,
    fontSize = 17.sp,
    lineHeight = 22.sp
)

// Standard Material 3 Typography overloads
val Typography = Typography(
    bodyLarge = BodyLg,
    bodySmall = BodySm,
    headlineMedium = HeadlineMd,
    labelLarge = LabelCaps,
    displayLarge = DisplayTimer,
    displayMedium = DisplayTimerMobile
)
