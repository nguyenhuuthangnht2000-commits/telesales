package com.nhakhoaquangninh.telesales.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.nhakhoaquangninh.telesales.R
import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.theme.DangerRed
import com.nhakhoaquangninh.telesales.theme.DialogBorder
import com.nhakhoaquangninh.telesales.theme.DialogSurface
import com.nhakhoaquangninh.telesales.theme.Dimens
import com.nhakhoaquangninh.telesales.theme.WarningAmber
import com.nhakhoaquangninh.telesales.theme.WarningDark

@Composable
fun ErrorDialog(
    error: Resource.Error,
    onDismiss: () -> Unit
) {
    val (headerTitle, icon, badgeBgColor) = when (error.source) {
        ErrorSource.APP_CLIENT -> Triple(
            stringResource(R.string.error_app_header),
            Icons.Default.PhoneAndroid,
            WarningDark
        )

        ErrorSource.NETWORK -> Triple(
            stringResource(R.string.error_network_header),
            Icons.Default.WifiOff,
            WarningAmber
        )

        ErrorSource.SERVER -> Triple(
            error.code?.let { stringResource(R.string.error_server_header_with_code, it) }
                ?: stringResource(R.string.error_server_header),
            Icons.Default.CloudOff,
            DangerRed
        )
    }

    ErrorDialogContent(
        headerTitle = headerTitle,
        icon = icon,
        badgeBgColor = badgeBgColor,
        message = error.message,
        rawDetails = error.rawDetails,
        onDismiss = onDismiss
    )
}

@Composable
fun ErrorDialogContent(
    headerTitle: String,
    icon: ImageVector = Icons.Default.ErrorOutline,
    badgeBgColor: Color = DangerRed,
    message: String,
    rawDetails: String? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = badgeBgColor),
                shape = RoundedCornerShape(Dimens.Space10),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.common_understood),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(badgeBgColor, RoundedCornerShape(Dimens.Space6))
                        .padding(horizontal = Dimens.Space10, vertical = Dimens.Space5)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(Dimens.Space16)
                        )
                        Spacer(modifier = Modifier.width(Dimens.Space6))
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = Dimens.FontSize20),
                    color = DialogBorder,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        containerColor = DialogSurface,
        shape = RoundedCornerShape(Dimens.Size20)
    )
}
