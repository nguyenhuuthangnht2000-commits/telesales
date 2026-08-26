package com.nhakhoaquangninh.telesales.ui.main.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.nhakhoaquangninh.telesales.R
import com.nhakhoaquangninh.telesales.data.local.FailedCallEvent
import com.nhakhoaquangninh.telesales.data.local.SyncStatus
import com.nhakhoaquangninh.telesales.domain.model.CallType
import com.nhakhoaquangninh.telesales.domain.model.FailureReason
import com.nhakhoaquangninh.telesales.theme.Dimens
import com.nhakhoaquangninh.telesales.theme.OnSurfaceDark
import com.nhakhoaquangninh.telesales.theme.OnSurfaceVariant
import com.nhakhoaquangninh.telesales.theme.OutlineVariant
import com.nhakhoaquangninh.telesales.theme.PrimaryTeal
import com.nhakhoaquangninh.telesales.theme.SuccessContainer
import com.nhakhoaquangninh.telesales.theme.SuccessText
import com.nhakhoaquangninh.telesales.theme.SurfaceContainer
import com.nhakhoaquangninh.telesales.theme.SurfaceLowest
import com.nhakhoaquangninh.telesales.ui.main.AudioItemState
import com.nhakhoaquangninh.telesales.ui.main.HistoryFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed interface HistoryEntry {
    val id: String
    val timestamp: Long

    data class Recording(val item: AudioItemState) : HistoryEntry {
        override val id: String = item.recording.uri
        override val timestamp: Long = item.recording.modifiedAtMillis
    }

    data class FailedCall(val event: FailedCallEvent) : HistoryEntry {
        override val id: String = event.id
        override val timestamp: Long = event.callAtMillis
    }
}

@Composable
fun HistoryScreenContent(
    audioFiles: List<AudioItemState>,
    failedCallEvents: List<FailedCallEvent>,
    currentlyPlayingPath: String?,
    onPlayClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSyncClick: (AudioItemState) -> Unit = {},
    onDeleteFailedCall: (String) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }

    val historyEntries = remember(audioFiles, failedCallEvents) {
        (audioFiles.map { HistoryEntry.Recording(it) } +
                failedCallEvents.map { HistoryEntry.FailedCall(it) })
            .sortedByDescending { it.timestamp }
    }
    val filteredList = remember(historyEntries, selectedFilter) {
        when (selectedFilter) {
            HistoryFilter.PENDING -> historyEntries.filter {
                it is HistoryEntry.Recording &&
                        (it.item.status == SyncStatus.PENDING || it.item.status == SyncStatus.UPLOADING)
            }

            HistoryFilter.SYNCED -> historyEntries.filter {
                it is HistoryEntry.Recording && it.item.status == SyncStatus.SYNCED
            }

            HistoryFilter.FAILED -> historyEntries.filter {
                it is HistoryEntry.FailedCall ||
                        (it is HistoryEntry.Recording && (it.item.status == SyncStatus.FAILED || it.item.status == SyncStatus.NEEDS_REVIEW))
            }

            else -> historyEntries
        }
    }

    val failedCount = historyEntries.count {
        it is HistoryEntry.FailedCall ||
                (it is HistoryEntry.Recording && (it.item.status == SyncStatus.FAILED || it.item.status == SyncStatus.NEEDS_REVIEW))
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = Dimens.PaddingMedium)
    ) {
        // Header
        Column(modifier = Modifier.padding(horizontal = Dimens.PaddingMedium)) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = OnSurfaceDark
            )
            Spacer(modifier = Modifier.height(Dimens.Space4))
            Text(
                text = stringResource(R.string.history_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(Dimens.PaddingMedium))

        // Filter Tabs
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = Dimens.PaddingMedium),
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space8)
        ) {
            item {
                FilterTab(
                    title = stringResource(R.string.tab_all_calls),
                    isSelected = selectedFilter == HistoryFilter.ALL,
                    onClick = { selectedFilter = HistoryFilter.ALL }
                )
            }
            item {
                FilterTab(
                    title = stringResource(R.string.sync_status_pending),
                    isSelected = selectedFilter == HistoryFilter.PENDING,
                    onClick = { selectedFilter = HistoryFilter.PENDING }
                )
            }
            item {
                FilterTab(
                    title = stringResource(R.string.sync_status_synced),
                    isSelected = selectedFilter == HistoryFilter.SYNCED,
                    onClick = { selectedFilter = HistoryFilter.SYNCED }
                )
            }
            item {
                FilterTab(
                    title = stringResource(R.string.sync_status_failed),
                    isSelected = selectedFilter == HistoryFilter.FAILED,
                    badgeCount = failedCount,
                    isError = true,
                    onClick = { selectedFilter = HistoryFilter.FAILED }
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = Dimens.Space8),
            color = OutlineVariant.copy(alpha = 0.3f)
        )

        // List
        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.history_empty_icon),
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(modifier = Modifier.height(Dimens.Space8))
                    Text(
                        text = stringResource(R.string.history_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = Dimens.PaddingMedium,
                    vertical = Dimens.Space8
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space12)
            ) {
                items(filteredList, key = { it.id }) { entry ->
                    when (entry) {
                        is HistoryEntry.Recording -> {
                            val item = entry.item
                            val isPlaying = currentlyPlayingPath == item.recording.uri
                            HistoryItemCard(
                                item = item,
                                isPlaying = isPlaying,
                                onPlayClick = { onPlayClick(item.recording.uri) },
                                onSyncClick = { onSyncClick(item) }
                            )
                        }

                        is HistoryEntry.FailedCall -> FailedCallCard(
                            event = entry.event,
                            onDeleteClick = { onDeleteFailedCall(entry.event.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FailedCallCard(
    event: FailedCallEvent,
    onDeleteClick: () -> Unit
) {
    val isIncoming = event.callType == CallType.INCOMING
    val displayPhone = (if (isIncoming) event.phoneNumberFrom else event.phoneNumberTo)
        ?: stringResource(R.string.history_unknown_phone)
    val statusText = when {
        event.failureReason == FailureReason.MISSED -> stringResource(R.string.history_missed_call)
        event.failureReason == FailureReason.NO_RECORDING || event.durationSeconds > 0 -> stringResource(R.string.history_no_recording)
        else -> stringResource(R.string.history_call_not_connected)
    }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
        colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
        border = BorderStroke(
            Dimens.BorderThickness,
            MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationSmall)
    ) {
        Column(modifier = Modifier.padding(Dimens.PaddingMedium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.size(Dimens.IconSizeExtraLarge)
                    ) {
                        Icon(
                            imageVector = if (isIncoming) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(Dimens.PaddingSmall)
                        )
                    }
                    Spacer(modifier = Modifier.width(Dimens.PaddingSmall))
                    Column {
                        Text(
                            text = displayPhone,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OnSurfaceDark
                        )
                        val formattedDate = dateFormatter.format(Date(event.callAtMillis))
                        val durationInfo = if (event.durationSeconds > 0) {
                            val m = event.durationSeconds / 60
                            val s = event.durationSeconds % 60
                            String.format(Locale.US, " • %02d:%02d", m, s)
                        } else {
                            ""
                        }
                        Text(
                            text = "$formattedDate$durationInfo",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.history_delete_failed_call),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(Dimens.PaddingSmall))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                border = BorderStroke(
                    Dimens.BorderThickness,
                    MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = Dimens.PaddingSmall,
                        vertical = Dimens.PaddingExtraSmall
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(Dimens.IconSizeSmall)
                    )
                    Spacer(modifier = Modifier.width(Dimens.PaddingExtraSmall))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    badgeCount: Int = 0,
    isError: Boolean = false
) {
    val textColor = if (isSelected) PrimaryTeal else OnSurfaceVariant
    val borderColor = if (isSelected) PrimaryTeal else Color.Transparent

    Row(
        modifier = Modifier
            .clickable { onClick() }
            .drawBehind {
                if (isSelected) {
                    val strokeWidth = Dimens.Space2.toPx()
                    val y = size.height - strokeWidth / 2
                    drawLine(
                        color = borderColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                }
            }
            .padding(horizontal = Dimens.Space16, vertical = Dimens.Space12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
            color = textColor
        )
        if (badgeCount > 0) {
            Spacer(modifier = Modifier.width(Dimens.Space6))
            Surface(
                shape = CircleShape,
                color = if (isError) MaterialTheme.colorScheme.errorContainer else PrimaryTeal.copy(
                    alpha = 0.2f
                )
            ) {
                Text(
                    text = badgeCount.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isError) MaterialTheme.colorScheme.error else PrimaryTeal,
                    modifier = Modifier.padding(
                        horizontal = Dimens.Space6,
                        vertical = Dimens.Space2
                    )
                )
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    item: AudioItemState,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    val isIncoming = item.metadata?.callType == CallType.INCOMING

    val displayPhone = item.recording.displayName.substringBeforeLast('.')

    val durationText = if (item.metadata != null && item.metadata.durationSeconds > 0) {
        val m = item.metadata.durationSeconds / 60
        val s = item.metadata.durationSeconds % 60
        String.format(Locale.US, "%02d:%02d", m, s)
    } else {
        ""
    }

    val statusColor = when (item.status) {
        SyncStatus.SYNCED -> SuccessText
        SyncStatus.FAILED -> MaterialTheme.colorScheme.error
        SyncStatus.PENDING, SyncStatus.UPLOADING -> OnSurfaceVariant
        SyncStatus.NEEDS_REVIEW -> MaterialTheme.colorScheme.tertiary
    }
    val statusBgColor = when (item.status) {
        SyncStatus.SYNCED -> SuccessContainer
        SyncStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
        SyncStatus.PENDING, SyncStatus.UPLOADING -> SurfaceContainer
        SyncStatus.NEEDS_REVIEW -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val statusIcon = when (item.status) {
        SyncStatus.SYNCED -> Icons.Default.CloudDone
        SyncStatus.FAILED -> Icons.Default.CloudOff
        SyncStatus.PENDING, SyncStatus.UPLOADING -> Icons.Default.Schedule
        SyncStatus.NEEDS_REVIEW -> Icons.Default.ErrorOutline
    }
    val statusText = when (item.status) {
        SyncStatus.SYNCED -> stringResource(R.string.sync_status_synced)
        SyncStatus.FAILED -> stringResource(R.string.sync_status_failed)
        SyncStatus.PENDING -> stringResource(R.string.sync_status_pending)
        SyncStatus.UPLOADING -> stringResource(R.string.sync_status_uploading)
        SyncStatus.NEEDS_REVIEW -> stringResource(R.string.sync_status_needs_review)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.Space12),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) PrimaryTeal.copy(alpha = 0.05f) else SurfaceLowest
        ),
        border = BorderStroke(
            Dimens.BorderThickness,
            if (item.status == SyncStatus.FAILED) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else OutlineVariant.copy(
                alpha = 0.3f
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationExtraSmall)
    ) {
        Column(modifier = Modifier.padding(Dimens.Space16)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left info
                Row(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = if (item.status == SyncStatus.FAILED) MaterialTheme.colorScheme.errorContainer.copy(
                            alpha = 0.3f
                        ) else SurfaceContainer,
                        modifier = Modifier.size(Dimens.Size40)
                    ) {
                        Icon(
                            imageVector = if (isIncoming) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade,
                            contentDescription = null,
                            tint = if (item.status == SyncStatus.FAILED) MaterialTheme.colorScheme.error else OnSurfaceVariant,
                            modifier = Modifier.padding(Dimens.Space8)
                        )
                    }
                    Spacer(modifier = Modifier.width(Dimens.Space12))
                    Column {
                        Text(
                            text = displayPhone,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OnSurfaceDark,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(Dimens.Space2))
                        val sdf =
                            remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }
                        Text(
                            text = sdf.format(Date(item.recording.modifiedAtMillis)),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }

                // Play Button
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier.size(Dimens.Size36)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.history_play_stop),
                        tint = PrimaryTeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Space12))

            // Bottom tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (durationText.isNotEmpty()) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(Dimens.Space16)
                        )
                        Spacer(modifier = Modifier.width(Dimens.Space4))
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(Dimens.Space8))
                        Text(
                            stringResource(R.string.history_separator),
                            color = OnSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(Dimens.Space8))
                    }
                    Text(
                        text = stringResource(
                            R.string.history_file_size_kb,
                            item.recording.sizeBytes / 1024
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = statusBgColor,
                    border = BorderStroke(Dimens.BorderThickness, statusColor.copy(alpha = 0.2f)),
                    modifier = if (item.status == SyncStatus.FAILED || item.status == SyncStatus.PENDING) {
                        Modifier.clickable { onSyncClick() }
                    } else {
                        Modifier
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = Dimens.Space10,
                            vertical = Dimens.Space4
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(Dimens.Size14)
                        )
                        Spacer(modifier = Modifier.width(Dimens.Space4))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )
                    }
                }
            }
        }
    }
}
