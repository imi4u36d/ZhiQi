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
import com.zhiqi.app.ui.designsystem.AppRadii

object ZhiQiTokens {
    // Sunset garden palette
    val Primary = Color(0xFFE86D72)
    val PrimaryStrong = Color(0xFFD4525F)
    val PrimarySoft = Color(0xFFFFE5E3)

    val Secondary = Color(0xFFCF9731)
    val SecondarySoft = Color(0xFFF6E3B7)

    val Tertiary = Color(0xFF607D90)
    val TertiarySoft = Color(0xFFDDE9EE)

    // Backward-compatible aliases used across existing screens
    val AccentSoft = SecondarySoft
    val AccentStrongerSoft = TertiarySoft

    val TextPrimary = Color(0xFF233042)
    val TextSecondary = Color(0xFF667A8A)
    val TextMuted = Color(0xFFA2AFBA)

    val Background = Color(0xFFFDF1E3)
    val Surface = Color(0xFFFFFBF6)
    val SurfaceSoft = Color(0xFFF8EFE4)

    val Border = Color(0xFFE9D6C0)
    val BorderStrong = Color(0xFFD9C0A5)

    val Danger = Color(0xFFD96A74)

    // Phase Colors
    val PhaseMenstrual = Color(0xFFE8949E)
    val PhaseFollicular = Color(0xFF9CB9D2)
    val PhaseFertile = Secondary
    val PhaseLuteal = Color(0xFFD6C1A0)
}

private val LightColors = lightColorScheme(
    primary = ZhiQiTokens.Primary,
    onPrimary = Color.White,
    primaryContainer = ZhiQiTokens.PrimarySoft,
    onPrimaryContainer = ZhiQiTokens.PrimaryStrong,
    secondary = ZhiQiTokens.Secondary,
    onSecondary = Color.White,
    secondaryContainer = ZhiQiTokens.SecondarySoft,
    onSecondaryContainer = ZhiQiTokens.TextPrimary,
    tertiary = ZhiQiTokens.Tertiary,
    onTertiary = Color.White,
    tertiaryContainer = ZhiQiTokens.TertiarySoft,
    onTertiaryContainer = Color(0xFF5D7A9A),
    error = ZhiQiTokens.Danger,
    onError = Color.White,
    background = ZhiQiTokens.Background,
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
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(AppRadii.small),
    small = androidx.compose.foundation.shape.RoundedCornerShape(AppRadii.small),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(AppRadii.medium),
    large = androidx.compose.foundation.shape.RoundedCornerShape(AppRadii.large),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(AppRadii.sheet)
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
