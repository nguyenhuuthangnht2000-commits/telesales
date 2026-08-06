package com.nhakhoaquangninh.telesales

import android.content.Context
import com.nhakhoaquangninh.telesales.data.local.TokenManager
import com.nhakhoaquangninh.telesales.data.repository.AuthRepositoryImpl
import com.nhakhoaquangninh.telesales.data.repository.CallRecordRepositoryImpl
import com.nhakhoaquangninh.telesales.domain.common.MessageProvider
import com.nhakhoaquangninh.telesales.domain.repository.AuthRepository
import com.nhakhoaquangninh.telesales.domain.repository.CallRecordRepository
import com.nhakhoaquangninh.telesales.domain.usecase.GetSessionUseCase
import com.nhakhoaquangninh.telesales.domain.usecase.RequestOtpUseCase
import com.nhakhoaquangninh.telesales.domain.usecase.UploadCallRecordUseCase
import com.nhakhoaquangninh.telesales.domain.usecase.VerifyOtpUseCase

object ServiceLocator {

    @Volatile
    private var tokenManager: TokenManager? = null

    @Volatile
    private var messageProvider: MessageProvider? = null

    fun init(context: Context) {
        if (tokenManager == null || messageProvider == null) {
            synchronized(this) {
                if (tokenManager == null) {
                    tokenManager = TokenManager.getInstance(context)
                }
                if (messageProvider == null) {
                    messageProvider = AppMessageProvider(context.applicationContext)
                }
            }
        }
    }

    private fun requireTokenManager(): TokenManager {
        return tokenManager ?: throw IllegalStateException(
            "ServiceLocator chưa được khởi tạo! Gọi ServiceLocator.init(context) trong Application.onCreate()"
        )
    }

    private fun requireMessageProvider(): MessageProvider {
        return messageProvider ?: throw IllegalStateException(
            "ServiceLocator chưa được khởi tạo! Gọi ServiceLocator.init(context) trong Application.onCreate()"
        )
    }

    // ── Repositories ──────────────────────────────────────────────
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            tokenManager = requireTokenManager(),
            messageProvider = requireMessageProvider()
        )
    }

    val callRecordRepository: CallRecordRepository by lazy {
        CallRecordRepositoryImpl(
            tokenManager = requireTokenManager(),
            messageProvider = requireMessageProvider()
        )
    }

    // ── Use Cases ─────────────────────────────────────────────────
    val requestOtpUseCase: RequestOtpUseCase by lazy {
        RequestOtpUseCase(authRepository)
    }

    val verifyOtpUseCase: VerifyOtpUseCase by lazy {
        VerifyOtpUseCase(authRepository)
    }

    val getSessionUseCase: GetSessionUseCase by lazy {
        GetSessionUseCase(authRepository)
    }

    val uploadCallRecordUseCase: UploadCallRecordUseCase by lazy {
        UploadCallRecordUseCase(callRecordRepository)
    }
}
