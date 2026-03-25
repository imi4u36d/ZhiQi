package com.zhiqi.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.SoftwareKeyboardController
import com.zhiqi.app.R
import com.zhiqi.app.security.PinManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UnlockScreen(
    pinManager: PinManager,
    onUnlocked: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var shaking by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        requestKeyboardFocus(focusManager, focusRequester, keyboardController, scope)
    }

    fun handleCompletePin(input: String) {
        // 首次进入如果还没有 PIN，这里直接把输入写成新密码；否则走校验流程。
        if (!pinManager.isPinSet()) {
            pinManager.setPin(input)
            onUnlocked()
            return
        }

        val ok = pinManager.verifyPin(input)
        if (ok) {
            onUnlocked()
        } else {
            error = "密码错误"
            shaking = true
            pin = ""
            scope.launch {
                delay(240)
                shaking = false
            }
        }
    }

    GlassBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_splash_art),
                contentDescription = "解锁插画",
                modifier = Modifier.height(104.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text("隐私保护", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (pinManager.isPinSet()) "请输入4位密码" else "设置4位密码",
                color = ZhiQiTokens.TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            BasicTextField(
                value = pin,
                onValueChange = { value ->
                    val filtered = value.filter { it.isDigit() }.take(4)
                    pin = filtered
                    error = null
                    if (filtered.length == 4) {
                        handleCompletePin(filtered)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                cursorBrush = SolidColor(Color.Transparent),
                modifier = Modifier
                    .size(1.dp)
                    .focusRequester(focusRequester)
            )

            val shakeOffset = if (shaking) 6.dp else 0.dp
            Row(
                modifier = Modifier
                    .padding(start = shakeOffset)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        requestKeyboardFocus(focusManager, focusRequester, keyboardController, scope)
                    },
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(4) { index ->
                    val filled = index < pin.length
                    val active = index == pin.length.coerceAtMost(3)
                    Box(
                        modifier = Modifier
                            .size(width = 54.dp, height = 62.dp)
                            .background(
                                color = if (filled) ZhiQiTokens.PrimarySoft else ZhiQiTokens.SurfaceSoft,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .border(
                                width = if (active) 1.5.dp else 1.dp,
                                color = if (active) ZhiQiTokens.Primary else ZhiQiTokens.Border,
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (filled) "•" else "",
                            style = MaterialTheme.typography.headlineSmall,
                            color = ZhiQiTokens.Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            if (error != null) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("输入完成后自动解锁", color = ZhiQiTokens.TextMuted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun requestKeyboardFocus(
    focusManager: FocusManager,
    focusRequester: FocusRequester,
    keyboardController: SoftwareKeyboardController?,
    scope: kotlinx.coroutines.CoroutineScope
) {
    scope.launch {
        focusManager.clearFocus(force = true)
        delay(40)
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}
