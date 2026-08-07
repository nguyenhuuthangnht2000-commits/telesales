package com.nhakhoaquangninh.telesales.ui.main

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhakhoaquangninh.telesales.R
import com.nhakhoaquangninh.telesales.TelesalesForegroundService
import com.nhakhoaquangninh.telesales.data.local.SyncStatus
import com.nhakhoaquangninh.telesales.data.local.TokenManager
import com.nhakhoaquangninh.telesales.theme.BackgroundLight
import com.nhakhoaquangninh.telesales.theme.Dimens
import com.nhakhoaquangninh.telesales.theme.NotificationBadgeRed
import com.nhakhoaquangninh.telesales.theme.OnSurfaceDark
import com.nhakhoaquangninh.telesales.theme.OnSurfaceVariant
import com.nhakhoaquangninh.telesales.theme.OutlineVariant
import com.nhakhoaquangninh.telesales.theme.PrimaryTeal
import com.nhakhoaquangninh.telesales.theme.SecondaryContainer
import com.nhakhoaquangninh.telesales.theme.SurfaceContainer
import com.nhakhoaquangninh.telesales.theme.SurfaceLowest
import com.nhakhoaquangninh.telesales.ui.main.components.HistoryScreenContent
import com.nhakhoaquangninh.telesales.ui.main.components.HomeScreenContent
import com.nhakhoaquangninh.telesales.ui.main.components.SettingsScreenContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {},
    viewModel: MainScreenViewModel = viewModel()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val audioFiles by viewModel.audioFiles.collectAsStateWithLifecycle()
    val failedCallEvents by viewModel.failedCallEvents.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val session = remember { TokenManager.getInstance(context).getSession() }

    val isServiceRunning by TelesalesForegroundService.isRunning.collectAsStateWithLifecycle()

    var currentlyPlayingPath by remember { mutableStateOf<String?>(null) }
    val mediaPlayerRef = remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayerRef.value?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
            mediaPlayerRef.value = null
        }
    }

    fun playOrStop(filePath: String) {
        val currentPlayer = mediaPlayerRef.value

        if (currentlyPlayingPath == filePath) {
            currentPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
            mediaPlayerRef.value = null
            currentlyPlayingPath = null
        } else {
            currentPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
            mediaPlayerRef.value = null
            currentlyPlayingPath = null

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_NORMAL

            try {
                val newPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(context, filePath.toUri())
                    prepare()
                    start()
                    setOnCompletionListener {
                        reset()
                        release()
                        mediaPlayerRef.value = null
                        currentlyPlayingPath = null
                    }
                    setOnErrorListener { _, what, extra ->
                        android.util.Log.e("MediaPlayer", "Lỗi phát nhạc: what=$what extra=$extra")
                        reset()
                        release()
                        mediaPlayerRef.value = null
                        currentlyPlayingPath = null
                        true
                    }
                }
                mediaPlayerRef.value = newPlayer
                currentlyPlayingPath = filePath
            } catch (e: Exception) {
                android.util.Log.e("MediaPlayer", "Không thể phát file âm thanh")
            }
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadFiles(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                viewModel.loadFiles(context)
            }
        }

        val filter =
            android.content.IntentFilter(com.nhakhoaquangninh.telesales.CallStateReceiver.ACTION_REFRESH_FILES)
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadFiles(context)
    }

    LaunchedEffect(onLogout) {
        com.nhakhoaquangninh.telesales.UnauthorizedEventBus.events.collect {
            onLogout()
        }
    }

    val hasRecordAudioPerm = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val hasCallLogPerm = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CALL_LOG
    ) == PackageManager.PERMISSION_GRANTED

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isBatteryOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Surface(
                        modifier = Modifier
                            .padding(start = Dimens.PaddingMedium, end = Dimens.PaddingSmall)
                            .size(Dimens.IconSizeExtraLarge),
                        shape = CircleShape,
                        color = SurfaceLowest,
                        border = BorderStroke(
                            Dimens.BorderThickness,
                            OutlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(R.string.avatar_desc),
                            modifier = Modifier.padding(Dimens.PaddingSmall),
                            tint = OnSurfaceVariant
                        )
                    }
                },
                title = {
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = session?.userName ?: stringResource(R.string.app_title),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryTeal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.active_shift),
                            style = MaterialTheme.typography.bodyLarge,
                            color = PrimaryTeal
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            Toast.makeText(
                                context,
                                resources.getString(R.string.no_notifications),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.padding(end = Dimens.PaddingMedium)
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = stringResource(R.string.notifications_desc),
                                tint = OnSurfaceDark,
                                modifier = Modifier.size(Dimens.Size28)
                            )
                            Box(
                                modifier = Modifier
                                    .size(Dimens.Space10)
                                    .background(NotificationBadgeRed, shape = CircleShape)
                                    .align(Alignment.TopEnd)
                                    .offset(x = Dimens.OffsetNegativeSmall, y = Dimens.Space2)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = PrimaryTeal,
                    actionIconContentColor = OnSurfaceDark
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceContainer
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = stringResource(R.string.tab_home)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_home)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryTeal,
                        selectedTextColor = PrimaryTeal,
                        indicatorColor = SecondaryContainer
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            Icons.Default.History,
                            contentDescription = stringResource(R.string.tab_history)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_history)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryTeal,
                        selectedTextColor = PrimaryTeal,
                        indicatorColor = SecondaryContainer
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.tab_settings)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_settings)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryTeal,
                        selectedTextColor = PrimaryTeal,
                        indicatorColor = SecondaryContainer
                    )
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = {
                        val filesToSync =
                            audioFiles.filter { it.status == SyncStatus.PENDING || it.status == SyncStatus.FAILED }
                        if (filesToSync.isEmpty()) {
                            Toast.makeText(
                                context,
                                resources.getString(R.string.msg_no_file_to_sync),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            viewModel.syncFiles(context, filesToSync) { msg, success ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    containerColor = PrimaryTeal,
                    contentColor = Color.White
                ) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = stringResource(R.string.manual_sync_all)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> {
                    HomeScreenContent(
                        isServiceRunning = isServiceRunning,
                        totalCallsToday = audioFiles.size,
                        syncedCalls = 0,
                        pendingCalls = audioFiles.size,
                        hasRecordAudioPerm = hasRecordAudioPerm,
                        hasCallLogPerm = hasCallLogPerm,
                        isBatteryOptimized = isBatteryOptimized,
                        onToggleService = { enable ->
                            val intent = Intent(context, TelesalesForegroundService::class.java)
                            if (enable) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                            } else {
                                context.stopService(intent)
                            }
                        },
                        onSyncNowClick = {
                            val filesToSync =
                                audioFiles.filter { it.status == SyncStatus.PENDING || it.status == SyncStatus.FAILED }
                            if (filesToSync.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    resources.getString(R.string.msg_no_file_to_sync),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                viewModel.syncFiles(context, filesToSync) { msg, success ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onFixBatteryOptClick = {
                            try {
                                val intent =
                                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = "package:${context.packageName}".toUri()
                                    }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val fallbackIntent =
                                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(fallbackIntent)
                                } catch (_: Exception) {
                                }
                            }
                        }
                    )
                }

                1 -> {
                    HistoryScreenContent(
                        audioFiles = audioFiles,
                        failedCallEvents = failedCallEvents,
                        currentlyPlayingPath = currentlyPlayingPath,
                        onPlayClick = { filePath -> playOrStop(filePath) },
                        onSyncClick = { item ->
                            viewModel.syncFiles(context, listOf(item)) { msg, success ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        onDeleteFailedCall = { eventId ->
                            viewModel.deleteFailedCallEvent(context, eventId)
                        }
                    )
                }

                2 -> {
                    SettingsScreenContent(
                        context = context,
                        onLogoutClick = onLogout
                    )
                }
            }

            val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
            if (isSyncing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = PrimaryTeal
                    )
                }
            }
        }
    }
}
