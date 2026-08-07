package com.nhakhoaquangninh.telesales

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.nhakhoaquangninh.telesales.theme.BackgroundLight
import com.nhakhoaquangninh.telesales.theme.Dimens
import com.nhakhoaquangninh.telesales.theme.ErrorCrimson
import com.nhakhoaquangninh.telesales.theme.OnSurfaceDark
import com.nhakhoaquangninh.telesales.theme.OnSurfaceVariant
import com.nhakhoaquangninh.telesales.theme.OutlineVariant
import com.nhakhoaquangninh.telesales.theme.PrimaryTeal
import com.nhakhoaquangninh.telesales.theme.SurfaceContainer
import com.nhakhoaquangninh.telesales.theme.SurfaceContainerHighest
import com.nhakhoaquangninh.telesales.theme.TertiaryContainer
import com.nhakhoaquangninh.telesales.theme.TertiaryFixedDim

/**
 * WarningActivity — Màn hình cảnh báo vi phạm toàn màn hình với tính răn đe cao.
 *
 * Kích hoạt KHI VÀ CHỈ KHI nhân viên tắt tính năng ghi âm cuộc gọi.
 */
class WarningActivity : ComponentActivity() {

    companion object {
        private const val TAG = "WarningActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Chặn nút Back hoàn toàn (Cách làm mới chuẩn Jetpack) ─────────
        onBackPressedDispatcher.addCallback(this) {
            Log.w(TAG, "⚠️ Nhân viên cố thoát bằng Back — bị chặn hoàn toàn.")
        }

        // ── Hiển thị đè lên màn hình khoá + giữ màn hình sáng ──────────
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // ── Đếm số lần vi phạm trong ngày ─────────────────────────────
        val prefs = getSharedPreferences("TelesalesCompliance", MODE_PRIVATE)
        val violationCount = prefs.getInt("violation_count", 0) + 1
        prefs.edit().putInt("violation_count", violationCount).apply()

        // ── Phát âm thanh cảnh báo & Rung khẩn cấp ────────────────────
        triggerAlarmEffects()

        setContent {
            MaterialTheme {
                WarningScreen(
                    violationCount = violationCount,
                    onOpenSettings = { openPhoneCallSettings() },
                    onConfirm = {
                        Log.d(TAG, "✅ Nhân viên xác nhận đã sửa xong vi phạm.")
                        finish()
                    }
                )
            }
        }

        makeFullscreen()

        Log.w(TAG, "🚨 WarningActivity đã kích hoạt! Số lần vi phạm: $violationCount")
    }

    private fun triggerAlarmEffects() {
        try {
            val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneG.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1200)

            val effect = VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(effect)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi hiệu ứng cảnh báo")
        }
    }

    private fun openPhoneCallSettings() {
        try {
            val intent = Intent("android.telecom.action.SHOW_CALL_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e2: Exception) {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        }
    }

    private fun makeFullscreen() {
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi chuyển sang toàn màn hình")
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) makeFullscreen()
    }
}

@Composable
fun WarningScreen(
    violationCount: Int,
    onOpenSettings: () -> Unit,
    onConfirm: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(Dimens.PaddingMedium),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = Dimens.WarningMaxWidth),
            shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
            border = BorderStroke(Dimens.BorderThickness, OutlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationMedium)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.PaddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .background(TertiaryContainer.copy(alpha = 0.2f), shape = CircleShape)
                        .padding(Dimens.PaddingMedium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = TertiaryFixedDim,
                        modifier = Modifier.size(Dimens.IconSizeWarning)
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.PaddingMedium))

                Text(
                    text = stringResource(R.string.warning_title),
                    color = ErrorCrimson,
                    fontSize = Dimens.FontSize28,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = Dimens.FontSize36
                )

                Spacer(modifier = Modifier.height(Dimens.PaddingSmall))

                Text(
                    text = stringResource(R.string.warning_message),
                    color = OnSurfaceVariant,
                    fontSize = Dimens.FontSize16,
                    textAlign = TextAlign.Center,
                    lineHeight = Dimens.FontSize24
                )

                if (violationCount > 0) {
                    Spacer(modifier = Modifier.height(Dimens.PaddingSmall))
                    Text(
                        text = stringResource(R.string.warning_violation_count, violationCount),
                        color = ErrorCrimson,
                        fontSize = Dimens.FontSize14,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.PaddingMedium))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.CornerRadiusSmall))
                        .background(BackgroundLight)
                        .padding(Dimens.PaddingMedium)
                ) {
                    Text(
                        text = stringResource(R.string.warning_instruction_title),
                        fontWeight = FontWeight.Medium,
                        fontSize = Dimens.FontSize18,
                        color = OnSurfaceDark,
                        modifier = Modifier.padding(bottom = Dimens.Space12)
                    )
                    HorizontalDivider(
                        color = SurfaceContainerHighest,
                        thickness = Dimens.DividerThickness
                    )

                    Spacer(modifier = Modifier.height(Dimens.Space12))

                    Text(
                        text = stringResource(R.string.warning_instruction_body),
                        color = OnSurfaceVariant,
                        fontSize = Dimens.FontSize14,
                        lineHeight = Dimens.FontSize20
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.PaddingLarge))

                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.Size20),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(Dimens.Space8))
                    Text(
                        text = stringResource(R.string.warning_btn_open_settings),
                        fontSize = Dimens.FontSize14,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.PaddingSmall))

                OutlinedButton(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryTeal),
                    border = BorderStroke(Dimens.BorderThickness, PrimaryTeal),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = stringResource(R.string.warning_btn_confirm),
                        fontSize = Dimens.FontSize14
                    )
                }
            }
        }
    }
}
