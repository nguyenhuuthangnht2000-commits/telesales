package com.nhakhoaquangninh.telesales

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.nhakhoaquangninh.telesales.core.BaseActivity
import com.nhakhoaquangninh.telesales.data.local.TokenManager
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
                showToast(getString(R.string.perm_all_granted_starting_service))
                startTelesalesServiceIfAllowed()
            } else {
                showToast(
                    getString(R.string.perm_missing_warning),
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

        val needsRuntimePermission = permissionsToRequest.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needsRuntimePermission) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            startTelesalesServiceIfAllowed()
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
        startTelesalesServiceIfAllowed()
    }

    private fun startTelesalesServiceIfAllowed() {
        val tokenManager = TokenManager.getInstance(this)
        val hasPhoneState = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        val hasCallLog = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        // Chỉ tự động khởi chạy service nếu đã đăng nhập, monitoring enabled và đã được cấp các quyền cần thiết
        if (tokenManager.isLoggedIn() && tokenManager.isMonitoringEnabled() && hasPhoneState && hasCallLog) {
            val serviceIntent = Intent(this, TelesalesForegroundService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }
}
