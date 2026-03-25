package com.zhiqi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.zhiqi.app.ui.designsystem.AppRadii
import com.zhiqi.app.ui.designsystem.AppSpacing

@Composable
fun GlassBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFE1D2),
                        Color(0xFFFFF0D9),
                        Color(0xFFE6F0E8)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.92f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ZhiQiTokens.PrimarySoft.copy(alpha = 0.92f),
                            Color(0xFFFFD7C5).copy(alpha = 0.74f),
                            ZhiQiTokens.SecondarySoft.copy(alpha = 0.38f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(120f, 140f),
                        radius = 860f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.88f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ZhiQiTokens.TertiarySoft.copy(alpha = 0.86f),
                            Color(0xFFD7E6D9).copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(920f, 1540f),
                        radius = 1180f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = AppSpacing.page, vertical = AppSpacing.small)
        ) {
            content()
        }
    }
}

fun Modifier.glassCard(): Modifier {
    val shape = RoundedCornerShape(AppRadii.large)
    return this
        .shadow(
            elevation = 24.dp,
            shape = shape,
            ambientColor = ZhiQiTokens.Primary.copy(alpha = 0.12f),
            spotColor = ZhiQiTokens.Secondary.copy(alpha = 0.16f)
        )
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.92f),
                    ZhiQiTokens.Surface.copy(alpha = 0.84f),
                    Color(0xFFFFF6EE).copy(alpha = 0.8f)
                )
            )
        )
        .border(1.dp, Color.White.copy(alpha = 0.72f), shape)
}

fun Modifier.glassPanel(
    shape: Shape = RoundedCornerShape(AppRadii.medium),
    backgroundAlpha: Float = 0.78f,
    borderAlpha: Float = 0.82f
): Modifier {
    return this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = (backgroundAlpha + 0.08f).coerceAtMost(0.98f)),
                    ZhiQiTokens.Surface.copy(alpha = backgroundAlpha)
                )
            ),
            shape
        )
        .border(1.dp, Color.White.copy(alpha = borderAlpha), shape)
}

@Composable
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}

@Composable
fun SectionTitle(text: String) {
    androidx.compose.material3.Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}
