package com.nhakhoaquangninh.telesales.ui.main.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nhakhoaquangninh.telesales.R
import com.nhakhoaquangninh.telesales.theme.Dimens

@Composable
fun LocationPermissionNotice(needsSetup: Boolean, onRequestSetup: () -> Unit) {
    if (!needsSetup) return
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.fillMaxWidth().padding(Dimens.Space12)) {
            Text(stringResource(R.string.location_notice), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRequestSetup) {
                Text(stringResource(R.string.location_setup))
            }
        }
    }
}
