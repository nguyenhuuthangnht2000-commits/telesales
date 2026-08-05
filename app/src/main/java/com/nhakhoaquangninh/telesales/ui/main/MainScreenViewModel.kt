package com.nhakhoaquangninh.telesales.ui.main

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.nhakhoaquangninh.telesales.CallStateReceiver
import com.nhakhoaquangninh.telesales.core.BaseViewModel
import com.nhakhoaquangninh.telesales.data.local.SyncStatus
import com.nhakhoaquangninh.telesales.data.local.SyncStatusManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class AudioItemState(
    val file: File,
    val status: SyncStatus
)

class MainScreenViewModel : BaseViewModel() {
    private val _audioFiles = MutableStateFlow<List<AudioItemState>>(emptyList())
    val audioFiles: StateFlow<List<AudioItemState>> = _audioFiles

    companion object {
        private const val TAG = "MainScreenViewModel"
    }

    fun loadFiles(context: Context) {
        val validExtensions = setOf("mp3", "amr", "wav", "m4a", "3gp", "aac")
        val deviceDirs = CallStateReceiver.getDirectoriesForDevice()
        val foundFiles = mutableListOf<File>()

        try {
            val projection = arrayOf(
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DATE_MODIFIED
            )
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, null, null,
                "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
            )
            cursor?.use { c ->
                val dataIndex = c.getColumnIndex(MediaStore.Audio.Media.DATA)
                while (c.moveToNext()) {
                    if (dataIndex != -1) {
                        val filePath = c.getString(dataIndex)
                        if (!filePath.isNullOrEmpty()) {
                            val file = File(filePath)
                            if (file.exists() && file.extension.lowercase() in validExtensions) {
                                if (isCallRecording(file, deviceDirs)) {
                                    foundFiles.add(file)
                                }
                            }
                        }
                    }
                }
            }
            Log.d(TAG, "MediaStore tìm thấy ${foundFiles.size} file ghi âm cuộc gọi")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi đọc MediaStore: ${e.message}")
        }

        val appMusicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        if (appMusicDir != null && appMusicDir.exists()) {
            appMusicDir.listFiles()?.filter {
                it.isFile && it.extension.lowercase() in validExtensions
            }?.let { foundFiles.addAll(it) }
        }

        for (dirPath in deviceDirs) {
            val dir = File(dirPath)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.filter {
                    it.isFile && it.extension.lowercase() in validExtensions && isCallRecording(
                        it,
                        deviceDirs
                    )
                }?.let { foundFiles.addAll(it) }
            }
        }

        val sortedList = foundFiles
            .distinctBy { it.absolutePath }
            .sortedByDescending { it.lastModified() }
            
        val syncManager = SyncStatusManager.getInstance(context)
        val stateList = sortedList.map { file ->
            AudioItemState(file, syncManager.getStatus(file.name))
        }

        Log.d(TAG, "Tổng cộng tìm thấy ${stateList.size} file ghi âm cuộc gọi hợp lệ")
        _audioFiles.value = stateList
    }

    private fun isCallRecording(file: File, deviceDirs: List<String>): Boolean {
        val pathLower = file.absolutePath.lowercase()
        val nameLower = file.name.lowercase()

        val excludeKeywords = listOf(
            "viber", "whatsapp", "telegram", "ringtone", "notification", "alarm",
            "over_the_horizon", "over the horizon", "overthehorizon", "horizon",
            "podcasts", "ui/audio", "notifications", "ringtones", "alarms", "sec_music",
            "system/media", "media/audio"
        )
        if (excludeKeywords.any { pathLower.contains(it) || nameLower.contains(it) }) {
            return false
        }

        val dedicatedDirs = deviceDirs.filter { !it.endsWith("/Music/", ignoreCase = true) }
        if (dedicatedDirs.any { dir -> pathLower.startsWith(dir.lowercase()) }) {
            return true
        }

        val includeKeywords = listOf(
            "call", "record", "recording", "cuoc_goi", "cuocgoi", "sound_recorder", "phonerecord"
        )
        return includeKeywords.any { pathLower.contains(it) || nameLower.contains(it) }
    }

    fun deleteFile(file: File) {
        if (file.exists() && file.delete()) {
            val currentList = _audioFiles.value.toMutableList()
            currentList.removeAll { it.file.absolutePath == file.absolutePath }
            _audioFiles.value = currentList
        }
    }
}
