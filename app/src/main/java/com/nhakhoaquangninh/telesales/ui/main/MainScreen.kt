package com.nhakhoaquangninh.telesales.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.theme.BackgroundLight
import com.nhakhoaquangninh.telesales.theme.DangerRed
import com.nhakhoaquangninh.telesales.theme.Dimens
import com.nhakhoaquangninh.telesales.theme.NotificationBadgeRed
import com.nhakhoaquangninh.telesales.theme.OnSurfaceDark
import com.nhakhoaquangninh.telesales.theme.OnSurfaceMuted
import com.nhakhoaquangninh.telesales.theme.OnSurfaceVariant
import com.nhakhoaquangninh.telesales.theme.OutlineVariant
import com.nhakhoaquangninh.telesales.theme.PrimaryTeal
import com.nhakhoaquangninh.telesales.theme.SecondaryContainer
import com.nhakhoaquangninh.telesales.theme.SurfaceContainer
import com.nhakhoaquangninh.telesales.theme.SurfaceLowest
import com.nhakhoaquangninh.telesales.theme.SurfaceMuted
import com.nhakhoaquangninh.telesales.ui.components.OtpSixDigitInput
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

    val requestStopServiceOtpState by viewModel.requestStopServiceOtpState.collectAsStateWithLifecycle()
    val verifyStopServiceOtpState by viewModel.verifyStopServiceOtpState.collectAsStateWithLifecycle()
    val stopServiceOtpInput by viewModel.stopServiceOtpInput.collectAsStateWithLifecycle()
    val stopServiceOtpError by viewModel.stopServiceOtpError.collectAsStateWithLifecycle()

    var showConfirmStopServiceDialog by rememberSaveable { mutableStateOf(false) }
    var showStopServiceOtpDialog by rememberSaveable { mutableStateOf(false) }
    val stopServiceFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val session = remember { TokenManager.getInstance(context).getSession() }
    val careTypeOptions = remember(session) { session?.careTypeOptions ?: emptyList() }
    val selectedCareType by viewModel.selectedCareType.collectAsStateWithLifecycle()

    LaunchedEffect(careTypeOptions) {
        val savedValue = TokenManager.getInstance(context).getSelectedCareTypeValue()
        viewModel.initCareType(careTypeOptions, savedValue, context)
    }

    val isServiceRunning by TelesalesForegroundService.isRunning.collectAsStateWithLifecycle()

    LaunchedEffect(requestStopServiceOtpState) {
        when (val state = requestStopServiceOtpState) {
            is Resource.Success -> {
                showConfirmStopServiceDialog = false
                showStopServiceOtpDialog = true
                viewModel.resetRequestStopServiceOtpState()
            }
            is Resource.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetRequestStopServiceOtpState()
            }
            else -> Unit
        }
    }

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
            } catch (_: Exception) {
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
        TelesalesForegroundService.startService(context)
        viewModel.loadFiles(context)
    }

    LaunchedEffect(onLogout) {
        com.nhakhoaquangninh.telesales.UnauthorizedEventBus.events.collect {
            onLogout()
        }
    }

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
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryTeal,
                            maxLines = 2,
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
                            viewModel.syncFiles(context, filesToSync) { msg, _ ->
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
                    val callRecords by viewModel.callRecords.collectAsStateWithLifecycle()
                    
                    val todayStart = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")).apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val todayRecords = callRecords.filter { it.startedAtMillis >= todayStart }

                    val totalCount = todayRecords.size
                    val syncedCount = todayRecords.count { it.status == "SYNCED" }
                    val pendingCount = todayRecords.count { it.status == "PENDING" || it.status == "FAILED" || it.status == "RETRYABLE" }

                    HomeScreenContent(
                        isServiceRunning = isServiceRunning,
                        totalCallsToday = totalCount,
                        syncedCalls = syncedCount,
                        pendingCalls = pendingCount,
                        recentCalls = todayRecords.take(10), // Passed to HomeScreenContent
                        careTypeOptions = careTypeOptions,
                        selectedCareType = selectedCareType,
                        onCareTypeSelected = { option ->
                            viewModel.onCareTypeSelected(option, context)
                        },
                        onToggleService = { enable ->
                            if (enable) {
                                val intent = Intent(context, TelesalesForegroundService::class.java)
                                ContextCompat.startForegroundService(context, intent)
                                Toast.makeText(context, resources.getString(R.string.home_service_start_success), Toast.LENGTH_SHORT).show()
                            } else {
                                showConfirmStopServiceDialog = true
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
                                viewModel.syncFiles(context, filesToSync) { msg, _ ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
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
                            viewModel.syncFiles(context, listOf(item)) { msg, _ ->
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
                    CircularProgressIndicator(
                        color = PrimaryTeal
                    )
                }
            }
        }
    }

    // Dialog 1: Xác nhận Tạm dừng Dịch vụ
    if (showConfirmStopServiceDialog) {
        AlertDialog(
            onDismissRequest = { 
                showConfirmStopServiceDialog = false 
                viewModel.resetStopServiceOtpState()
            },
            containerColor = SurfaceLowest,
            titleContentColor = OnSurfaceDark,
            textContentColor = OnSurfaceVariant,
            title = {
                Text(
                    text = stringResource(R.string.home_service_stop_confirm_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.home_service_stop_confirm_msg),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                val isLoading = requestStopServiceOtpState is Resource.Loading
                Button(
                    onClick = {
                        session?.userId?.let { userId ->
                            viewModel.requestStopServiceOtp(userId)
                        } ?: run {
                            Toast.makeText(context, resources.getString(R.string.msg_user_not_found, 0), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(Dimens.Size20),
                            strokeWidth = Dimens.Space2
                        )
                    } else {
                        Text(
                            stringResource(R.string.home_service_stop_confirm_btn),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                Button(
                    onClick = { 
                        showConfirmStopServiceDialog = false 
                        viewModel.resetStopServiceOtpState()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceMuted)
                ) {
                    Text(
                        stringResource(R.string.home_service_stop_cancel),
                        color = OnSurfaceMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    // Dialog 2: Nhập OTP Xác nhận Tạm dừng Dịch vụ
    if (showStopServiceOtpDialog) {
        AlertDialog(
            onDismissRequest = { 
                showStopServiceOtpDialog = false 
                viewModel.resetStopServiceOtpState()
            },
            containerColor = SurfaceLowest,
            titleContentColor = OnSurfaceDark,
            textContentColor = OnSurfaceVariant,
            title = {
                Text(
                    text = stringResource(R.string.otp_security_verification),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space16)
                ) {
                    Text(
                        text = stringResource(R.string.home_service_stop_otp_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    OtpSixDigitInput(
                        value = stopServiceOtpInput,
                        onValueChange = { viewModel.onStopServiceOtpChanged(it) },
                        focusRequester = stopServiceFocusRequester,
                        onDone = {
                            keyboardController?.hide()
                            session?.userId?.let { userId ->
                                viewModel.verifyStopServiceOtp(userId, context) {
                                    val intent = Intent(context, TelesalesForegroundService::class.java)
                                    context.stopService(intent)
                                    showStopServiceOtpDialog = false
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.home_service_stop_success),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (stopServiceOtpError != null) {
                        Text(
                            text = stopServiceOtpError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                val isLoading = verifyStopServiceOtpState is Resource.Loading
                Button(
                    onClick = {
                        keyboardController?.hide()
                        session?.userId?.let { userId ->
                            viewModel.verifyStopServiceOtp(userId, context) {
                                val intent = Intent(context, TelesalesForegroundService::class.java)
                                context.stopService(intent)
                                showStopServiceOtpDialog = false
                                Toast.makeText(
                                    context,
                                    resources.getString(R.string.home_service_stop_success),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    enabled = !isLoading && stopServiceOtpInput.length == 6,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(Dimens.Size20),
                            strokeWidth = Dimens.Space2
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.home_service_stop_otp_confirm),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                Button(
                    onClick = { 
                        showStopServiceOtpDialog = false
                        viewModel.resetStopServiceOtpState()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceMuted)
                ) {
                    Text(
                        text = stringResource(R.string.home_service_stop_cancel),
                        color = OnSurfaceMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}
