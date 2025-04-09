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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.HSpacer
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL_PLUS

data class CheckboxWithTextData(
    val isChecked: Boolean,
    val enabled: Boolean = true,
    val onCheckedChange: ((Boolean) -> Unit)? = null,
    val text: String,
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrapCheckboxWithText(
    checkboxData: CheckboxWithTextData, modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalRippleConfiguration provides BaseRippleConfiguration) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .toggleable(
                    value = checkboxData.isChecked,
                    onValueChange = checkboxData.onCheckedChange ?: {},
                    role = Role.Checkbox,
                )
                .padding(vertical = SPACING_SMALL_PLUS.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Checkbox(
                checked = checkboxData.isChecked,
                onCheckedChange = null // null recommended for accessibility with screenreaders
            )

            HSpacer.Small()

            val textConfig = TextConfig(
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                maxLines = 4
            )
            WrapText(text = checkboxData.text, textConfig = textConfig)
        }
    }
}

@ThemeModePreviews
@Composable
private fun CheckboxWithTextPreview() {
    var isChecked by remember {
        mutableStateOf(true)
    }
    val checkBoxData = CheckboxWithTextData(
        isChecked = isChecked,
        onCheckedChange = {
            isChecked = it
        },
        text = "Checkbox with text"
    )
    val uncheckedBoxData = CheckboxWithTextData(
        isChecked = false,
        onCheckedChange = {},
        text = "Unchecked Checkbox with text"
    )

    PreviewTheme {
        Column {
            WrapCheckboxWithText(checkboxData = checkBoxData)
            WrapCheckboxWithText(checkboxData = uncheckedBoxData)
        }
    }
}