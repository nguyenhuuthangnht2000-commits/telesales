package com.nhakhoaquangninh.telesales

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.nhakhoaquangninh.telesales.core.BaseActivity
import com.nhakhoaquangninh.telesales.theme.TelesalesAppTheme
import com.nhakhoaquangninh.telesales.ui.util.SettingsNavUtils

class MainActivity : BaseActivity() {

    private var hasPromptedCallRecording = false

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val readPhoneStateGranted = permissions[Manifest.permission.READ_PHONE_STATE] ?: false
            val readCallLogGranted = permissions[Manifest.permission.READ_CALL_LOG] ?: false

            var postNotificationsGranted = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                postNotificationsGranted =
                    permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
            }

            var storageGranted = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                storageGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] ?: false
            } else {
                storageGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
            }

            if (readPhoneStateGranted && readCallLogGranted && postNotificationsGranted && storageGranted) {
                showToast(getString(R.string.perm_all_granted_starting_service))
                checkNextPermissionsAndNavigate()
            } else {
                showToast(
                    "Vui lòng cấp đủ các quyền đọc trạng thái cuộc gọi, thông báo & bộ nhớ để ứng dụng hoạt động!",
                    isLong = true
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        hasPromptedCallRecording = prefs.getBoolean(KEY_CALL_RECORDING_PROMPTED, false)

        val permissionsToRequest = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_NUMBERS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val needsRuntimePermission = permissionsToRequest.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needsRuntimePermission) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }

        setContent {
            TelesalesAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) { MainNavigation() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkNextPermissionsAndNavigate()
    }

    private fun checkNextPermissionsAndNavigate() {
        // 1. Kiểm tra quyền Overlay (Vẽ trên ứng dụng khác)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            showToast(
                "Vui lòng bật quyền 'Vẽ trên ứng dụng khác' để cảnh báo hoạt động!",
                isLong = true
            )
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }

        // 2. Sau khi đã cấp các quyền runtime & thông báo, điều hướng sang Bật Ghi Âm Cuộc Gọi (lần đầu khởi chạy)
        if (!hasPromptedCallRecording) {
            hasPromptedCallRecording = true
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_CALL_RECORDING_PROMPTED, true)
                .apply()

            showToast(
                getString(R.string.prompt_enable_call_recording),
                isLong = true
            )
            SettingsNavUtils.openCallRecordingSettings(this)
            return
        }

        // 3. Đã hoàn tất các bước phân quyền & thiết lập -> Khởi chạy Foreground Service
        startTelesalesService()
    }

    private fun startTelesalesService() {
        val serviceIntent = Intent(this, TelesalesForegroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    companion object {
        private const val PREFS_NAME = "telesales_app_prefs"
        private const val KEY_CALL_RECORDING_PROMPTED = "key_call_recording_prompted"
    }
}
