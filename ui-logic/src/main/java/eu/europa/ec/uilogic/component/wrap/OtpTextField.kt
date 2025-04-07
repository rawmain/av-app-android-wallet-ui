/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package eu.europa.ec.uilogic.component.wrap

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.OneTimeLaunchedEffect
import eu.europa.ec.uilogic.component.utils.SIZE_EXTRA_SMALL
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL_PLUS

@Composable
fun OtpTextField(
    modifier: Modifier = Modifier,
    otpText: String,
    length: Int = 6,
    onUpdate: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    pinWidth: Dp = 40.dp,
    hasError: Boolean = false,
    errorMessage: String? = null,
    focusOnCreate: Boolean = false,
) {
    LaunchedEffect(Unit) {
        if (otpText.length > length) {
            throw IllegalArgumentException("Otp text value must not have more than otpCount: $length characters")
        }
    }

    val focusRequester = FocusRequester()

    Column(modifier = modifier) {
        BasicTextField(
            modifier = modifier.focusRequester(focusRequester),
            value = TextFieldValue(otpText, selection = TextRange(otpText.length)),
            onValueChange = {
                onUpdate.invoke(it.text)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = visualTransformation,
            decorationBox = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    repeat(length) { index ->
                        CharView(
                            index = index,
                            text = visualTransformation.filter(AnnotatedString(otpText)).text.text,
                            pinWidth = pinWidth,
                            hasError = hasError,
                        )

                    }
                }
            })

        errorMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        OneTimeLaunchedEffect {
            if (focusOnCreate) {
                focusRequester.requestFocus()
            }
        }
    }

}

@Composable
private fun CharView(
    index: Int,
    text: String,
    pinWidth: Dp = 40.dp,
    hasError: Boolean = false,
) {
    val isFocused = text.length == index
    val char = when {
        index >= text.length -> ""
        else -> text[index].toString()
    }

    val borderColor = when {
        hasError -> MaterialTheme.colorScheme.error
        isFocused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    val borderWidth = when {
        isFocused -> 2.dp
        else -> 1.dp
    }
    Text(
        modifier = Modifier
            .width(pinWidth)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(SIZE_EXTRA_SMALL.dp)
            )
            .padding(vertical = SIZE_SMALL_PLUS.dp), text = char,
        style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center
    )

}


@ThemeModePreviews
@Composable
private fun PreviewOtpTextField() {
    PreviewTheme {
        OtpTextField(
            modifier = Modifier.wrapContentSize(),
            onUpdate = {},
            length = 6,
            otpText = "123456",
            visualTransformation = PasswordVisualTransformation(),
            pinWidth = 42.dp,
        )
    }
}


@ThemeModePreviews
@Composable
private fun PreviewOtpTextFieldWithError() {
    PreviewTheme {
        OtpTextField(
            modifier = Modifier.wrapContentSize(),
            onUpdate = {},
            length = 6,
            otpText = "123",
            visualTransformation = PasswordVisualTransformation(),
            pinWidth = 42.dp,
            hasError = true,
            errorMessage = "Invalid code"
        )
    }
}