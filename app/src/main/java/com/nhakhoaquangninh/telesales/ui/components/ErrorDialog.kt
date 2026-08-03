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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.Resource

@Composable
fun ErrorDialog(
    error: Resource.Error,
    onDismiss: () -> Unit
) {
    val (headerTitle, icon, badgeBgColor) = when (error.source) {
        ErrorSource.APP_CLIENT -> Triple(
            "📱 LỖI ỨNG DỤNG (APP CLIENT)",
            Icons.Default.PhoneAndroid,
            Color(0xFFC2410C)
        )
        ErrorSource.NETWORK -> Triple(
            "🌐 LỖI KẾT NỐI MẠNG (NETWORK)",
            Icons.Default.WifiOff,
            Color(0xFFD97706)
        )
        ErrorSource.SERVER -> Triple(
            "🖥️ LỖI TỪ MÁY CHỦ (SERVER ${error.code?.let { "HTTP $it" } ?: ""})",
            Icons.Default.CloudOff,
            Color(0xFFDC2626)
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
    badgeBgColor: Color = Color(0xFFDC2626),
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
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Đã hiểu",
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
                        .background(badgeBgColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
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
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = Color(0xFFE2E8F0),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        containerColor = Color(0xFF1E293B),
        shape = RoundedCornerShape(20.dp)
    )
}
