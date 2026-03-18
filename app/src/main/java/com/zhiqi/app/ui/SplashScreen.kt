package com.zhiqi.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zhiqi.app.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var start by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (start) 1f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (start) 1f else 0.92f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        start = true
        delay(1200)
        onFinished()
    }

    GlassBackground {
        Column(
            modifier = Modifier.fillMaxSize().graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_splash_art),
                contentDescription = "启动插画",
                modifier = Modifier.size(196.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text("知期", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = ZhiQiTokens.TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("像写手帐一样，轻轻记录今天", style = MaterialTheme.typography.bodyLarge, color = ZhiQiTokens.TextSecondary)
        }
    }
}
