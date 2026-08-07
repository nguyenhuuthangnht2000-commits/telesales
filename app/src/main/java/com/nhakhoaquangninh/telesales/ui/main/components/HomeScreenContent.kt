package com.nhakhoaquangninh.telesales.ui.main.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.nhakhoaquangninh.telesales.R
import com.nhakhoaquangninh.telesales.theme.ActiveEmerald
import com.nhakhoaquangninh.telesales.theme.Dimens
import com.nhakhoaquangninh.telesales.theme.OnPrimaryContainer
import com.nhakhoaquangninh.telesales.theme.OnSurfaceDark
import com.nhakhoaquangninh.telesales.theme.OnSurfaceVariant
import com.nhakhoaquangninh.telesales.theme.OutlineVariant
import com.nhakhoaquangninh.telesales.theme.PrimaryContainer
import com.nhakhoaquangninh.telesales.theme.PrimaryTeal
import com.nhakhoaquangninh.telesales.theme.SurfaceContainer
import com.nhakhoaquangninh.telesales.theme.SurfaceLowest
import com.nhakhoaquangninh.telesales.theme.WarningAmber

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
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(Dimens.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
    ) {
        // ── 1. Service Control Card ──────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
            border = BorderStroke(Dimens.BorderThickness, OutlineVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.PaddingMedium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.service_title),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = Dimens.PaddingMedium),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnSurfaceDark
                    )
                    Surface(
                        shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
                        color = if (isServiceRunning) ActiveEmerald.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                        border = BorderStroke(Dimens.BorderThickness, if (isServiceRunning) ActiveEmerald.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Dimens.PaddingSmall, vertical = Dimens.PaddingExtraSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(Dimens.Space6)
                                    .background(
                                        if (isServiceRunning) ActiveEmerald else Color.Gray,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(Dimens.PaddingExtraSmall))
                            Text(
                                text = if (isServiceRunning) stringResource(R.string.service_active) else stringResource(R.string.service_inactive),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isServiceRunning) PrimaryContainer else Color.Gray
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Dimens.CornerRadiusMedium))
                Text(
                    text = stringResource(R.string.service_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = onToggleService,
                        thumbContent = {
                            if (isServiceRunning) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(Dimens.IconSizeSmall),
                                    tint = PrimaryTeal
                                )
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryTeal,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = OutlineVariant
                        )
                    )
                }
            }
        }

        // ── 2. Quick Metrics Stack ──────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
        ) {
            // Metric 1: Tổng cuộc gọi
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
                colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
                border = BorderStroke(Dimens.BorderThickness, OutlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(Dimens.PaddingMedium)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.total_calls), style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(
                            Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(Dimens.Size18)
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.PaddingSmall))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = "$totalCallsToday", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), color = OnSurfaceDark)
                        Spacer(modifier = Modifier.width(Dimens.PaddingSmall))
                        Text(
                            text = stringResource(R.string.home_growth_placeholder),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ActiveEmerald,
                            modifier = Modifier.padding(bottom = Dimens.Space4)
                        )
                    }
                }
            }

            // Metric 2: Đã đồng bộ
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
                colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
                border = BorderStroke(Dimens.BorderThickness, OutlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(Dimens.PaddingMedium)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.synced_calls), style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(
                            Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = PrimaryContainer,
                            modifier = Modifier.size(Dimens.Size18)
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.PaddingSmall))
                    Text(text = "$syncedCalls", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), color = OnSurfaceDark)
                }
            }

            // Metric 3: Chờ đồng bộ
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
                colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
                border = BorderStroke(Dimens.BorderThickness, OutlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(Dimens.PaddingMedium)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.pending_calls), style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(
                            Icons.Default.PendingActions,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(Dimens.Size18)
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.PaddingSmall))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "$pendingCalls", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), color = OnSurfaceDark)
                        Text(
                            text = stringResource(R.string.sync_now),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryTeal,
                            modifier = Modifier
                                .clickable { onSyncNowClick() }
                                .padding(vertical = Dimens.Space4, horizontal = Dimens.Space8)
                        )
                    }
                }
            }
        }

        // ── 3. Recent Calls Card ──────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
            colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
            border = BorderStroke(Dimens.BorderThickness, OutlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.PaddingMedium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.recent_calls),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnSurfaceDark
                    )
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = stringResource(R.string.home_filter_desc),
                        tint = OnSurfaceVariant
                    )
                }
                HorizontalDivider(color = OutlineVariant.copy(alpha = 0.2f))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.PaddingLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.IconSizeExtraLarge),
                        tint = OnSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
                    Text(
                        text = stringResource(R.string.recent_calls_empty_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // ── 3. System Permissions Checklist ──────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
            colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
            border = BorderStroke(Dimens.BorderThickness, OutlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(Dimens.PaddingMedium)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(Dimens.IconSizeMedium))
                    Spacer(modifier = Modifier.width(Dimens.PaddingSmall))
                    Text(
                        text = stringResource(R.string.sys_permissions_status),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnSurfaceDark
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.CornerRadiusMedium))

                PermissionRow(
                    title = stringResource(R.string.perm_record_audio),
                    isGranted = hasRecordAudioPerm
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingSmall), color = OutlineVariant.copy(alpha = 0.2f))
                PermissionRow(
                    title = stringResource(R.string.perm_read_call_log),
                    isGranted = hasCallLogPerm
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.PaddingSmall), color = OutlineVariant.copy(alpha = 0.2f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (!isBatteryOptimized) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (!isBatteryOptimized) ActiveEmerald else WarningAmber,
                            modifier = Modifier.size(Dimens.Size18)
                        )
                        Spacer(modifier = Modifier.width(Dimens.PaddingSmall))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.battery_opt), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = OnSurfaceDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = if (!isBatteryOptimized) stringResource(R.string.battery_opt_ignored) else stringResource(R.string.battery_opt_needed),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                    if (isBatteryOptimized) {
                        TextButton(onClick = onFixBatteryOptClick) {
                            Text(stringResource(R.string.fix_issue), color = PrimaryTeal, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── 4. Compliance Note Card ──────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
            colors = CardDefaults.cardColors(containerColor = PrimaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationSmall)
        ) {
            Row(
                modifier = Modifier.padding(Dimens.Space14),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = OnPrimaryContainer, modifier = Modifier.size(Dimens.IconSizeMedium))
                Spacer(modifier = Modifier.width(Dimens.Space10))
                Column {
                    Text(
                        text = stringResource(R.string.compliance_note_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = OnPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(Dimens.Space2))
                    Text(
                        text = stringResource(R.string.compliance_note_desc),
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
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) ActiveEmerald else WarningAmber,
                modifier = Modifier.size(Dimens.Size18)
            )
            Spacer(modifier = Modifier.width(Dimens.PaddingSmall))
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = OnSurfaceDark, modifier = Modifier.weight(1f))
        }
        Text(
            text = if (isGranted) stringResource(R.string.perm_granted) else stringResource(R.string.perm_denied),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isGranted) ActiveEmerald else WarningAmber
        )
    }
}
