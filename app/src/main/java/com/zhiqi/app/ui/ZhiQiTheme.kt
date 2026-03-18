package com.zhiqi.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

object ZhiQiTokens {
    val Primary = Color(0xFFF07FA5)
    val PrimaryStrong = Color(0xFFE35D8F)
    val PrimarySoft = Color(0xFFFFE3EC)
    val AccentSoft = Color(0xFFFFF7D8)
    val AccentStrongerSoft = Color(0xFFDFF7F1)

    val TextPrimary = Color(0xFF1E293B)
    val TextSecondary = Color(0xFF64748B)
    val TextMuted = Color(0xFF94A3B8)

    val BackgroundTop = Color(0xFFFFF8FB)
    val BackgroundBottom = Color(0xFFF5F7FF)

    val Surface = Color(0xFFFFFFFF)
    val SurfaceSoft = Color(0xFFF8FAFC)
    val Border = Color(0xFFE2E8F0)
    val BorderStrong = Color(0xFFD7DFEA)

    val Danger = Color(0xFFEF6F7E)
}

private val LightColors = lightColorScheme(
    primary = ZhiQiTokens.Primary,
    onPrimary = Color.White,
    primaryContainer = ZhiQiTokens.PrimarySoft,
    onPrimaryContainer = ZhiQiTokens.PrimaryStrong,
    secondary = ZhiQiTokens.PrimaryStrong,
    onSecondary = Color.White,
    secondaryContainer = ZhiQiTokens.AccentSoft,
    onSecondaryContainer = ZhiQiTokens.TextPrimary,
    tertiary = Color(0xFF14B8A6),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDBF7F1),
    onTertiaryContainer = Color(0xFF0F766E),
    error = ZhiQiTokens.Danger,
    onError = Color.White,
    background = ZhiQiTokens.BackgroundTop,
    onBackground = ZhiQiTokens.TextPrimary,
    surface = ZhiQiTokens.Surface,
    onSurface = ZhiQiTokens.TextPrimary,
    surfaceVariant = ZhiQiTokens.SurfaceSoft,
    onSurfaceVariant = ZhiQiTokens.TextSecondary,
    outline = ZhiQiTokens.BorderStrong,
    outlineVariant = ZhiQiTokens.Border
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 29.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 29.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(30.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(38.dp)
)

private fun TextStyle.scaleBy(scale: Float): TextStyle {
    val scaledFontSize = if (fontSize.isSpecified) (fontSize.value * scale).sp else fontSize
    val scaledLineHeight = if (lineHeight.isSpecified) (lineHeight.value * scale).sp else lineHeight
    return copy(fontSize = scaledFontSize, lineHeight = scaledLineHeight)
}

private fun Typography.scaleBy(scale: Float): Typography {
    return copy(
        displayLarge = displayLarge.scaleBy(scale),
        displayMedium = displayMedium.scaleBy(scale),
        displaySmall = displaySmall.scaleBy(scale),
        headlineLarge = headlineLarge.scaleBy(scale),
        headlineMedium = headlineMedium.scaleBy(scale),
        headlineSmall = headlineSmall.scaleBy(scale),
        titleLarge = titleLarge.scaleBy(scale),
        titleMedium = titleMedium.scaleBy(scale),
        titleSmall = titleSmall.scaleBy(scale),
        bodyLarge = bodyLarge.scaleBy(scale),
        bodyMedium = bodyMedium.scaleBy(scale),
        bodySmall = bodySmall.scaleBy(scale),
        labelLarge = labelLarge.scaleBy(scale),
        labelMedium = labelMedium.scaleBy(scale),
        labelSmall = labelSmall.scaleBy(scale)
    )
}

@Composable
private fun rememberAdaptiveTypography(): Typography {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val scale = when {
        screenWidthDp <= 320 -> 0.88f
        screenWidthDp <= 360 -> 0.93f
        screenWidthDp <= 411 -> 0.98f
        screenWidthDp <= 480 -> 1f
        else -> 1.04f
    }
    return remember(scale) { AppTypography.scaleBy(scale) }
}

@Composable
fun ZhiQiTheme(content: @Composable () -> Unit) {
    val adaptiveTypography = rememberAdaptiveTypography()
    MaterialTheme(
        colorScheme = LightColors,
        typography = adaptiveTypography,
        shapes = AppShapes,
        content = content
    )
}
