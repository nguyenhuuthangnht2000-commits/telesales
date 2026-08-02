package com.nhakhoaquangninh.telesales

import android.content.Context
import android.os.Environment
import android.util.Log
import com.stringee.StringeeClient
import com.stringee.call.StringeeCall
import com.stringee.call.StringeeCall2
import com.stringee.exception.StringeeError
import com.stringee.listener.StatusListener
import com.stringee.listener.StringeeConnectionListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File

/**
 * Singleton quản lý toàn bộ vòng đời Stringee:
 * - Kết nối / Ngắt kết nối
 * - Thực hiện cuộc gọi đi (outbound app-to-phone)
 * - Ghi âm 2 chiều qua AudioRecorderHelper.startRecordingVoIP()
 * - Trạng thái cuộc gọi (StateFlow để UI observe)
 */
object StringeeManager : StringeeConnectionListener {

    private const val TAG = "StringeeManager"

    const val AGENT_USER_ID = "telesales_agent_01"

    private var client: StringeeClient? = null
    private var currentCall: StringeeCall? = null
    private var audioRecorder: AudioRecorderHelper? = null
    private var appContext: Context? = null

    // ── State flows ───────────────────────────────────────────────────────
    sealed class CallState {
        object Idle : CallState()
        object Connecting : CallState()
        data class Ringing(val toNumber: String) : CallState()
        data class Active(val toNumber: String) : CallState()
        data class Ended(val reason: String) : CallState()
        data class Error(val message: String) : CallState()
    }

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // ── Khởi tạo ──────────────────────────────────────────────────────────

    fun init(context: Context) {
        if (client != null) return
        appContext = context.applicationContext
        connect()
    }

    private fun connect() {
        val ctx = appContext ?: return
        val token = StringeeTokenHelper.generateToken(AGENT_USER_ID) ?: run {
            Log.e(TAG, "❌ Không tạo được token → Không kết nối Stringee")
            return
        }
        client = StringeeClient(ctx).also { c ->
            c.addConnectionListener(this@StringeeManager)
            c.connect(token)
            Log.d(TAG, "🔌 Đang kết nối Stringee userId=$AGENT_USER_ID...")
        }
    }

    // ── Thực hiện cuộc gọi đi ─────────────────────────────────────────────

    fun makeCall(toNumber: String) {
        val ctx = appContext ?: return
        val c = client
        if (c == null || !_isConnected.value) {
            _callState.value = CallState.Error("Chưa kết nối server, vui lòng thử lại")
            return
        }

        _callState.value = CallState.Connecting

        val normalizedNumber =
            if (toNumber.startsWith("0")) "+84${toNumber.substring(1)}" else toNumber

        val call = StringeeCall(c, AGENT_USER_ID, normalizedNumber)
        currentCall = call

        call.makeCall(object : StatusListener() {
            override fun onSuccess() {
                Log.d(TAG, "📞 makeCall OK → ringing tới $normalizedNumber")
                _callState.value = CallState.Ringing(toNumber)
            }
        })

        call.setCallListener(object : StringeeCall.StringeeCallListener {

            override fun onSignalingStateChange(
                call: StringeeCall?,
                state: StringeeCall.SignalingState?,
                reason: String?,
                sipCode: Int,
                sipReason: String?
            ) {
                Log.d(TAG, "📡 Signaling state=$state reason=$reason")
                when (state) {
                    StringeeCall.SignalingState.ANSWERED -> {
                        Log.d(TAG, "✅ Đối phương bắt máy → ghi âm")
                        _callState.value = CallState.Active(toNumber)
                        startRecording(ctx, toNumber)
                    }

                    StringeeCall.SignalingState.BUSY -> {
                        _callState.value = CallState.Ended("Máy bận")
                        stopRecording(ctx, toNumber, callAnswered = false)
                    }

                    StringeeCall.SignalingState.ENDED -> {
                        val wasActive = _callState.value is CallState.Active
                        _callState.value = CallState.Ended(reason ?: "Cuộc gọi kết thúc")
                        stopRecording(ctx, toNumber, callAnswered = wasActive)
                    }

                    else -> Unit
                }
            }

            override fun onError(call: StringeeCall?, code: Int, desc: String?) {
                Log.e(TAG, "❌ Call error code=$code desc=$desc")
                _callState.value = CallState.Error(desc ?: "Lỗi cuộc gọi ($code)")
                stopRecording(ctx, toNumber, callAnswered = false)
            }

            override fun onHandledOnAnotherDevice(
                call: StringeeCall?,
                state: StringeeCall.SignalingState?,
                desc: String?
            ) {
            }

            override fun onMediaStateChange(
                call: StringeeCall?,
                state: StringeeCall.MediaState?
            ) {
                Log.d(TAG, "🔊 Media state=$state")
            }

            override fun onLocalStream(call: StringeeCall?) {}
            override fun onRemoteStream(call: StringeeCall?) {}
            override fun onCallInfo(call: StringeeCall?, data: JSONObject?) {}
        })
    }

    fun hangUp() {
        currentCall?.hangup(object : StatusListener() {
            override fun onSuccess() {
                Log.d(TAG, "📵 Đã cúp máy")
            }
        })
    }

    // ── Ghi âm ───────────────────────────────────────────────────────────

    private fun startRecording(context: Context, toNumber: String) {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val fileName = "CallRecord_${toNumber}_${System.currentTimeMillis()}.wav"
        val file = File(dir, fileName)
        audioRecorder = AudioRecorderHelper()
        audioRecorder?.startRecordingVoIP(file.absolutePath)
        Log.d(TAG, "🎙️ Bắt đầu ghi âm VoIP → ${file.absolutePath}")
    }

    private fun stopRecording(context: Context, toNumber: String, callAnswered: Boolean) {
        audioRecorder?.stopRecording()
        audioRecorder = null
        if (!callAnswered) {
            Log.d(TAG, "🔇 Cuộc gọi không bắt máy → bỏ qua file")
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            dir?.listFiles { f -> f.name.contains("CallRecord_${toNumber}") }
                ?.forEach { it.delete() }
        }
    }

    // ── StringeeConnectionListener ────────────────────────────────────────

    override fun onConnectionConnected(client: StringeeClient?, isReconnected: Boolean) {
        Log.d(TAG, "✅ Stringee kết nối OK (reconnect=$isReconnected)")
        _isConnected.value = true
    }

    override fun onConnectionDisconnected(client: StringeeClient?, isReconnected: Boolean) {
        Log.w(TAG, "⚠️ Stringee mất kết nối")
        _isConnected.value = false
    }

    override fun onIncomingCall(call: StringeeCall?) {
        Log.d(TAG, "📲 Cuộc gọi đến qua Stringee (không xử lý)")
    }

    override fun onIncomingCall2(call: StringeeCall2?) {}

    override fun onConnectionError(client: StringeeClient?, error: StringeeError?) {
        Log.e(TAG, "❌ Stringee lỗi kết nối: ${error?.message}")
        _isConnected.value = false
    }

    override fun onRequestNewToken(client: StringeeClient?) {
        Log.d(TAG, "🔄 Stringee yêu cầu token mới...")
        val token = StringeeTokenHelper.generateToken(AGENT_USER_ID)
        token?.let { client?.connect(it) }
    }

    // Stringee 2.1.2: các callback này chỉ nhận String + JSONObject (không có StringeeClient param)
    override fun onCustomMessage(p0: String?, p1: JSONObject?) {}
    override fun onTopicMessage(p0: String?, p1: JSONObject?) {}
}
