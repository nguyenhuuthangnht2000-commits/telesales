package com.nhakhoaquangninh.telesales.core

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

abstract class BaseViewModel : ViewModel() {

    private val _unauthorizedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val unauthorizedEvent: SharedFlow<Unit> = _unauthorizedEvent.asSharedFlow()

    /**
     * Launch a coroutine with automatic exception categorization:
     * - UnknownHostException, SocketTimeoutException, IOException → NETWORK error
     * - Other Exception → APP_CLIENT error
     */
    protected fun launchSafe(
        onError: (Resource.Error) -> Unit,
        block: suspend CoroutineScope.() -> Unit
    ) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnknownHostException) {
                Log.e(TAG, "Lỗi mạng: không tìm thấy máy chủ")
                onError(
                    Resource.Error(
                        message = "Không tìm thấy địa chỉ máy chủ. Vui lòng kiểm tra Wifi/4G!",
                        source = ErrorSource.NETWORK,
                        rawDetails = e.localizedMessage
                    )
                )
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Lỗi mạng: quá thời gian phản hồi")
                onError(
                    Resource.Error(
                        message = "Quá thời gian phản hồi từ máy chủ (Timeout). Vui lòng thử lại!",
                        source = ErrorSource.NETWORK,
                        rawDetails = e.localizedMessage
                    )
                )
            } catch (e: IOException) {
                Log.e(TAG, "Lỗi mạng: kết nối bị gián đoạn")
                onError(
                    Resource.Error(
                        message = "Đường truyền internet bị gián đoạn.",
                        source = ErrorSource.NETWORK,
                        rawDetails = e.localizedMessage
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi xử lý phía ứng dụng")
                onError(
                    Resource.Error(
                        message = "Lỗi hệ thống: ${e.localizedMessage ?: "Lỗi ngoại lệ"}",
                        source = ErrorSource.APP_CLIENT,
                        rawDetails = e.stackTraceToString()
                    )
                )
            }
        }
    }

    protected fun triggerUnauthorized() {
        _unauthorizedEvent.tryEmit(Unit)
    }

    companion object {
        private const val TAG = "BaseViewModel"
    }
}
