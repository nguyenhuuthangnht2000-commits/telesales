package com.nhakhoaquangninh.telesales.ui.components

import android.Manifest
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nhakhoaquangninh.telesales.R
import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.Resource

private enum class LocationSetupStatus { READY, FOREGROUND_PERMISSION, BACKGROUND_PERMISSION, LOCATION_DISABLED }

/** Optional setup: refusing location never blocks call monitoring or upload. */
@Composable
fun LocationPermissionHost(
    existingPermissionsComplete: Boolean,
    content: @Composable (needsSetup: Boolean, onRequestSetup: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferences = remember(context) {
        context.applicationContext.getSharedPreferences("location_permission_setup", Context.MODE_PRIVATE)
    }
    fun granted(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    fun readStatus(): LocationSetupStatus = when {
        !granted(Manifest.permission.ACCESS_COARSE_LOCATION) && !granted(Manifest.permission.ACCESS_FINE_LOCATION) -> LocationSetupStatus.FOREGROUND_PERMISSION
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION) -> LocationSetupStatus.BACKGROUND_PERMISSION
        runCatching { context.getSystemService(LocationManager::class.java)?.let { LocationManagerCompat.isLocationEnabled(it) } }.getOrNull() != true -> LocationSetupStatus.LOCATION_DISABLED
        else -> LocationSetupStatus.READY
    }
    var settingsError by remember { mutableStateOf<Resource.Error?>(null) }
    var status by remember { mutableStateOf(readStatus()) }
    var showExplanation by rememberSaveable { mutableStateOf(false) }
    var requestedForeground by rememberSaveable { mutableStateOf(preferences.getBoolean("foreground_requested", false)) }
    var requestedBackground by rememberSaveable { mutableStateOf(preferences.getBoolean("background_requested", false)) }
    var requestInFlight by rememberSaveable { mutableStateOf(false) }
    var activityResumed by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    fun launchSafely(action: () -> Unit) {
        try {
            action()
        } catch (_: RuntimeException) {
            requestInFlight = false
            settingsError = Resource.Error(
                message = context.getString(R.string.location_settings_failed),
                source = ErrorSource.APP_CLIENT
            )
        }
    }
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        requestInFlight = false
        status = readStatus()
    }
    val foregroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        requestInFlight = false
        status = readStatus()
        if (status == LocationSetupStatus.BACKGROUND_PERMISSION) showExplanation = true
    }
    val backgroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        requestInFlight = false
        status = readStatus()
    }
    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            activityResumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (event == Lifecycle.Event.ON_RESUME) status = readStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                status = readStatus()
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(LocationManager.MODE_CHANGED_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            context.unregisterReceiver(receiver)
        }
    }
    LaunchedEffect(existingPermissionsComplete, activityResumed) {
        if (existingPermissionsComplete && activityResumed &&
            !preferences.getBoolean("introduction_shown_v1", false)) {
            preferences.edit().putBoolean("introduction_shown_v1", true).apply()
            status = readStatus()
            if (status != LocationSetupStatus.READY && !requestInFlight) showExplanation = true
        }
    }
    content(status != LocationSetupStatus.READY) {
        if (existingPermissionsComplete && activityResumed && !requestInFlight) {
            status = readStatus()
            showExplanation = status != LocationSetupStatus.READY
        }
    }
    settingsError?.let { error -> ErrorDialog(error = error, onDismiss = { settingsError = null }) }
    if (showExplanation && existingPermissionsComplete && activityResumed && !requestInFlight && status != LocationSetupStatus.READY) {
        AlertDialog(
            onDismissRequest = { showExplanation = false },
            title = { Text(stringResource(R.string.location_title)) },
            text = {
                Text(stringResource(when (status) {
                    LocationSetupStatus.BACKGROUND_PERMISSION -> R.string.location_background_explanation
                    LocationSetupStatus.LOCATION_DISABLED -> R.string.location_disabled_explanation
                    else -> R.string.location_foreground_explanation
                }), modifier = Modifier.verticalScroll(rememberScrollState()))
            },
            confirmButton = {
                Button(onClick = {
                    showExplanation = false
                    launchSafely {
                        requestInFlight = true
                        when {
                            status == LocationSetupStatus.FOREGROUND_PERMISSION && !requestedForeground -> {
                                requestedForeground = true
                                preferences.edit().putBoolean("foreground_requested", true).apply()
                                foregroundLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                            }
                            status == LocationSetupStatus.BACKGROUND_PERMISSION && Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && !requestedBackground -> {
                                requestedBackground = true
                                preferences.edit().putBoolean("background_requested", true).apply()
                                backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            }
                            else -> settingsLauncher.launch(
                                if (status == LocationSetupStatus.LOCATION_DISABLED) Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
                            )
                        }
                    }
                }) { Text(stringResource(R.string.location_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { showExplanation = false }) { Text(stringResource(R.string.location_later)) }
            }
        )
    }
}
