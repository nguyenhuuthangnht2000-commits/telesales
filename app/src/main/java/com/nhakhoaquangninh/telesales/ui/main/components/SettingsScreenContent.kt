package com.nhakhoaquangninh.telesales.ui.main.components

import android.content.Context
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.nhakhoaquangninh.telesales.R
import com.nhakhoaquangninh.telesales.data.local.TokenManager
import com.nhakhoaquangninh.telesales.theme.ActiveEmerald
import com.nhakhoaquangninh.telesales.theme.DangerDark
import com.nhakhoaquangninh.telesales.theme.DangerRed
import com.nhakhoaquangninh.telesales.theme.Dimens
import com.nhakhoaquangninh.telesales.theme.OnSurfaceDark
import com.nhakhoaquangninh.telesales.theme.OnSurfaceMuted
import com.nhakhoaquangninh.telesales.theme.OnSurfaceVariant
import com.nhakhoaquangninh.telesales.theme.OutlineVariant
import com.nhakhoaquangninh.telesales.theme.PrimaryTeal
import com.nhakhoaquangninh.telesales.theme.SuccessContainer
import com.nhakhoaquangninh.telesales.theme.SuccessText
import com.nhakhoaquangninh.telesales.theme.SurfaceLowest
import com.nhakhoaquangninh.telesales.theme.SurfaceMuted
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nhakhoaquangninh.telesales.ui.main.SettingsViewModel
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.ui.components.OtpSixDigitInput
import com.nhakhoaquangninh.telesales.ui.components.ErrorDialog
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@Composable
fun SettingsScreenContent(
    context: Context,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val session = remember { TokenManager.getInstance(context).getSession() }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }
    var showOtpDialog by remember { mutableStateOf(false) }
    
    val requestOtpState by viewModel.requestOtpState.collectAsStateWithLifecycle()
    val verifyOtpState by viewModel.verifyOtpState.collectAsStateWithLifecycle()
    val otpInput by viewModel.otpInput.collectAsStateWithLifecycle()
    val otpError by viewModel.otpError.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(requestOtpState) {
        if (requestOtpState is Resource.Success) {
            showLogoutDialog = false
            showOtpDialog = true
            viewModel.resetRequestOtpState()
        }
    }

    LaunchedEffect(verifyOtpState) {
        if (verifyOtpState is Resource.Success) {
            showOtpDialog = false
            viewModel.resetVerifyOtpState()
            onLogoutClick()
        }
    }

    if (requestOtpState is Resource.Error) {
        ErrorDialog(
            error = requestOtpState as Resource.Error,
            onDismiss = { viewModel.resetRequestOtpState() }
        )
    }

    if (verifyOtpState is Resource.Error && (verifyOtpState as Resource.Error).source != com.nhakhoaquangninh.telesales.domain.common.ErrorSource.APP_CLIENT) {
        ErrorDialog(
            error = verifyOtpState as Resource.Error,
            onDismiss = { viewModel.resetVerifyOtpState() }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.PaddingMedium, vertical = Dimens.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimens.Space16)
    ) {
        // Màn hình Cài đặt Header
        Column {
            Text(
                text = stringResource(R.string.tab_settings),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = OnSurfaceDark
            )
            Spacer(modifier = Modifier.height(Dimens.Space4))
            Text(
                text = stringResource(R.string.settings_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )
        }

        // Thẻ Hồ sơ Nhân viên (Profile Card - Premium Header Layout)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.Space16),
            colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
            border = BorderStroke(Dimens.BorderThickness, OutlineVariant.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.BorderThickness)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.Size20)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar với chữ cái đầu
                    Surface(
                        shape = CircleShape,
                        color = PrimaryTeal,
                        modifier = Modifier.size(Dimens.Size56)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val initial = session?.userName?.take(1)?.uppercase() ?: stringResource(
                                R.string.settings_default_initial
                            )
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(Dimens.Space16))

                    // Tên và Mã nhân viên
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session?.userName
                                ?: stringResource(R.string.settings_default_name),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = Dimens.FontSize18
                            ),
                            color = OnSurfaceDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(Dimens.Space2))
                        Text(
                            text = stringResource(
                                R.string.settings_user_id_value,
                                session?.userId?.toString() ?: stringResource(R.string.settings_na)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(Dimens.Space8))
                        Surface(
                            shape = CircleShape,
                            color = SuccessContainer,
                            border = BorderStroke(
                                Dimens.BorderThickness,
                                ActiveEmerald.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = Dimens.Space10,
                                    vertical = Dimens.Space5
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(Dimens.Space6)
                                        .clip(CircleShape)
                                        .background(ActiveEmerald)
                                )
                                Spacer(modifier = Modifier.width(Dimens.Space6))
                                Text(
                                    text = stringResource(R.string.settings_shift_active),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SuccessText
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tùy chọn Cài đặt & Hỗ trợ (Settings Options Group)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.Space16),
            colors = CardDefaults.cardColors(containerColor = SurfaceLowest),
            border = BorderStroke(Dimens.BorderThickness, OutlineVariant.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.BorderThickness)
        ) {
            Column(modifier = Modifier.padding(vertical = Dimens.Space4)) {
                // Tùy chọn 1: Hướng dẫn khắc phục sự cố
                SettingOptionRow(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    title = stringResource(R.string.settings_faq_btn),
                    subtitle = "Tắt tối ưu pin & cấp quyền chạy ngầm",
                    onClick = { showFaqDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.Space8))

        // Nút Đăng xuất (Logout Button)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showLogoutDialog = true },
            shape = RoundedCornerShape(Dimens.Space16),
            colors = CardDefaults.cardColors(containerColor = DangerRed),
            border = BorderStroke(Dimens.BorderThickness, DangerDark)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.Space16, vertical = Dimens.Space16),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(Dimens.Size20)
                )
                Spacer(modifier = Modifier.width(Dimens.Space8))
                Text(
                    text = stringResource(R.string.settings_logout_btn),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.Space16))
    }

    // Dialog Xác nhận Đăng xuất
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = SurfaceLowest,
            titleContentColor = OnSurfaceDark,
            textContentColor = OnSurfaceVariant,
            title = {
                Text(
                    text = stringResource(R.string.settings_logout_dialog_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_logout_dialog_msg),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                val isLoading = requestOtpState is Resource.Loading
                Button(
                    onClick = {
                        session?.userId?.let { userId ->
                            viewModel.requestOtp(userId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(Dimens.Size20),
                            strokeWidth = Dimens.Space2
                        )
                    } else {
                        Text(
                            stringResource(R.string.settings_logout_confirm),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                Button(
                    onClick = { showLogoutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceMuted)
                ) {
                    Text(
                        stringResource(R.string.settings_logout_cancel),
                        color = OnSurfaceMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    // Dialog Hướng dẫn Khắc phục Sự cố
    if (showFaqDialog) {
        AlertDialog(
            onDismissRequest = { showFaqDialog = false },
            containerColor = SurfaceLowest,
            titleContentColor = PrimaryTeal,
            textContentColor = OnSurfaceDark,
            title = {
                Text(
                    text = stringResource(R.string.settings_faq_btn),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryTeal
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space10)) {
                    Text(
                        stringResource(R.string.settings_faq_step_battery),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        stringResource(R.string.settings_faq_step_permissions),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        stringResource(R.string.settings_faq_step_autostart),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFaqDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text(
                        stringResource(R.string.common_understood),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    // Dialog Xác thực OTP Đăng xuất
    if (showOtpDialog) {
        AlertDialog(
            onDismissRequest = { 
                showOtpDialog = false 
                viewModel.resetVerifyOtpState()
            },
            containerColor = SurfaceLowest,
            titleContentColor = OnSurfaceDark,
            textContentColor = OnSurfaceVariant,
            title = {
                Text(
                    text = stringResource(R.string.otp_security_verification),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.Space16)
                ) {
                    Text(
                        text = stringResource(R.string.settings_logout_otp_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    OtpSixDigitInput(
                        value = otpInput,
                        onValueChange = { viewModel.onOtpChanged(it) },
                        focusRequester = focusRequester,
                        onDone = {
                            keyboardController?.hide()
                            session?.userId?.let { userId -> viewModel.verifyOtp(userId) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (otpError != null) {
                        Text(
                            text = otpError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                val isLoading = verifyOtpState is Resource.Loading
                Button(
                    onClick = {
                        keyboardController?.hide()
                        session?.userId?.let { userId -> viewModel.verifyOtp(userId) }
                    },
                    enabled = !isLoading && otpInput.length == 6,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(Dimens.Size20),
                            strokeWidth = Dimens.Space2
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.settings_logout_otp_confirm),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                Button(
                    onClick = { 
                        showOtpDialog = false
                        viewModel.resetVerifyOtpState()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceMuted)
                ) {
                    Text(
                        text = stringResource(R.string.settings_logout_cancel),
                        color = OnSurfaceMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

}

@Composable
private fun SettingOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Dimens.Space16, vertical = Dimens.Space14),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = PrimaryTeal.copy(alpha = 0.08f),
            modifier = Modifier.size(Dimens.Size40)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryTeal,
                    modifier = Modifier.size(Dimens.Size20)
                )
            }
        }
        Spacer(modifier = Modifier.width(Dimens.Space14))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = OnSurfaceDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = OnSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(Dimens.Space14)
        )
    }
}

