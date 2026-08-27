package com.nhakhoaquangninh.telesales.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhakhoaquangninh.telesales.R
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.theme.BackgroundLight
import com.nhakhoaquangninh.telesales.theme.Dimens
import com.nhakhoaquangninh.telesales.theme.OnSurfaceDark
import com.nhakhoaquangninh.telesales.theme.OnSurfaceVariant
import com.nhakhoaquangninh.telesales.theme.OutlineVariant
import com.nhakhoaquangninh.telesales.theme.PrimaryTeal
import com.nhakhoaquangninh.telesales.theme.SecondaryTurquoise
import com.nhakhoaquangninh.telesales.theme.SurfaceContainer
import com.nhakhoaquangninh.telesales.theme.SurfaceLowest
import com.nhakhoaquangninh.telesales.ui.components.ErrorDialog
import com.nhakhoaquangninh.telesales.ui.components.OtpSixDigitInput
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun OtpVerifyScreen(
    userId: Int,
    onVerifySuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OtpVerifyViewModel = viewModel()
) {
    val otpInput by viewModel.otpInput.collectAsStateWithLifecycle()
    val otpError by viewModel.otpError.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    val resendState by viewModel.resendState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.clearInput()
    }

    // Countdown Timer for Resend
    var timeLeft by remember { mutableIntStateOf(45) }
    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            delay(1000L.milliseconds)
            timeLeft--
        }
    }

    // Auto navigate on success
    LaunchedEffect(uiState) {
        if (uiState is Resource.Success) {
            viewModel.resetState()
            onVerifySuccess()
        }
    }

    LaunchedEffect(resendState) {
        if (resendState is Resource.Success) {
            timeLeft = 45
            viewModel.resetResendState()
        }
    }

    // Error Dialog
    if (uiState is Resource.Error) {
        ErrorDialog(
            error = uiState as Resource.Error,
            onDismiss = {
                viewModel.resetState()
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        )
    }

    if (resendState is Resource.Error) {
        ErrorDialog(
            error = resendState as Resource.Error,
            onDismiss = {
                viewModel.resetResendState()
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.PaddingMedium),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = Dimens.OtpMaxWidth),
            shape = RoundedCornerShape(Dimens.Space16),
            colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
            border = BorderStroke(Dimens.BorderThickness, OutlineVariant.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.Space2)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Decorative Gradient Line at top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.Space4)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(PrimaryTeal, SecondaryTurquoise)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.Space24),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Lock Icon inside Circle
                    Surface(
                        shape = CircleShape,
                        color = SurfaceContainer,
                        modifier = Modifier.size(Dimens.IconSizeAuth)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(Dimens.Size32)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimens.Space16))

                    // Title
                    Text(
                        text = stringResource(R.string.otp_security_verification),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = Dimens.FontSize24
                        ),
                        color = OnSurfaceDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(Dimens.Space6))

                    // Subtitle
                    Text(
                        text = stringResource(R.string.otp_enter_code_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(Dimens.Space12))

                    // Email Badge Chip
                    Surface(
                        shape = CircleShape,
                        color = SurfaceContainer.copy(alpha = 0.6f),
                        border = BorderStroke(
                            Dimens.BorderThickness,
                            OutlineVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = Dimens.Space14,
                                vertical = Dimens.Space6
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mail,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(Dimens.Space16)
                            )
                            Spacer(modifier = Modifier.width(Dimens.Space6))
                            Text(
                                text = stringResource(
                                    R.string.otp_sent_to_user,
                                    stringResource(R.string.otp_sent_to_email),
                                    userId
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimens.Space24))

                    // 6-Digit OTP Input
                    OtpSixDigitInput(
                        value = otpInput,
                        onValueChange = { viewModel.onOtpChanged(it) },
                        focusRequester = focusRequester,
                        onDone = {
                            keyboardController?.hide()
                            viewModel.verifyOtp(userId, context.applicationContext)
                        }
                    )

                    if (otpError != null) {
                        Spacer(modifier = Modifier.height(Dimens.Space8))
                        Text(
                            text = otpError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(Dimens.Space24))

                    // Verify & Start Button
                    val isLoading = uiState is Resource.Loading

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.verifyOtp(userId, context.applicationContext)
                        },
                        enabled = !isLoading && otpInput.length == 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.PrimaryButtonHeight),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryTeal,
                            contentColor = Color.White,
                            disabledContainerColor = OutlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(Dimens.Space24),
                                strokeWidth = Dimens.ProgressStrokeWidth
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.otp_verify_and_start),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(Dimens.Space8))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(Dimens.Size20)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimens.Size20))

                    // Timer & Resend
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.otp_didnt_receive),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(Dimens.Space4))
                        if (timeLeft > 0) {
                            Text(
                                text = stringResource(R.string.otp_resend_seconds, timeLeft),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = OnSurfaceVariant
                            )
                        } else if (resendState is Resource.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(Dimens.Space24),
                                color = PrimaryTeal,
                                strokeWidth = Dimens.ProgressStrokeWidth
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.otp_resend_now),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryTeal,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clickable {
                                        viewModel.resendOtp(userId)
                                    }
                                    .padding(Dimens.Space12)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimens.Size20))
                    HorizontalDivider(color = OutlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(Dimens.Space16))

                    // Return to Login Link
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimens.Space8))
                            .clickable {
                                viewModel.resetState()
                                onBackToLogin()
                            }
                            .padding(horizontal = Dimens.Space12, vertical = Dimens.Space8),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(Dimens.Size18)
                        )
                        Spacer(modifier = Modifier.width(Dimens.Space6))
                        Text(
                            text = stringResource(R.string.otp_return_to_login),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = PrimaryTeal
                        )
                    }
                }
            }
        }
    }
}


