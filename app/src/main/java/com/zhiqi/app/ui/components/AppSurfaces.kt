package com.zhiqi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zhiqi.app.ui.ZhiQiTokens
import com.zhiqi.app.ui.glassCard
import com.zhiqi.app.ui.glassPanel
import com.zhiqi.app.ui.noRippleClickable
import com.zhiqi.app.ui.designsystem.AppRadii
import com.zhiqi.app.ui.designsystem.AppSpacing

@Composable
fun AppSurfaceCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = AppSpacing.large, vertical = AppSpacing.large),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(AppSpacing.medium),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassCard()
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}

@Composable
fun AppPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppRadii.medium),
    backgroundAlpha: Float = 0.74f,
    borderAlpha: Float = 0.82f,
    contentPadding: PaddingValues = PaddingValues(horizontal = AppSpacing.medium, vertical = AppSpacing.medium),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(AppSpacing.small),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .glassPanel(shape = shape, backgroundAlpha = backgroundAlpha, borderAlpha = borderAlpha)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content
    )
}

@Composable
fun AppBadge(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = ZhiQiTokens.PrimarySoft,
    textColor: Color = ZhiQiTokens.PrimaryStrong
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 28.dp)
            .glassPanel(shape = RoundedCornerShape(AppRadii.small), backgroundAlpha = 0.92f, borderAlpha = 0.84f)
            .background(background, RoundedCornerShape(AppRadii.small))
            .padding(horizontal = AppSpacing.small, vertical = AppSpacing.xSmall),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun AppIconBadge(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    shape: Shape = RoundedCornerShape(AppRadii.small)
) {
    Box(
        modifier = modifier
            .size(size)
            .glassPanel(shape = shape, backgroundAlpha = 0.88f, borderAlpha = 0.84f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.44f)
        )
    }
}

@Composable
fun AppArrowAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(AppRadii.medium))
            .background(
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(
                        ZhiQiTokens.PrimarySoft.copy(alpha = 0.95f),
                        ZhiQiTokens.SecondarySoft.copy(alpha = 0.92f)
                    )
                )
            )
            .glassPanel(shape = RoundedCornerShape(AppRadii.medium), backgroundAlpha = 0.58f, borderAlpha = 0.7f)
            .noRippleClickable(onClick)
            .padding(horizontal = AppSpacing.medium, vertical = AppSpacing.medium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = ZhiQiTokens.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = label,
            tint = ZhiQiTokens.PrimaryStrong,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun AppQuickActionTile(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .glassPanel(
                shape = RoundedCornerShape(AppRadii.medium),
                backgroundAlpha = if (highlighted) 0.9f else 0.46f,
                borderAlpha = if (highlighted) 0.88f else 0.72f
            )
            .noRippleClickable(onClick)
            .padding(horizontal = AppSpacing.small, vertical = AppSpacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlighted) iconTint else ZhiQiTokens.TextSecondary,
            fontWeight = FontWeight.Bold
        )
    }
}
