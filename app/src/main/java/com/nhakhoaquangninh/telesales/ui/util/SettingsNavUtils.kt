package com.nhakhoaquangninh.telesales.ui.util

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri

object SettingsNavUtils {

    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Exception) {
            }
        }
    }

    fun openNotificationSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                openAppSettings(context)
            }
        } catch (_: Exception) {
            openAppSettings(context)
        }
    }

    @SuppressLint("BatteryLife")
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {
                openAppSettings(context)
            }
        }
    }

    fun openCallRecordingSettings(context: Context) {
        val oemCallRecordingIntents = listOf(
            // Xiaomi / MIUI / HyperOS (Trang cài đặt ghi âm cuộc gọi)
            Intent().setComponent(ComponentName("com.android.phone", "com.android.phone.settings.CallRecordSetting")),
            Intent().setComponent(ComponentName("com.android.phone", "com.android.phone.CallFeaturesSetting")),
            Intent().setComponent(ComponentName("com.android.server.telecom", "com.android.server.telecom.settings.CallRecordSetting")),

            // Samsung (Cài đặt cuộc gọi & ghi âm cuộc gọi)
            Intent("android.telecom.action.SHOW_CALL_SETTINGS"),
            Intent().setComponent(ComponentName("com.samsung.android.incallui", "com.samsung.android.incallui.setting.CallSettingActivity")),
            Intent().setComponent(ComponentName("com.samsung.android.dialer", "com.samsung.android.dialer.app.dialer.DialtactsActivity")),

            // Oppo / Realme / ColorOS
            Intent().setComponent(ComponentName("com.android.phone", "com.android.phone.RecordSetting")),
            Intent().setComponent(ComponentName("com.coloros.phonenoarea", "com.coloros.phonenoarea.RecordSetting")),
            Intent().setComponent(ComponentName("com.oppo.phone", "com.oppo.phone.RecordSetting")),

            // Vivo / FuntouchOS / OriginOS
            Intent().setComponent(ComponentName("com.android.phone", "com.android.phone.VivoCallRecordSetting")),
            Intent().setComponent(ComponentName("com.vivo.phonenoarea", "com.vivo.phonenoarea.RecordSetting")),

            // Huawei / Honor
            Intent().setComponent(ComponentName("com.android.phone", "com.android.phone.MSimCallFeaturesSetting")),

            // Standard Android Telecom Call Settings
            Intent("android.telecom.action.SHOW_CALL_SETTINGS"),

            // Fallback: Ứng dụng Điện thoại (Dialer)
            Intent(Intent.ACTION_DIAL)
        )

        for (intent in oemCallRecordingIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {
            }
        }

        // Cuối cùng fallback về App Settings
        openAppSettings(context)
    }

    fun openAutostartSettings(context: Context) {
        val oemIntents = listOf(
            // Xiaomi / MIUI / HyperOS
            Intent().setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            ),
            // Huawei / Honor
            Intent().setComponent(
                ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
                )
            ),
            // Oppo / Realme
            Intent().setComponent(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.oppo.safe",
                    "com.oppo.safe.permission.startup.StartupAppListActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.startupapp.StartupAppListActivity"
                )
            ),
            // Vivo
            Intent().setComponent(
                ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            ),
            // Samsung
            Intent().setComponent(
                ComponentName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                )
            )
        )

        for (intent in oemIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {
            }
        }

        // Fallback to App Settings
        openAppSettings(context)
    }
}
