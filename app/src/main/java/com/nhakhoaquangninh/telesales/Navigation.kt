package com.nhakhoaquangninh.telesales

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.nhakhoaquangninh.telesales.data.local.TokenManager
import com.nhakhoaquangninh.telesales.ui.auth.LoginScreen
import com.nhakhoaquangninh.telesales.ui.auth.OtpVerifyScreen
import com.nhakhoaquangninh.telesales.ui.main.MainScreen
import com.nhakhoaquangninh.telesales.ui.main.VoIPCallScreen

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val tokenManager = TokenManager.getInstance(context)

    // Nếu đã đăng nhập thành công (có Token) -> Mở thẳng Main, ngược lại mở Login
    val startKey = if (tokenManager.isLoggedIn()) Main else Login
    val backStack = rememberNavBackStack(startKey)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<Login> {
                    LoginScreen(
                        onRequestOtpSuccess = { userId ->
                            backStack.add(OtpVerify(userId))
                        },
                        modifier = Modifier.safeDrawingPadding()
                    )
                }
                entry<OtpVerify> { key ->
                    OtpVerifyScreen(
                        userId = key.userId,
                        onVerifySuccess = {
                            backStack.removeLastOrNull() // Pop OtpVerify
                            backStack.add(Main)          // Navigate to Main
                        },
                        onBackToLogin = {
                            backStack.removeLastOrNull()
                        },
                        modifier = Modifier.safeDrawingPadding()
                    )
                }
                entry<Main> {
                    MainScreen(
                        onItemClick = { navKey -> backStack.add(navKey) },
                        modifier = Modifier
                            .safeDrawingPadding()
                            .padding(16.dp)
                    )
                }
                entry<VoIPCall> {
                    VoIPCallScreen()
                }
            },
    )
}
