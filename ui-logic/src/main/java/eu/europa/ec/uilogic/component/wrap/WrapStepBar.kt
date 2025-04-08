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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SIZE_EXTRA_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL_PLUS

private val paddingValues = PaddingValues(
    horizontal = SPACING_SMALL_PLUS.dp, vertical = SPACING_EXTRA_SMALL.dp
)

private const val inactiveTextColorAlpha = 0.35f

@Composable
fun WrapStepBar(currentStep: Int, steps: List<String>, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth(),
        shape = buttonsShape,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = SIZE_EXTRA_SMALL.dp),
        colors = CardDefaults.elevatedCardColors(
            MaterialTheme.colorScheme.background,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            steps.forEachIndexed { index, text ->
                Label(text, index, currentStep)
            }
        }
    }
}

@Composable
private fun Label(text: String, index: Int, currentStep: Int) {
    val isActive = index == currentStep
    val textColor = getColor(isActive, inactiveTextColorAlpha)

    Text(
        modifier = Modifier.padding(paddingValues),
        text = text,
        color = textColor,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun getColor(isActive: Boolean, inactiveAlpha: Float) =
    if (isActive) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurface.copy(alpha = inactiveAlpha)


@Composable
@ThemeModePreviews
fun StartupProgressBarPreview() {
    PreviewTheme {
        WrapStepBar(
            currentStep = 1, steps = listOf("Step 1", "Step 2", "Step 3")
        )
    }
}
