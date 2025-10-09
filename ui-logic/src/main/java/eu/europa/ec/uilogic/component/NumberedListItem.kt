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

package eu.europa.ec.uilogic.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.TextConfig
import eu.europa.ec.uilogic.component.wrap.WrapText

data class NumberedListItemData(
    val title: String,
    val description: String? = null,
)

@Composable
fun NumberedListItem(
    modifier: Modifier = Modifier,
    item: NumberedListItemData,
    number: Int,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        WrapText(
            text = "$number.",
            textConfig = TextConfig(
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            ).copy(
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            ),
            modifier = Modifier.padding(end = 8.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            WrapText(
                text = item.title,
                textConfig = TextConfig(
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = Int.MAX_VALUE
                ).copy(
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            )

            item.description?.let { description ->
                WrapText(
                    text = description,
                    textConfig = TextConfig(
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = Int.MAX_VALUE
                    )
                )
            }
        }
    }
}

@Composable
fun NumberedList(
    modifier: Modifier = Modifier,
    items: List<NumberedListItemData>,
) {
    Column(modifier = modifier) {
        items.forEachIndexed { index, item ->
            NumberedListItem(item = item, number = index + 1)
            if (index < items.size - 1) {
                VSpacer.Medium()
            }
        }
    }
}

@ThemeModePreviews
@Composable
private fun NumberedListItemPreview() {
    PreviewTheme {
        NumberedListItem(
            item = NumberedListItemData(
                title = "Take a Photo of the Passport",
                description = "Capture a clear image of your Passport."
            ),
            number = 1
        )
    }
}

@ThemeModePreviews
@Composable
private fun NumberedListPreview() {
    PreviewTheme {
        NumberedList(
            items = listOf(
                NumberedListItemData(
                    title = "Take a Photo of the Passport",
                    description = "Capture a clear image of your Passport."
                ),
                NumberedListItemData(
                    title = "Read in your Passport",
                    description = "Place your Passport on your smartphone to read the data."
                ),
                NumberedListItemData(
                    title = "Consent"
                )
            )
        )
    }
}