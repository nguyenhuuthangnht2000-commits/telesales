package com.nhakhoaquangninh.telesales

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * AudioRecorderHelper v3
 *
 * Hỗ trợ 2 chế độ ghi âm:
 *
 * 1. [startRecordingVoIP]  — VoIP (Stringee): dùng VOICE_COMMUNICATION
 *    → Ghi được 2 chiều hoàn hảo, KHÔNG bị Android AudioPolicy chặn
 *
 * 2. [startRecording]      — SIM call (fallback): thử VOICE_RECOGNITION → MIC → DEFAULT
 *    → Có thể bị mute trên Samsung Android 10+
 *
 * Output: WAV (PCM 16-bit, Mono, 16kHz) — không cần codec, tương thích mọi player
 */
class AudioRecorderHelper {

    private val TAG = "AudioRecorderHelper"

    private val SAMPLE_RATE = 16000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BYTES_PER_SAMPLE = 2

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isRecording = false

    // ─────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Ghi âm VoIP — dùng VOICE_COMMUNICATION.
     * Ghi 2 chiều (nhân viên + khách hàng), KHÔNG bị chặn.
     */
    fun startRecordingVoIP(filePath: String) {
        val wavPath = ensureWav(filePath)
        startWithSource(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            "VOICE_COMMUNICATION",
            wavPath
        )
    }

    /**
     * Ghi âm SIM call — thử nhiều AudioSource theo thứ tự ưu tiên.
     */
    fun startRecording(filePath: String) {
        if (isRecording) {
            Log.w(TAG, "⚠️ Đang ghi âm, gọi stopRecording() trước.")
            return
        }
        val wavPath = ensureWav(filePath)
        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION to "VOICE_RECOGNITION",
            MediaRecorder.AudioSource.MIC to "MIC",
            MediaRecorder.AudioSource.DEFAULT to "DEFAULT"
        )
        for ((src, name) in sources) {
            if (startWithSource(src, name, wavPath)) return
        }
        Log.e(TAG, "❌ Không thể khởi tạo AudioRecord với bất kỳ AudioSource nào!")
    }

    /**
     * Dừng ghi âm. Thread-safe.
     */
    fun stopRecording() {
        if (!isRecording) return
        Log.d(TAG, "⏹️ Dừng ghi âm...")
        isRecording = false
    }

    // ─────────────────────────────────────────────────────────────────────
    // PRIVATE — Core logic
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Khởi tạo AudioRecord với source cho trước và bắt đầu ghi.
     * @return true nếu khởi tạo thành công
     */
    private fun startWithSource(source: Int, sourceName: String, wavPath: String): Boolean {
        if (isRecording) {
            Log.w(TAG, "⚠️ Đang ghi âm, gọi stopRecording() trước.")
            return false
        }
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(8192)
        return try {
            val ar = AudioRecord(source, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize)
            if (ar.state != AudioRecord.STATE_INITIALIZED) {
                Log.w(TAG, "⚠️ $sourceName: STATE không phải INITIALIZED")
                ar.release()
                false
            } else {
                Log.d(TAG, "✅ AudioSource $sourceName OK | File: $wavPath")
                audioRecord = ar
                isRecording = true
                recordingJob = CoroutineScope(Dispatchers.IO).launch {
                    recordToWav(ar, wavPath, bufferSize, sourceName)
                }
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ $sourceName exception: ${e.message}")
            false
        }
    }

    private suspend fun recordToWav(
        ar: AudioRecord,
        wavPath: String,
        bufferSize: Int,
        sourceName: String
    ) = withContext(Dispatchers.IO) {
        val outputFile = File(wavPath)
        var totalBytes = 0L
        var silentChunks = 0
        var totalChunks = 0

        try {
            FileOutputStream(outputFile).use { fos ->
                fos.write(ByteArray(44)) // WAV header placeholder
                ar.startRecording()
                Log.d(TAG, "▶️ AudioRecord.startRecording() | Source=$sourceName")

                val buffer = ByteArray(bufferSize)
                while (isRecording) {
                    val read = ar.read(buffer, 0, bufferSize)
                    if (read > 0) {
                        fos.write(buffer, 0, read)
                        totalBytes += read
                        totalChunks++
                        if (isSilent(buffer, read)) silentChunks++

                        if (totalChunks % 100 == 0) {
                            val silPct = silentChunks * 100 / totalChunks
                            Log.d(TAG, "📊 Chunks=$totalChunks Silence=$silPct% Bytes=$totalBytes")
                            if (silPct > 90) {
                                Log.w(
                                    TAG,
                                    "🔇 >90% silence — mic có thể bị OS mute (SIM call restriction)"
                                )
                            }
                        }
                    } else if (read == AudioRecord.ERROR_INVALID_OPERATION) {
                        Log.e(TAG, "❌ ERROR_INVALID_OPERATION")
                        break
                    }
                }
            }

            writeWavHeader(wavPath, totalBytes)
            val durationSec = totalBytes / (SAMPLE_RATE * BYTES_PER_SAMPLE).toFloat()
            val silPct = if (totalChunks > 0) silentChunks * 100 / totalChunks else 100
            Log.d(TAG, "✅ Ghi âm hoàn tất | ${durationSec.toInt()}s | Silence=$silPct% | $wavPath")

            if (silPct > 90) {
                Log.e(
                    TAG, """
                    ❌ FILE GHI ÂM IM LẶNG ($silPct% silence)!
                    Thiết bị này đang chặn quyền ghi âm trong cuộc gọi SIM.
                    Xem thêm: Cài đặt máy → Ứng dụng → Telesales → Quyền → Micro
                    Hoặc Cài đặt máy → Bảo mật & quyền riêng tư → Kiểm soát Micro.
                """.trimIndent()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi ghi âm: ${e.message}", e)
        } finally {
            try {
                ar.stop(); ar.release()
            } catch (_: Exception) {
            }
            audioRecord = null
            Log.d(TAG, "🔓 AudioRecord released")
        }
    }

    private fun isSilent(buffer: ByteArray, length: Int): Boolean {
        var sumSq = 0.0
        val samples = length / 2
        if (samples == 0) return true
        for (i in 0 until length step 2) {
            val s = ByteBuffer.wrap(buffer, i, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
            sumSq += s.toLong() * s.toLong()
        }
        return Math.sqrt(sumSq / samples) < 50.0
    }

    private fun writeWavHeader(filePath: String, pcmBytes: Long) {
        try {
            RandomAccessFile(filePath, "rw").use { raf ->
                val byteRate = (SAMPLE_RATE * 1 * BYTES_PER_SAMPLE).toLong()
                raf.seek(0)
                raf.write("RIFF".toByteArray())
                raf.write(i32((pcmBytes + 36).toInt()))
                raf.write("WAVE".toByteArray())
                raf.write("fmt ".toByteArray())
                raf.write(i32(16))
                raf.write(i16(1))
                raf.write(i16(1))
                raf.write(i32(SAMPLE_RATE))
                raf.write(i32(byteRate.toInt()))
                raf.write(i16(BYTES_PER_SAMPLE))
                raf.write(i16(16))
                raf.write("data".toByteArray())
                raf.write(i32(pcmBytes.toInt()))
            }
            Log.d(TAG, "✅ WAV header đã được ghi ($pcmBytes bytes PCM)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi ghi WAV header: ${e.message}")
        }
    }

    private fun ensureWav(path: String) =
        if (path.endsWith(".wav")) path else path.replace(Regex("\\.[^.]+$"), ".wav")

    private fun i32(v: Int) = byteArrayOf(
        (v and 0xFF).toByte(), (v shr 8 and 0xFF).toByte(),
        (v shr 16 and 0xFF).toByte(), (v shr 24 and 0xFF).toByte()
    )

    private fun i16(v: Int) = byteArrayOf((v and 0xFF).toByte(), (v shr 8 and 0xFF).toByte())
}
