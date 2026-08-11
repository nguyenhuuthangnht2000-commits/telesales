package com.nhakhoaquangninh.telesales.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhakhoaquangninh.telesales.R
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.theme.BackgroundLight
import com.nhakhoaquangninh.telesales.theme.Dimens
import com.nhakhoaquangninh.telesales.theme.OnPrimaryContainer
import com.nhakhoaquangninh.telesales.theme.OnSurfaceDark
import com.nhakhoaquangninh.telesales.theme.OnSurfaceVariant
import com.nhakhoaquangninh.telesales.theme.OutlineVariant
import com.nhakhoaquangninh.telesales.theme.PrimaryContainer
import com.nhakhoaquangninh.telesales.theme.PrimaryTeal
import com.nhakhoaquangninh.telesales.theme.SurfaceLow
import com.nhakhoaquangninh.telesales.theme.SurfaceLowest
import com.nhakhoaquangninh.telesales.ui.components.ErrorDialog

@Composable
fun LoginScreen(
    onRequestOtpSuccess: (userId: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel()
) {
    val userIdInput by viewModel.userIdInput.collectAsStateWithLifecycle()
    val userIdError by viewModel.userIdError.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Tự động chuyển sang màn hình nhập OTP khi gửi yêu cầu OTP thành công
    LaunchedEffect(uiState) {
        if (uiState is Resource.Success) {
            val userIdInt = userIdInput.trim().toIntOrNull() ?: 0
            viewModel.resetState()
            viewModel.onUserIdChanged("")
            onRequestOtpSuccess(userIdInt)
        }
    }

    // Popup Error Dialog
    if (uiState is Resource.Error) {
        ErrorDialog(
            error = uiState as Resource.Error,
            onDismiss = { viewModel.resetState() }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.Space16),
        contentAlignment = Alignment.Center
    ) {
        // Subtle Teal Background Orbs (Decorative)
        Box(
            modifier = Modifier
                .size(Dimens.AuthDecorSmall)
                .align(Alignment.TopStart)
                .background(
                    color = PrimaryTeal.copy(alpha = 0.05f),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(Dimens.AuthDecorLarge)
                .align(Alignment.BottomEnd)
                .background(
                    color = PrimaryContainer.copy(alpha = 0.05f),
                    shape = CircleShape
                )
        )

        // Auth Card (Matching code.html structure & DESIGN.md specification)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = Dimens.LoginMaxWidth),
            shape = RoundedCornerShape(Dimens.Space16),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceLowest
            ),
            border = BorderStroke(Dimens.BorderThickness, OutlineVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.Space6)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // ── Card Header (Medical Logo & Title) ─────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceLowest)
                        .padding(
                            top = Dimens.Size28,
                            bottom = Dimens.Size20,
                            start = Dimens.Space24,
                            end = Dimens.Space24
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_round),
                        contentDescription = stringResource(R.string.login_dental_icon_desc),
                        modifier = Modifier
                            .size(Dimens.Size72)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(Dimens.Space16))

                    Text(
                        text = stringResource(R.string.login_brand),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = Dimens.FontSize24
                        ),
                        color = PrimaryTeal,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(Dimens.Space4))

                    Text(
                        text = stringResource(R.string.login_portal_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                HorizontalDivider(color = OutlineVariant.copy(alpha = 0.3f))

                // ── Card Body (Form & Action) ──────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.Space24)
                ) {
                    // Input Field: Employee ID / User ID
                    OutlinedTextField(
                        value = userIdInput,
                        onValueChange = { viewModel.onUserIdChanged(it) },
                        label = { Text(stringResource(R.string.login_employee_id_label)) },
                        placeholder = { Text(stringResource(R.string.login_employee_id_placeholder)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = OnSurfaceVariant
                            )
                        },
                        isError = userIdError != null,
                        supportingText = if (userIdError != null) {
                            { Text(text = userIdError ?: "") }
                        } else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                viewModel.requestOtp()
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = OutlineVariant,
                            focusedLabelColor = PrimaryTeal,
                            unfocusedLabelColor = OnSurfaceVariant,
                            focusedTextColor = OnSurfaceDark,
                            unfocusedTextColor = OnSurfaceDark,
                            cursorColor = PrimaryTeal,
                            focusedContainerColor = BackgroundLight,
                            unfocusedContainerColor = BackgroundLight
                        ),
                        shape = RoundedCornerShape(Dimens.Space12),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(Dimens.Space16))

                    Text(
                        text = stringResource(R.string.login_otp_delivery_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(Dimens.Size20))

                    // Primary Action Button: Request OTP
                    val isLoading = uiState is Resource.Loading

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.requestOtp()
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.PrimaryButtonHeight),
                        shape = RoundedCornerShape(Dimens.CornerRadiusPill), // Pill Shape per DESIGN.md
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryTeal,
                            contentColor = Color.White,
                            disabledContainerColor = OutlineVariant
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = Dimens.Space2)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(Dimens.Space24),
                                strokeWidth = Dimens.ProgressStrokeWidth
                            )
                            Spacer(modifier = Modifier.width(Dimens.Space10))
                            Text(
                                text = stringResource(R.string.login_connecting),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.login_request_otp),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = Dimens.FontSize16
                                    ),
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
                }

                // ── Card Footer ────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceLow)
                        .padding(Dimens.Space16),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.login_support_prompt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(Dimens.Space4))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.login_support_contact),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = PrimaryTeal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Spacer(modifier = Modifier.width(Dimens.Space4))

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(Dimens.Space16)
                            )
                        }
                    }
                }
            }
        }
    }
}

