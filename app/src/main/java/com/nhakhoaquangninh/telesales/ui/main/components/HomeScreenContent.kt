package com.nhakhoaquangninh.telesales.ui.main.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.nhakhoaquangninh.telesales.R
import com.nhakhoaquangninh.telesales.domain.model.CareTypeOption
import com.nhakhoaquangninh.telesales.theme.ActiveEmerald
import com.nhakhoaquangninh.telesales.theme.Dimens
import com.nhakhoaquangninh.telesales.theme.OnPrimaryContainer
import com.nhakhoaquangninh.telesales.theme.OnSecondaryContainer
import com.nhakhoaquangninh.telesales.theme.OnSurfaceDark
import com.nhakhoaquangninh.telesales.theme.OnSurfaceVariant
import com.nhakhoaquangninh.telesales.theme.OutlineColor
import com.nhakhoaquangninh.telesales.theme.OutlineVariant
import com.nhakhoaquangninh.telesales.theme.PrimaryContainer
import com.nhakhoaquangninh.telesales.theme.PrimaryTeal
import com.nhakhoaquangninh.telesales.theme.SecondaryContainer
import com.nhakhoaquangninh.telesales.theme.SurfaceContainer
import com.nhakhoaquangninh.telesales.theme.SurfaceContainerLow
import com.nhakhoaquangninh.telesales.theme.SurfaceLowest
import com.nhakhoaquangninh.telesales.theme.WarningAmber

@Composable
fun HomeScreenContent(
    isServiceRunning: Boolean,
    totalCallsToday: Int,
    syncedCalls: Int,
    pendingCalls: Int,
    recentCalls: List<com.nhakhoaquangninh.telesales.data.local.room.CallRecordEntity> = emptyList(),
    careTypeOptions: List<CareTypeOption> = emptyList(),
    selectedCareType: CareTypeOption? = null,
    onCareTypeSelected: (CareTypeOption) -> Unit = {},
    onToggleService: (Boolean) -> Unit,
    onSyncNowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

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

        // ── 2. Care Type Selection Card (Modern Interactive Combobox) ───────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
            colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
            border = BorderStroke(Dimens.BorderThickness, OutlineVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.PaddingMedium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(Dimens.IconSizeMedium)
                        )
                        Spacer(modifier = Modifier.width(Dimens.PaddingSmall))
                        Text(
                            text = stringResource(R.string.care_type_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OnSurfaceDark
                        )
                    }
                    if (careTypeOptions.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(Dimens.CornerRadiusSmall),
                            color = SecondaryContainer.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = stringResource(R.string.care_type_options_count, careTypeOptions.size),
                                modifier = Modifier.padding(horizontal = Dimens.Space8, vertical = Dimens.Space2),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = OnSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.Space4))
                Text(
                    text = stringResource(R.string.care_type_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Dimens.PaddingMedium))

                // Interactive Trigger Box
                val chevronRotation by animateFloatAsState(
                    targetValue = if (isDropdownExpanded) 180f else 0f,
                    label = "chevron_rotation"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
                        .border(
                            BorderStroke(
                                if (isDropdownExpanded) Dimens.Space2 else Dimens.BorderThickness,
                                if (isDropdownExpanded) PrimaryTeal else OutlineVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(Dimens.CornerRadiusMedium)
                        )
                        .clickable { isDropdownExpanded = !isDropdownExpanded },
                    color = if (isDropdownExpanded) PrimaryTeal.copy(alpha = 0.04f) else SurfaceContainerLow,
                    shape = RoundedCornerShape(Dimens.CornerRadiusMedium)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.PaddingMedium, vertical = Dimens.Space12),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDropdownExpanded) PrimaryTeal else PrimaryTeal.copy(alpha = 0.12f),
                            modifier = Modifier.size(Dimens.Size36)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MedicalServices,
                                    contentDescription = null,
                                    tint = if (isDropdownExpanded) Color.White else PrimaryTeal,
                                    modifier = Modifier.size(Dimens.IconSizeMedium)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(Dimens.Space12))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.care_type_current_active),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = PrimaryTeal
                            )
                            Spacer(modifier = Modifier.height(Dimens.Space2))
                            Text(
                                text = selectedCareType?.label ?: stringResource(R.string.care_type_select_hint),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = OnSurfaceDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(Dimens.Space8))

                        Surface(
                            shape = CircleShape,
                            color = if (isDropdownExpanded) PrimaryTeal.copy(alpha = 0.15f) else Color.Transparent,
                            modifier = Modifier.size(Dimens.Size32)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = if (isDropdownExpanded) PrimaryTeal else OnSurfaceVariant,
                                    modifier = Modifier
                                        .size(Dimens.IconSizeLarge)
                                        .graphicsLayer { rotationZ = chevronRotation }
                                )
                            }
                        }
                    }
                }

                // Smooth Animated Expandable Dropdown List
                AnimatedVisibility(
                    visible = isDropdownExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimens.Space10),
                        verticalArrangement = Arrangement.spacedBy(Dimens.Space8)
                    ) {
                        if (careTypeOptions.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
                                color = SurfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.care_type_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceVariant,
                                    modifier = Modifier.padding(Dimens.PaddingMedium),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            careTypeOptions.forEach { option ->
                                val isSelected = option.value == selectedCareType?.value
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(Dimens.CornerRadiusMedium))
                                        .border(
                                            BorderStroke(
                                                if (isSelected) Dimens.Space2 else Dimens.BorderThickness,
                                                if (isSelected) PrimaryTeal else OutlineVariant.copy(alpha = 0.4f)
                                            ),
                                            shape = RoundedCornerShape(Dimens.CornerRadiusMedium)
                                        )
                                        .clickable {
                                            onCareTypeSelected(option)
                                            isDropdownExpanded = false
                                        },
                                    color = if (isSelected) PrimaryTeal.copy(alpha = 0.08f) else SurfaceLowest,
                                    shape = RoundedCornerShape(Dimens.CornerRadiusMedium)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = Dimens.PaddingMedium,
                                                vertical = Dimens.Space12
                                            ),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (isSelected) PrimaryTeal else OutlineColor.copy(alpha = 0.6f),
                                                modifier = Modifier.size(Dimens.IconSizeMedium)
                                            )
                                            Spacer(modifier = Modifier.width(Dimens.Space12))
                                            Text(
                                                text = option.label,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = if (isSelected) PrimaryTeal else OnSurfaceDark,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (isSelected) {
                                            Surface(
                                                shape = RoundedCornerShape(Dimens.CornerRadiusSmall),
                                                color = PrimaryTeal,
                                                modifier = Modifier.padding(start = Dimens.Space8)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.care_type_active_badge),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White,
                                                    modifier = Modifier.padding(
                                                        horizontal = Dimens.Space8,
                                                        vertical = Dimens.Space2
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 3. Quick Metrics Stack ──────────────────────────────────────
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
                    if (recentCalls.isEmpty()) {
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
                    } else {
                        recentCalls.forEach { record ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Dimens.PaddingSmall),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = record.phoneNumberFrom ?: record.phoneNumberTo ?: stringResource(R.string.history_unknown_phone),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceDark
                                )
                                val statusText = when (record.status) {
                                    "SYNCED" -> stringResource(R.string.sync_status_synced)
                                    "PENDING" -> stringResource(R.string.sync_status_pending)
                                    "UPLOADING" -> stringResource(R.string.sync_status_uploading)
                                    "FAILED" -> stringResource(R.string.sync_status_failed)
                                    "NEEDS_REVIEW" -> stringResource(R.string.sync_status_needs_review)
                                    else -> record.status
                                }
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (record.status == "SYNCED") ActiveEmerald else WarningAmber
                                )
                            }
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
