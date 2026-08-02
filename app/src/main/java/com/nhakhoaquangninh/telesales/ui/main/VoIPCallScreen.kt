package com.nhakhoaquangninh.telesales.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nhakhoaquangninh.telesales.StringeeManager

@Composable
fun VoIPCallScreen() {
    val callState by StringeeManager.callState.collectAsStateWithLifecycle()
    val isConnected by StringeeManager.isConnected.collectAsStateWithLifecycle()
    var phoneNumber by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1117), Color(0xFF1A1F2E))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
        ) {
            // ── Trạng thái kết nối ────────────────────────────────────────
            ConnectionStatusBadge(isConnected)

            Spacer(modifier = Modifier.height(16.dp))

            // ── Avatar + Trạng thái cuộc gọi ─────────────────────────────
            CallAvatar(callState)

            // ── Thông tin trạng thái ──────────────────────────────────────
            CallStatusText(callState)

            Spacer(modifier = Modifier.height(8.dp))

            // ── Nhập số điện thoại (chỉ hiện khi Idle) ───────────────────
            if (callState is StringeeManager.CallState.Idle ||
                callState is StringeeManager.CallState.Ended ||
                callState is StringeeManager.CallState.Error
            ) {
                PhoneNumberInput(
                    value = phoneNumber,
                    onChange = { phoneNumber = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Nút hành động ─────────────────────────────────────────────
            CallActionButton(
                callState = callState,
                isConnected = isConnected,
                phoneNumber = phoneNumber,
                onCall = {
                    if (phoneNumber.isNotBlank()) {
                        StringeeManager.makeCall(phoneNumber.trim())
                    }
                },
                onHangUp = { StringeeManager.hangUp() }
            )
        }
    }
}

@Composable
private fun ConnectionStatusBadge(isConnected: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isConnected) Color(0xFF00E676) else Color(0xFFFF5252))
        )
        Text(
            text = if (isConnected) "Đã kết nối Stringee" else "Đang kết nối...",
            color = if (isConnected) Color(0xFF00E676) else Color(0xFFFFB300),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CallAvatar(callState: StringeeManager.CallState) {
    // Hiệu ứng pulse khi đang gọi
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val isActive = callState is StringeeManager.CallState.Active ||
            callState is StringeeManager.CallState.Ringing

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(120.dp)
            .scale(if (isActive) scale else 1f)
            .clip(CircleShape)
            .background(
                when (callState) {
                    is StringeeManager.CallState.Active -> Color(0xFF00897B)
                    is StringeeManager.CallState.Ringing -> Color(0xFF1565C0)
                    is StringeeManager.CallState.Error -> Color(0xFFC62828)
                    else -> Color(0xFF2C3E50)
                }
            )
    ) {
        Text(
            text = when (callState) {
                is StringeeManager.CallState.Active -> "📞"
                is StringeeManager.CallState.Ringing -> "📲"
                is StringeeManager.CallState.Ended -> "📵"
                is StringeeManager.CallState.Error -> "⚠️"
                else -> "☎️"
            },
            fontSize = 48.sp
        )
    }
}

@Composable
private fun CallStatusText(callState: StringeeManager.CallState) {
    AnimatedContent(targetState = callState, label = "status") { state ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when (state) {
                    is StringeeManager.CallState.Idle -> "Sẵn sàng gọi"
                    is StringeeManager.CallState.Connecting -> "Đang kết nối..."
                    is StringeeManager.CallState.Ringing -> state.toNumber
                    is StringeeManager.CallState.Active -> state.toNumber
                    is StringeeManager.CallState.Ended -> state.reason
                    is StringeeManager.CallState.Error -> state.message
                },
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (state is StringeeManager.CallState.Ringing) {
                Text(
                    text = "Đang đổ chuông...",
                    color = Color(0xFFB0BEC5),
                    fontSize = 14.sp
                )
            }
            if (state is StringeeManager.CallState.Active) {
                Text(
                    text = "🔴 Đang ghi âm 2 chiều",
                    color = Color(0xFF00E676),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PhoneNumberInput(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("Số điện thoại khách hàng") },
        placeholder = { Text("Ví dụ: 0987654321") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00BCD4),
            unfocusedBorderColor = Color(0xFF37474F),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color(0xFF00BCD4),
            focusedLabelColor = Color(0xFF00BCD4),
            unfocusedLabelColor = Color(0xFF78909C)
        )
    )
}

@Composable
private fun CallActionButton(
    callState: StringeeManager.CallState,
    isConnected: Boolean,
    phoneNumber: String,
    onCall: () -> Unit,
    onHangUp: () -> Unit
) {
    val isInCall = callState is StringeeManager.CallState.Ringing ||
            callState is StringeeManager.CallState.Active ||
            callState is StringeeManager.CallState.Connecting

    Button(
        onClick = { if (isInCall) onHangUp() else onCall() },
        enabled = isConnected && (isInCall || phoneNumber.isNotBlank()),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isInCall) Color(0xFFE53935) else Color(0xFF00897B),
            disabledContainerColor = Color(0xFF37474F)
        )
    ) {
        Text(
            text = if (isInCall) "📵  Cúp máy" else "📞  Gọi ngay",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
