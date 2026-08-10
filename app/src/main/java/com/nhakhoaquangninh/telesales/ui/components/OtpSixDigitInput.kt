package com.nhakhoaquangninh.telesales.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import com.nhakhoaquangninh.telesales.theme.Dimens
import com.nhakhoaquangninh.telesales.theme.OnSurfaceDark
import com.nhakhoaquangninh.telesales.theme.OutlineVariant
import com.nhakhoaquangninh.telesales.theme.PrimaryTeal
import com.nhakhoaquangninh.telesales.theme.SurfaceContainer

@Composable
fun OtpSixDigitInput(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var textFieldValue by remember(value) {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        )
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        // Hidden BasicTextField to capture actual keyboard inputs
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                val newText = newValue.text
                if (newText.length <= 6 && newText.all { it.isDigit() }) {
                    textFieldValue = newValue
                    if (newText != value) {
                        onValueChange(newText)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier
                .focusRequester(focusRequester)
                .size(Dimens.BorderThickness)
        )

        // Row of 6 Visual Boxes
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.Space8),
            modifier = Modifier.clickable {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        ) {
            for (i in 0 until 6) {
                val digit = value.getOrNull(i)?.toString() ?: ""
                val isFocused = value.length == i || (value.length == 6 && i == 5)

                val boxBg = if (isFocused) SurfaceContainer else SurfaceContainer.copy(alpha = 0.6f)
                val borderColor = if (isFocused) PrimaryTeal else OutlineVariant.copy(alpha = 0.5f)

                Box(
                    modifier = Modifier
                        .width(Dimens.OtpCellWidth)
                        .height(Dimens.OtpCellHeight)
                        .clip(RoundedCornerShape(topStart = Dimens.Space6, topEnd = Dimens.Space6))
                        .background(boxBg)
                        .border(
                            width = if (isFocused) Dimens.Space2 else Dimens.BorderThickness,
                            color = borderColor,
                            shape = RoundedCornerShape(
                                topStart = Dimens.Space6,
                                topEnd = Dimens.Space6
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = Dimens.FontSize22
                        ),
                        color = OnSurfaceDark
                    )
                }
            }
        }
    }
}
