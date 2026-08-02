package com.nhakhoaquangninh.telesales.ui.main

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.nhakhoaquangninh.telesales.VoIPCall
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val context = LocalContext.current
    val audioFiles by viewModel.audioFiles.collectAsStateWithLifecycle()

    // Trạng thái: file nào đang được phát
    var currentlyPlayingPath by remember { mutableStateOf<String?>(null) }

    // MediaPlayer được quản lý thủ công, dùng ref thay vì remember để tránh recompose leak
    val mediaPlayerRef = remember { mutableStateOf<MediaPlayer?>(null) }

    // Dọn dẹp khi rời khỏi màn hình
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

    // Hàm play/stop an toàn
    fun playOrStop(filePath: String) {
        val currentPlayer = mediaPlayerRef.value

        if (currentlyPlayingPath == filePath) {
            // Đang phát bài này → Dừng
            currentPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
            mediaPlayerRef.value = null
            currentlyPlayingPath = null
        } else {
            // Dừng bài cũ nếu đang phát
            currentPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
            mediaPlayerRef.value = null
            currentlyPlayingPath = null

            // ✅ FIX AUDIO ROUTING:
            // Sau cuộc gọi điện thoại, Android đôi khi giữ audio mode = IN_CALL
            // khiến MediaPlayer phát ra earpiece (loa nhỏ) thay vì loa lớn.
            // Phải reset về MODE_NORMAL và dùng AudioAttributes MUSIC trước khi play.
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_NORMAL

            // Tạo MediaPlayer mới với AudioAttributes rõ ràng
            try {
                val newPlayer = MediaPlayer().apply {
                    // Ép dùng luồng MUSIC → phát qua loa ngoài (media speaker)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(filePath)
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
                android.util.Log.e("MediaPlayer", "Exception khi setDataSource: ${e.message}")
            }
        }
    }

    // Tự động load file khi vào màn hình, khi cấp đủ quyền (ON_RESUME), và khi cuộc gọi kết thúc
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadFiles(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: android.content.Intent?) {
                viewModel.loadFiles(context)
            }
        }

        val filter =
            android.content.IntentFilter(com.nhakhoaquangninh.telesales.CallStateReceiver.ACTION_REFRESH_FILES)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📋 Danh Sách Ghi Âm") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { onItemClick(VoIPCall) }) {
                        Text(text = "📞", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.loadFiles(context)
            }) {
                Text(text = "🔄", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (audioFiles.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⚠️", style = MaterialTheme.typography.displayMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Chưa phát hiện file ghi âm cuộc gọi nào!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "📋 Hướng dẫn tuân thủ:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "1. Mở ứng dụng Điện thoại trên máy\n2. Vào Cài đặt (⚙️) ➔ Ghi âm cuộc gọi\n3. Bật tính năng 'Tự động ghi âm cuộc gọi'\n4. Thực hiện cuộc gọi để ứng dụng ghi nhận",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(audioFiles, key = { it.absolutePath }) { file ->
                        val isPlaying = currentlyPlayingPath == file.absolutePath
                        AudioFileItem(
                            file = file,
                            isPlaying = isPlaying,
                            onPlayClick = { playOrStop(file.absolutePath) },
                            onDeleteClick = {
                                if (currentlyPlayingPath == file.absolutePath) {
                                    mediaPlayerRef.value?.apply {
                                        if (isPlaying) stop()
                                        reset()
                                        release()
                                    }
                                    mediaPlayerRef.value = null
                                    currentlyPlayingPath = null
                                }
                                viewModel.deleteFile(file)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioFileItem(
    file: File,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon micrô bên trái
            Text(
                text = if (isPlaying) "🔊" else "🎙️",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.nameWithoutExtension,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                val sdf = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
                Text(
                    text = "${sdf.format(Date(file.lastModified()))}  •  ${file.length() / 1024} KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isPlaying) {
                    Text(
                        text = "⏵ Đang phát...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Nút Play / Stop
            FilledTonalButton(
                onClick = onPlayClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(if (isPlaying) "■ Stop" else "▶ Play")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Nút Xóa
            OutlinedButton(
                onClick = onDeleteClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("🗑")
            }
        }
    }
}
