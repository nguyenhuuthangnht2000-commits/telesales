package com.nhakhoaquangninh.telesales

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback

/**
 * WarningActivity — Màn hình cảnh báo vi phạm toàn màn hình với tính răn đe cao.
 *
 * Kích hoạt KHI VÀ CHỈ KHI nhân viên tắt tính năng ghi âm cuộc gọi.
 * 
 * Tính năng tăng cường răn đe:
 *  1. Phát chuông cảnh báo + rung khẩn cấp.
 *  2. Đếm và hiển thị số lần vi phạm trong ngày.
 *  3. Chặn nút Back hoàn toàn.
 *  4. Có nút bấm "⚙️ Mở Cài Đặt Điện Thoại" đưa thẳng nhân viên tới nơi bật lại ghi âm.
 *  5. Không làm gián đoạn luồng làm việc nếu nhân viên ĐÃ BẬT ghi âm tuân thủ.
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // ── Đếm số lần vi phạm trong ngày ─────────────────────────────
        val prefs = getSharedPreferences("TelesalesCompliance", MODE_PRIVATE)
        val violationCount = prefs.getInt("violation_count", 0) + 1
        prefs.edit().putInt("violation_count", violationCount).apply()

        // ── Phát âm thanh cảnh báo & Rung khẩn cấp ────────────────────
        triggerAlarmEffects()

        setContentView(buildUI(violationCount))
        makeFullscreen()

        Log.w(TAG, "🚨 WarningActivity đã kích hoạt! Số lần vi phạm: $violationCount")
    }

    private fun triggerAlarmEffects() {
        try {
            // Chuông báo động
            val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneG.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1200)

            // Rung khẩn cấp
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(1000)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi hiệu ứng âm thanh/rung cảnh báo: ${e.message}")
        }
    }

    private fun buildUI(violationCount: Int): View {
        // ── Root container ────────────────────────────────────────────────
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.setBackgroundColor(Color.parseColor("#880E4F")) // Deep Red / Crimson 900
        root.setPadding(dp(24), dp(50), dp(24), dp(40))

        // ── Icon cảnh báo vi phạm ──────────────────────────────────────────
        val iconView = TextView(this)
        iconView.text = "🚨"
        iconView.textSize = 72f
        iconView.gravity = Gravity.CENTER
        val iconLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        iconLp.bottomMargin = dp(12)
        root.addView(iconView, iconLp)

        // ── Tiêu đề RĂN ĐE ────────────────────────────────────────────────
        val titleView = TextView(this)
        titleView.text = getString(R.string.warning_title)
        titleView.textSize = 30f
        titleView.setTextColor(Color.WHITE)
        titleView.gravity = Gravity.CENTER
        titleView.typeface = Typeface.DEFAULT_BOLD
        val titleLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        titleLp.bottomMargin = dp(8)
        root.addView(titleView, titleLp)

        // ── Đếm số lần vi phạm ────────────────────────────────────────────
        val badgeBg = GradientDrawable()
        badgeBg.setColor(Color.parseColor("#D50000")) // Bright Red
        badgeBg.cornerRadius = dp(20).toFloat()

        val violationBadge = TextView(this)
        violationBadge.text = getString(R.string.warning_violation_count, violationCount)
        violationBadge.textSize = 14f
        violationBadge.setTextColor(Color.WHITE)
        violationBadge.typeface = Typeface.DEFAULT_BOLD
        violationBadge.gravity = Gravity.CENTER
        violationBadge.background = badgeBg
        violationBadge.setPadding(dp(16), dp(8), dp(16), dp(8))
        val badgeLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        badgeLp.bottomMargin = dp(24)
        root.addView(violationBadge, badgeLp)

        // ── Nội dung thông báo vi phạm ─────────────────────────────────────
        val messageView = TextView(this)
        messageView.text = getString(R.string.warning_message)
        messageView.textSize = 18f
        messageView.setTextColor(Color.WHITE)
        messageView.gravity = Gravity.CENTER
        messageView.setLineSpacing(0f, 1.3f)
        val messageLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        messageLp.bottomMargin = dp(28)
        root.addView(messageView, messageLp)

        // ── Hộp hướng dẫn xử lý ──────────────────────────────────────────
        val instructionBg = GradientDrawable()
        instructionBg.setColor(Color.parseColor("#4A148C")) // Deep Purple Accent
        instructionBg.cornerRadius = dp(12).toFloat()

        val instructionBox = LinearLayout(this)
        instructionBox.orientation = LinearLayout.VERTICAL
        instructionBox.setPadding(dp(20), dp(16), dp(20), dp(16))
        instructionBox.background = instructionBg

        val instructionText = TextView(this)
        instructionText.text = "${getString(R.string.warning_instruction_title)}\n\n${getString(R.string.warning_instruction_body)}"
        instructionText.textSize = 14f
        instructionText.setTextColor(Color.parseColor("#E1BEE7"))
        instructionText.setLineSpacing(0f, 1.3f)
        instructionBox.addView(instructionText)

        val boxLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        boxLp.bottomMargin = dp(32)
        root.addView(instructionBox, boxLp)

        // ── Spacer đẩy nút xuống cuối ─────────────────────────────────────
        val spacer = View(this)
        root.addView(spacer, LinearLayout.LayoutParams(0, 0, 1f))

        // ── Nút 1: Mở Cài Đặt Điện Thoại Trực Tiếp ─────────────────────────
        val openSettingsBg = GradientDrawable()
        openSettingsBg.setColor(Color.parseColor("#FFD600")) // Yellow Accent
        openSettingsBg.cornerRadius = dp(30).toFloat()

        val openSettingsBtn = Button(this)
        openSettingsBtn.text = getString(R.string.warning_btn_open_settings)
        openSettingsBtn.textSize = 16f
        openSettingsBtn.typeface = Typeface.DEFAULT_BOLD
        openSettingsBtn.setTextColor(Color.BLACK)
        openSettingsBtn.background = openSettingsBg
        openSettingsBtn.setPadding(dp(24), dp(14), dp(24), dp(14))
        openSettingsBtn.setOnClickListener {
            openPhoneCallSettings()
        }

        val settingsBtnLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        settingsBtnLp.bottomMargin = dp(12)
        root.addView(openSettingsBtn, settingsBtnLp)

        // ── Nút 2: Tôi đã sửa xong ─────────────────────────────────────────
        val confirmBtn = Button(this)
        confirmBtn.text = getString(R.string.warning_btn_confirm)
        confirmBtn.textSize = 14f
        confirmBtn.typeface = Typeface.DEFAULT_BOLD
        confirmBtn.setTextColor(Color.WHITE)
        confirmBtn.background = GradientDrawable().apply {
            setColor(Color.parseColor("#B71C1C"))
            cornerRadius = dp(30).toFloat()
            setStroke(dp(2), Color.WHITE)
        }
        confirmBtn.setPadding(dp(20), dp(12), dp(20), dp(12))
        confirmBtn.setOnClickListener {
            Log.d(TAG, "✅ Nhân viên xác nhận đã sửa xong vi phạm.")
            finish()
        }

        val confirmBtnLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        root.addView(confirmBtn, confirmBtnLp)

        return root
    }

    private fun openPhoneCallSettings() {
        try {
            // Mở thẳng cài đặt ứng dụng Điện thoại / Cuộc gọi
            val intent = Intent("android.telecom.action.SHOW_CALL_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                // Fallback: Mở giao diện quay số Điện thoại
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e2: Exception) {
                // Fallback: Mở Cài đặt hệ thống chung
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        }
    }

    private fun makeFullscreen() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val controller = window.decorView.windowInsetsController ?: window.insetsController
                controller?.let { ctrl ->
                    ctrl.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    ctrl.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi makeFullscreen: ${e.message}")
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) makeFullscreen()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
