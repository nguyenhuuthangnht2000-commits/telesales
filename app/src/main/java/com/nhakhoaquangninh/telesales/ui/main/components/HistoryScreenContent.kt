package com.nhakhoaquangninh.telesales.ui.main.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nhakhoaquangninh.telesales.R
import com.nhakhoaquangninh.telesales.data.local.SyncStatus
import com.nhakhoaquangninh.telesales.theme.ActiveEmerald
import com.nhakhoaquangninh.telesales.theme.Dimens
import com.nhakhoaquangninh.telesales.theme.OnSurfaceDark
import com.nhakhoaquangninh.telesales.theme.OnSurfaceVariant
import com.nhakhoaquangninh.telesales.theme.OutlineVariant
import com.nhakhoaquangninh.telesales.theme.PrimaryTeal
import com.nhakhoaquangninh.telesales.theme.SurfaceContainer
import com.nhakhoaquangninh.telesales.theme.SurfaceLowest
import com.nhakhoaquangninh.telesales.theme.WarningAmber
import com.nhakhoaquangninh.telesales.ui.main.AudioItemState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreenContent(
    audioFiles: List<AudioItemState>,
    currentlyPlayingPath: String?,
    onPlayClick: (String) -> Unit,
    onSyncClick: (AudioItemState) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredList = remember(audioFiles, selectedFilter) {
        when (selectedFilter) {
            "PENDING" -> audioFiles.filter { it.status == SyncStatus.PENDING || it.status == SyncStatus.UPLOADING }
            "SYNCED" -> audioFiles.filter { it.status == SyncStatus.SYNCED }
            "FAILED" -> audioFiles.filter { it.status == SyncStatus.FAILED }
            else -> audioFiles
        }
    }

    val failedCount = audioFiles.count { it.status == SyncStatus.FAILED }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
            .padding(top = Dimens.PaddingMedium)
    ) {
        // Header
        Column(modifier = Modifier.padding(horizontal = Dimens.PaddingMedium)) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = OnSurfaceDark
            )
            Spacer(modifier = Modifier.height(4.dp))
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterTab(
                    title = stringResource(R.string.tab_all_calls),
                    isSelected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" }
                )
            }
            item {
                FilterTab(
                    title = stringResource(R.string.sync_status_pending),
                    isSelected = selectedFilter == "PENDING",
                    onClick = { selectedFilter = "PENDING" }
                )
            }
            item {
                FilterTab(
                    title = stringResource(R.string.sync_status_synced),
                    isSelected = selectedFilter == "SYNCED",
                    onClick = { selectedFilter = "SYNCED" }
                )
            }
            item {
                FilterTab(
                    title = stringResource(R.string.sync_status_failed),
                    isSelected = selectedFilter == "FAILED",
                    badgeCount = failedCount,
                    isError = true,
                    onClick = { selectedFilter = "FAILED" }
                )
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = OutlineVariant.copy(alpha = 0.3f))

        // List
        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📦", style = MaterialTheme.typography.displayMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Không có cuộc gọi nào",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Dimens.PaddingMedium, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList, key = { it.file.absolutePath }) { item ->
                    val isPlaying = currentlyPlayingPath == item.file.absolutePath
                    HistoryItemCard(
                        item = item,
                        isPlaying = isPlaying,
                        onPlayClick = { onPlayClick(item.file.absolutePath) },
                        onSyncClick = { onSyncClick(item) }
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
                    val strokeWidth = 2.dp.toPx()
                    val y = size.height - strokeWidth / 2
                    drawLine(
                        color = borderColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
            color = textColor
        )
        if (badgeCount > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = CircleShape,
                color = if (isError) MaterialTheme.colorScheme.errorContainer else PrimaryTeal.copy(alpha = 0.2f)
            ) {
                Text(
                    text = badgeCount.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isError) MaterialTheme.colorScheme.error else PrimaryTeal,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
    val isIncoming = if (item.metadata?.callType != null) {
        item.metadata.callType == "incoming"
    } else {
        item.file.name.contains("incoming", ignoreCase = true) || item.file.name.contains("nhan", ignoreCase = true)
    }

    val displayPhone = if (item.metadata != null) {
        if (isIncoming) item.metadata.phoneNumberFrom else item.metadata.phoneNumberTo
    } else {
        item.file.nameWithoutExtension
    } ?: item.file.nameWithoutExtension

    val durationText = if (item.metadata != null && item.metadata.durationSeconds > 0) {
        val m = item.metadata.durationSeconds / 60
        val s = item.metadata.durationSeconds % 60
        String.format(Locale.US, "%02d:%02d", m, s)
    } else {
        ""
    }
    
    val statusColor = when (item.status) {
        SyncStatus.SYNCED -> Color(0xFF137333)
        SyncStatus.FAILED -> MaterialTheme.colorScheme.error
        SyncStatus.PENDING, SyncStatus.UPLOADING -> OnSurfaceVariant
    }
    val statusBgColor = when (item.status) {
        SyncStatus.SYNCED -> Color(0xFFE6F4EA)
        SyncStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
        SyncStatus.PENDING, SyncStatus.UPLOADING -> SurfaceContainer
    }
    val statusIcon = when (item.status) {
        SyncStatus.SYNCED -> Icons.Default.CloudDone
        SyncStatus.FAILED -> Icons.Default.CloudOff
        SyncStatus.PENDING, SyncStatus.UPLOADING -> Icons.Default.Schedule
    }
    val statusText = when (item.status) {
        SyncStatus.SYNCED -> "Synced"
        SyncStatus.FAILED -> "Failed"
        SyncStatus.PENDING -> "Pending"
        SyncStatus.UPLOADING -> "Uploading..."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) PrimaryTeal.copy(alpha = 0.05f) else SurfaceLowest
        ),
        border = BorderStroke(1.dp, if (item.status == SyncStatus.FAILED) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else OutlineVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left info
                Row(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = if (item.status == SyncStatus.FAILED) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else SurfaceContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isIncoming) Icons.Default.CallReceived else Icons.Default.CallMade,
                            contentDescription = null,
                            tint = if (item.status == SyncStatus.FAILED) MaterialTheme.colorScheme.error else OnSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = displayPhone,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = OnSurfaceDark,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val sdf = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }
                        Text(
                            text = sdf.format(Date(item.file.lastModified())),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
                
                // Play Button
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Play/Stop",
                        tint = PrimaryTeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (durationText.isNotEmpty()) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("•", color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "${item.file.length() / 1024} KB",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = statusBgColor,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f)),
                    modifier = if (item.status == SyncStatus.FAILED || item.status == SyncStatus.PENDING) {
                        Modifier.clickable { onSyncClick() }
                    } else {
                        Modifier
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
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
