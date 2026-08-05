# Home Dashboard & Bottom Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Xây dựng màn hình Home Dashboard chính cho ứng dụng Telesales App (Nha Khoa Quảng Ninh) theo mẫu thiết kế `code.html` và tích hợp cấu trúc Bottom Navigation Bar (3 tab: Trang chủ, Lịch sử, Cài đặt).

**Architecture:** `MainScreen` đóng vai trò Scaffold container chính quản lý Bottom Navigation Bar. Tab "Trang chủ" chứa `HomeScreenContent` (Bật/tắt Foreground Service, Thống kê Bento Grid, Kiểm tra quyền Android). Tab "Lịch sử" chứa danh sách file ghi âm và trình phát Audio cũ.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Android Foreground Service, WorkManager.

## Global Constraints

- **Design Tokens**: `PrimaryTeal` (`#005C55`), `PrimaryContainer` (`#0F766E`), `ActiveEmerald` (`#10B981`), `WarningAmber` (`#D97706`), `BackgroundLight` (`#F9F9F9`).
- **Language**: 100% Tiếng Việt (Rule 6).

---

### Task 1: Tạo Component `HomeScreenContent.kt` (Màn hình Home Dashboard)

**Files:**
- Create: `app/src/main/java/com/nhakhoaquangninh/telesales/ui/main/components/HomeScreenContent.kt`

**Interfaces:**
- Consumes: `TelesalesForegroundService`, `Context`, `Resource`
- Produces: `@Composable fun HomeScreenContent(isServiceRunning: Boolean, onToggleService: (Boolean) -> Unit, onSyncNowClick: () -> Unit, onFixPermissionClick: () -> Unit)`

- [ ] **Step 1: Định nghĩa file `HomeScreenContent.kt` với các component Service Control, Bento Grid, Permissions Checklist**

```kotlin
package com.nhakhoaquangninh.telesales.ui.main.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PrimaryTeal = Color(0xFF005C55)
private val PrimaryContainer = Color(0xFF0F766E)
private val OnPrimaryContainer = Color(0xFFA3FAEF)
private val SurfaceContainer = Color(0xFFEEEEEE)
private val SurfaceContainerLow = Color(0xFFF3F3F3)
private val SurfaceLowest = Color(0xFFFFFFFF)
private val ActiveEmerald = Color(0xFF10B981)
private val WarningAmber = Color(0xFFD97706)
private val OnSurfaceDark = Color(0xFF1A1C1C)
private val OnSurfaceVariant = Color(0xFF3E4947)
private val OutlineVariant = Color(0xFFBDC9C6)

@Composable
fun HomeScreenContent(
    isServiceRunning: Boolean,
    totalCallsToday: Int,
    syncedCalls: Int,
    pendingCalls: Int,
    hasRecordAudioPerm: Boolean,
    hasCallLogPerm: Boolean,
    isBatteryOptimized: Boolean,
    onToggleService: (Boolean) -> Unit,
    onSyncNowClick: () -> Unit,
    onFixBatteryOptClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. Service Control Card ──────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
            border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Dịch vụ Ghi âm Cuộc gọi",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OnSurfaceDark
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (isServiceRunning) ActiveEmerald.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (isServiceRunning) ActiveEmerald.copy(alpha = 0.4f) else Color.Gray)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (isServiceRunning) ActiveEmerald else Color.Gray, shape = CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isServiceRunning) "HOẠT ĐỘNG" else "ĐÃ TẮT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isServiceRunning) PrimaryContainer else Color.Gray
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tự động ghi âm cuộc gọi GSM ngầm và lưu lịch sử cuộc gọi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }

                Switch(
                    checked = isServiceRunning,
                    onCheckedChange = onToggleService,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryTeal,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = OutlineVariant
                    )
                )
            }
        }

        // ── 2. Quick Metrics Bento Grid ──────────────────────────────────
        Text(
            text = "Thống kê hôm nay",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = PrimaryTeal
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Metric 1: Tổng cuộc gọi
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tổng cuộc gọi", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                        Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "$totalCallsToday", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = OnSurfaceDark)
                }
            }

            // Metric 2: Đã đồng bộ
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Đã đồng bộ", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = PrimaryContainer, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "$syncedCalls", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = OnSurfaceDark)
                }
            }

            // Metric 3: Chờ đồng bộ
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Chờ tải lên", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                        Icon(Icons.Default.PendingActions, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "$pendingCalls", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = OnSurfaceDark)
                        Text(
                            text = "Đồng bộ",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryTeal,
                            modifier = Modifier.clickable { onSyncNowClick() }
                        )
                    }
                }
            }
        }

        // ── 3. System Permissions Checklist ──────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
            border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Trạng thái Quyền Hệ thống",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnSurfaceDark
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                PermissionRow(
                    title = "Ghi âm cuộc gọi (RECORD_AUDIO)",
                    isGranted = hasRecordAudioPerm
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = OutlineVariant.copy(alpha = 0.2f))
                PermissionRow(
                    title = "Nhật ký cuộc gọi (READ_CALL_LOG)",
                    isGranted = hasCallLogPerm
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = OutlineVariant.copy(alpha = 0.2f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (!isBatteryOptimized) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (!isBatteryOptimized) ActiveEmerald else WarningAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Tối ưu hóa Pin (Battery Opt)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = OnSurfaceDark)
                            Text(
                                text = if (!isBatteryOptimized) "Đã bỏ qua (Tốt)" else "Cần tắt tối ưu pin để không bị dừng ngầm",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                    if (isBatteryOptimized) {
                        TextButton(onClick = onFixBatteryOptClick) {
                            Text("Khắc phục", color = PrimaryTeal, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── 4. Compliance Note Card ──────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = OnPrimaryContainer, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Quy định Bảo mật & Tuân thủ",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = OnPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Vui lòng tuân thủ quy trình giao tiếp với khách hàng của Nha Khoa Quảng Ninh. File ghi âm được mã hóa bảo mật khi đồng bộ lên Server.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnPrimaryContainer.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(title: String, isGranted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) ActiveEmerald else WarningAmber,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = OnSurfaceDark)
        }
        Text(
            text = if (isGranted) "Đã cấp" else "Chưa cấp",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isGranted) ActiveEmerald else WarningAmber
        )
    }
}
```

- [ ] **Step 2: Tạo tệp `HomeScreenContent.kt` trong dự án**

---

### Task 2: Cập nhật `MainScreen.kt` với Bottom Navigation Bar & Tab Management

**Files:**
- Modify: `app/src/main/java/com/nhakhoaquangninh/telesales/ui/main/MainScreen.kt`

**Interfaces:**
- Consumes: `HomeScreenContent`, `AudioFileItem`, `MediaPlayer`
- Produces: `MainScreen` với 3 Tab: `Home`, `History`, `Settings`

- [ ] **Step 1: Cập nhật `MainScreen.kt` để quản lý tab index (`0: Home`, `1: History`, `2: Settings`) và gắn Navigation BottomBar**

```kotlin
// Thêm NavigationBar & NavigationBarItem trong MainScreen.kt:
NavigationBar(
    containerColor = Color(0xFFEEEEEE)
) {
    NavigationBarItem(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        icon = { Icon(Icons.Default.Home, contentDescription = "Trang chủ") },
        label = { Text("Trang chủ") },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color(0xFF005C55),
            selectedTextColor = Color(0xFF005C55),
            indicatorColor = Color(0xFF86F2E4)
        )
    )
    NavigationBarItem(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        icon = { Icon(Icons.Default.History, contentDescription = "Lịch sử") },
        label = { Text("Lịch sử") },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color(0xFF005C55),
            selectedTextColor = Color(0xFF005C55),
            indicatorColor = Color(0xFF86F2E4)
        )
    )
    NavigationBarItem(
        selected = selectedTab == 2,
        onClick = { selectedTab = 2 },
        icon = { Icon(Icons.Default.Settings, contentDescription = "Cài đặt") },
        label = { Text("Cài đặt") },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color(0xFF005C55),
            selectedTextColor = Color(0xFF005C55),
            indicatorColor = Color(0xFF86F2E4)
        )
    )
}
```

- [ ] **Step 2: Cập nhật logic `MainScreen.kt`**

---

### Task 3: Biên dịch & Kiểm định kết quả (Verification)

- [ ] **Step 1: Chạy lệnh Gradle build**
`$env:JAVA_HOME="C:\Users\thangnh7\.jdks\jbr-17.0.14"; ./gradlew assembleDebug`
- [ ] **Step 2: Xác nhận BUILD SUCCESSFUL**
