package com.nhakhoaquangninh.telesales

import android.content.Context
import com.nhakhoaquangninh.telesales.call.CallEventCoordinator
import com.nhakhoaquangninh.telesales.call.CallLogDataSource
import com.nhakhoaquangninh.telesales.call.CallSessionTracker
import com.nhakhoaquangninh.telesales.call.ComplianceNotifier
import com.nhakhoaquangninh.telesales.call.RecordingLocator
import com.nhakhoaquangninh.telesales.call.UploadScheduler
import com.nhakhoaquangninh.telesales.data.MediaStoreRecordingRepository
import com.nhakhoaquangninh.telesales.data.local.TokenManager
import com.nhakhoaquangninh.telesales.data.repository.AuthRepositoryImpl
import com.nhakhoaquangninh.telesales.data.repository.CallRecordRepositoryImpl
import com.nhakhoaquangninh.telesales.domain.common.MessageProvider
import com.nhakhoaquangninh.telesales.domain.repository.AuthRepository
import com.nhakhoaquangninh.telesales.domain.repository.CallRecordRepository
import com.nhakhoaquangninh.telesales.domain.repository.RecordingRepository
import com.nhakhoaquangninh.telesales.domain.usecase.GetSessionUseCase
import com.nhakhoaquangninh.telesales.domain.usecase.LogoutUseCase
import com.nhakhoaquangninh.telesales.domain.usecase.RequestLogoutOtpUseCase
import com.nhakhoaquangninh.telesales.domain.usecase.RequestOtpUseCase
import com.nhakhoaquangninh.telesales.domain.usecase.UploadCallRecordUseCase
import com.nhakhoaquangninh.telesales.domain.usecase.VerifyOtpUseCase

object ServiceLocator {
    @Volatile
    private var applicationContext: Context? = null

    @Volatile
    private var tokenManager: TokenManager? = null

    @Volatile
    private var messageProvider: MessageProvider? = null

    fun init(context: Context) {
        if (applicationContext == null || tokenManager == null || messageProvider == null) {
            synchronized(this) {
                val appContext = context.applicationContext
                if (applicationContext == null) applicationContext = appContext
                if (tokenManager == null) tokenManager = TokenManager.getInstance(appContext)
                if (messageProvider == null) messageProvider = AppMessageProvider(appContext)
            }
        }
    }

    private fun requireContext(): Context = applicationContext
        ?: error("ServiceLocator chưa được khởi tạo")

    private fun requireTokenManager(): TokenManager = tokenManager
        ?: error("ServiceLocator chưa được khởi tạo")

    private fun requireMessageProvider(): MessageProvider = messageProvider
        ?: error("ServiceLocator chưa được khởi tạo")

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            tokenManager = requireTokenManager(),
            messageProvider = requireMessageProvider()
        )
    }

    val callRecordRepository: CallRecordRepository by lazy {
        CallRecordRepositoryImpl(
            context = requireContext(),
            tokenManager = requireTokenManager(),
            messageProvider = requireMessageProvider()
        )
    }

    private val recordingLocator: RecordingLocator by lazy {
        RecordingLocator(requireContext())
    }

    val recordingRepository: RecordingRepository by lazy {
        MediaStoreRecordingRepository(recordingLocator)
    }

    val uploadScheduler: UploadScheduler by lazy {
        UploadScheduler(requireContext())
    }

    val complianceNotifier: ComplianceNotifier by lazy {
        ComplianceNotifier(requireContext())
    }

    val callSessionTracker: CallSessionTracker by lazy {
        CallSessionTracker()
    }

    val callEventCoordinator: CallEventCoordinator by lazy {
        CallEventCoordinator(
            context = requireContext(),
            callLogDataSource = CallLogDataSource(requireContext()),
            recordingLocator = recordingLocator,
            uploadScheduler = uploadScheduler,
            notifier = complianceNotifier
        )
    }

    val requestOtpUseCase: RequestOtpUseCase by lazy {
        RequestOtpUseCase(authRepository)
    }

    val verifyOtpUseCase: VerifyOtpUseCase by lazy {
        VerifyOtpUseCase(authRepository)
    }

    val getSessionUseCase: GetSessionUseCase by lazy {
        GetSessionUseCase(authRepository)
    }

    val requestLogoutOtpUseCase: RequestLogoutOtpUseCase by lazy {
        RequestLogoutOtpUseCase(authRepository)
    }

    val logoutUseCase: LogoutUseCase by lazy {
        LogoutUseCase(authRepository)
    }

    val uploadCallRecordUseCase: UploadCallRecordUseCase by lazy {
        UploadCallRecordUseCase(callRecordRepository)
    }
}