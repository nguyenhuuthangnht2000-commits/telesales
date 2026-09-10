package com.nhakhoaquangninh.telesales.call

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.CancellationSignal
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.annotation.MainThread

private enum class LocationCaptureOutcome {
    CACHED, CURRENT, PERMISSION_MISSING, LOCATION_DISABLED, NO_PROVIDER,
    TIMEOUT, NO_VALID_FIX, PROVIDER_ERROR
}

/** One bounded location request per ended call; never starts continuous tracking. */
class CallLocationProvider(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(LocationManager::class.java)
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission") // Runtime grants are checked before accessing LocationManager.
    @MainThread
    fun capture(sessionId: Long, onResult: (Location?) -> Unit) {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MILLIS
        val requests = mutableListOf<CancellationSignal>()
        var finished = false
        lateinit var timeout: Runnable
        fun finish(location: Location?, outcome: LocationCaptureOutcome) {
            if (finished) return
            finished = true
            handler.removeCallbacks(timeout)
            requests.forEach { runCatching { it.cancel() } }
            val permissionAvailable = granted(Manifest.permission.ACCESS_FINE_LOCATION) ||
                granted(Manifest.permission.ACCESS_COARSE_LOCATION)
            val locationEnabled = runCatching {
                manager != null && LocationManagerCompat.isLocationEnabled(manager)
            }.getOrDefault(false)
            val finalOutcome = when {
                !permissionAvailable -> LocationCaptureOutcome.PERMISSION_MISSING
                manager == null -> LocationCaptureOutcome.NO_PROVIDER
                !locationEnabled -> LocationCaptureOutcome.LOCATION_DISABLED
                SystemClock.elapsedRealtime() >= deadline -> LocationCaptureOutcome.TIMEOUT
                location != null && !isUsable(location) -> LocationCaptureOutcome.NO_VALID_FIX
                else -> outcome
            }
            val acceptedLocation = location.takeIf {
                finalOutcome == LocationCaptureOutcome.CACHED || finalOutcome == LocationCaptureOutcome.CURRENT
            }
            // Keep caller/enqueue exceptions outside the location-provider exception handler.
            handler.post {
                com.nhakhoaquangninh.telesales.core.FileLogger.logLocal(appContext, "API_LOG", "location_capture session_id=$sessionId outcome=${finalOutcome.name.lowercase()} location_available=${acceptedLocation != null}")
                onResult(acceptedLocation)
            }
        }
        timeout = Runnable { finish(null, LocationCaptureOutcome.TIMEOUT) }
        handler.postAtTime(timeout, SystemClock.uptimeMillis() + TIMEOUT_MILLIS)
        try {
            val fine = granted(Manifest.permission.ACCESS_FINE_LOCATION)
            val coarse = granted(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (!fine && !coarse) {
                finish(null, LocationCaptureOutcome.PERMISSION_MISSING)
                return
            }
            if (manager == null) {
                finish(null, LocationCaptureOutcome.NO_PROVIDER)
                return
            }
            if (!LocationManagerCompat.isLocationEnabled(manager)) {
                finish(null, LocationCaptureOutcome.LOCATION_DISABLED)
                return
            }
            val providers = listOfNotNull(
                LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER.takeIf { fine }
            ).filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
            val cached = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
                .filter(::isUsable).maxByOrNull { it.elapsedRealtimeNanos }
            if (SystemClock.elapsedRealtime() >= deadline) {
                finish(null, LocationCaptureOutcome.TIMEOUT)
                return
            }
            if (cached != null) {
                finish(cached, LocationCaptureOutcome.CACHED)
                return
            }
            if (providers.isEmpty()) {
                finish(null, LocationCaptureOutcome.NO_PROVIDER)
                return
            }
            var remaining = providers.size
            providers.forEach { provider ->
                if (finished) return@forEach
                val cancellation = CancellationSignal()
                requests.add(cancellation)
                LocationManagerCompat.getCurrentLocation(
                    manager, provider, cancellation, ContextCompat.getMainExecutor(appContext)
                ) { location ->
                    if (SystemClock.elapsedRealtime() >= deadline) {
                        finish(null, LocationCaptureOutcome.TIMEOUT)
                    } else if (location != null && isUsable(location)) {
                        finish(location, LocationCaptureOutcome.CURRENT)
                    } else {
                        remaining--
                        if (remaining == 0) finish(null, LocationCaptureOutcome.NO_VALID_FIX)
                    }
                }
            }
        } catch (_: RuntimeException) {
            // Revoked permissions, unavailable providers and OEM errors must not block uploads.
            finish(null, LocationCaptureOutcome.PROVIDER_ERROR)
        }
    }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED

    private fun isUsable(location: Location): Boolean {
        val age = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
        return age in 0..MAX_AGE_NANOS && location.latitude.isFinite() &&
            location.longitude.isFinite() && location.latitude in -90.0..90.0 &&
            location.longitude in -180.0..180.0
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
        const val MAX_AGE_NANOS = 60_000_000_000L
    }
}
