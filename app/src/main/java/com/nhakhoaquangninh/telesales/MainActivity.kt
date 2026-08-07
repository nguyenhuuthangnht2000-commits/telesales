package com.nhakhoaquangninh.telesales

import android.Manifest
import android.content.Intent
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

class MainActivity : BaseActivity() {

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
                showToast("Đã cấp đủ quyền. Khởi chạy Foreground Service...")
                checkOverlayPermissionAndStartService()
            } else {
                showToast(
                    "Vui lòng cấp đủ các quyền đọc trạng thái cuộc gọi & bộ nhớ để ứng dụng hoạt động!",
                    isLong = true
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())

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
        checkOverlayPermissionAndStartService()
    }

    private fun checkOverlayPermissionAndStartService() {
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

        startTelesalesService()
    }

    private fun startTelesalesService() {
        val serviceIntent = Intent(this, TelesalesForegroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}
