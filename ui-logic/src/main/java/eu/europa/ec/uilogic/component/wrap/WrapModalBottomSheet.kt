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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.europa.ec.resourceslogic.theme.values.divider
import eu.europa.ec.resourceslogic.theme.values.warning
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.IconData
import eu.europa.ec.uilogic.component.ModalOptionUi
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.DEFAULT_ICON_SIZE
import eu.europa.ec.uilogic.component.utils.HSpacer
import eu.europa.ec.uilogic.component.utils.SIZE_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_MEDIUM
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.extension.throttledClickable
import eu.europa.ec.uilogic.mvi.ViewEvent

private val defaultBottomSheetPadding: PaddingValues = PaddingValues(
    start = SPACING_LARGE.dp,
    end = SPACING_LARGE.dp,
    top = 0.dp,
    bottom = SPACING_LARGE.dp
)

private val bottomSheetDefaultBackgroundColor: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainerLowest

private val bottomSheetDefaultTextColor: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

/**
 * Data class representing the text content for a bottom sheet.
 *
 * This class holds the title, message, and button texts for a bottom sheet.
 * It also includes flags to indicate if a button should be styled as a warning.
 *
 * @property title The title of the bottom sheet.
 * @property message The message displayed in the bottom sheet.
 * @property positiveButtonText The text for the positive button (e.g., "OK", "Confirm"). Can be null if no positive button is needed.
 * @property isPositiveButtonWarning A flag indicating if the positive button should be styled as a warning (e.g., red color). Defaults to false.
 * @property negativeButtonText The text for the negative button (e.g., "Cancel", "Dismiss"). Can be null if no negative button is needed.
 * @property isNegativeButtonWarning A flag indicating if the negative button should be styled as a warning (e.g., red color). Defaults to false.
 */
data class BottomSheetTextData(
    val title: String,
    val message: String,
    val positiveButtonText: String? = null,
    val isPositiveButtonWarning: Boolean = false,
    val negativeButtonText: String? = null,
    val isNegativeButtonWarning: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrapModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    dragHandle: @Composable (() -> Unit) = { BottomSheetDefaultHandle() },
    sheetContent: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        dragHandle = dragHandle,
        content = sheetContent,
    )
}

/**
 * A composable function that displays a dialog-style bottom sheet.
 *
 * This bottom sheet presents information to the user with optional icons,
 * title, message, and two buttons for positive and negative actions.
 *
 * @param textData Data class containing the text content for the bottom sheet. This includes
 *                 title, message, positive button text, and negative button text.
 * @param leadingIcon An optional icon to be displayed at the beginning of the title.
 * @param leadingIconTint An optional tint color for the leading icon.
 * @param onPositiveClick A lambda function to be executed when the positive button is clicked.
 * @param onNegativeClick A lambda function to be executed when the negative button is clicked.
 */
@Composable
fun DialogBottomSheet(
    textData: BottomSheetTextData,
    leadingIcon: IconData? = null,
    leadingIconTint: Color? = null,
    onPositiveClick: () -> Unit = {},
    onNegativeClick: () -> Unit = {},
) {
    BaseBottomSheet(
        textData = textData,
        leadingIcon = leadingIcon,
        leadingIconTint = leadingIconTint,
        bodyContent = {
            Row(
                modifier = Modifier.padding(vertical = SPACING_EXTRA_SMALL.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                textData.negativeButtonText?.let { safeNegativeButtonText ->
                    WrapButton(
                        modifier = Modifier.weight(1f),
                        buttonConfig = ButtonConfig(
                            type = ButtonType.SECONDARY,
                            onClick = onNegativeClick,
                            isWarning = textData.isNegativeButtonWarning,
                        )
                    ) {
                        Text(
                            text = safeNegativeButtonText,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                HSpacer.Small()

                textData.positiveButtonText?.let { safePositiveButtonText ->
                    WrapButton(
                        modifier = Modifier.weight(1f),
                        buttonConfig = ButtonConfig(
                            type = ButtonType.PRIMARY,
                            onClick = onPositiveClick,
                            isWarning = textData.isPositiveButtonWarning,
                        )
                    ) {
                        Text(
                            text = safePositiveButtonText,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    )
}

/**
 * A simple bottom sheet composable function.
 *
 * This function displays a basic bottom sheet with a title and message.
 * It can optionally include a leading icon with a custom tint.
 * It utilizes the `BaseBottomSheet` composable for its core functionality, providing a
 * standardized structure for bottom sheets.
 *
 * @param textData An object of type `BottomSheetTextData` containing the title and message
 * to be displayed in the bottom sheet.
 * @param leadingIcon An optional `IconData` object representing the icon to be displayed
 * at the leading edge of the bottom sheet.
 * @param leadingIconTint An optional `Color` to apply as a tint to the leading icon. If null,
 * the default icon color will be used.
 */
@Composable
fun SimpleBottomSheet(
    textData: BottomSheetTextData,
    leadingIcon: IconData? = null,
    leadingIconTint: Color? = null,
) {
    BaseBottomSheet(
        textData = textData,
        leadingIcon = leadingIcon,
        leadingIconTint = leadingIconTint,
    )
}

@Composable
private fun BaseBottomSheet(
    textData: BottomSheetTextData,
    leadingIcon: IconData? = null,
    leadingIconTint: Color? = null,
    bodyContent: @Composable (() -> Unit)? = null,
    sheetBackgroundColor: Color = bottomSheetDefaultBackgroundColor,
    sheetPadding: PaddingValues = defaultBottomSheetPadding,
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .background(color = sheetBackgroundColor)
            .fillMaxWidth()
            .padding(sheetPadding),
        verticalArrangement = Arrangement.spacedBy(SPACING_MEDIUM.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let { safeLeadingIcon ->
                WrapIcon(
                    modifier = Modifier.size(DEFAULT_ICON_SIZE.dp),
                    iconData = safeLeadingIcon,
                    customTint = leadingIconTint
                )
            }
            Text(
                text = textData.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = bottomSheetDefaultTextColor
                )
            )
        }

        Text(
            text = textData.message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = bottomSheetDefaultTextColor
            )
        )

        bodyContent?.let { safeBodyContent ->
            safeBodyContent()
        }
    }
}

@Composable
fun <T : ViewEvent> BottomSheetWithOptionsList(
    textData: BottomSheetTextData,
    options: List<ModalOptionUi<T>>,
    onEventSent: (T) -> Unit,
) {
    if (options.isNotEmpty()) {
        BaseBottomSheet(
            textData = textData,
            bodyContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    OptionsList(
                        optionItems = options,
                        itemSelected = onEventSent
                    )
                }
            }
        )
    }
}

@Composable
private fun <T : ViewEvent> OptionsList(
    optionItems: List<ModalOptionUi<T>>,
    itemSelected: (T) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp)
    ) {
        itemsIndexed(optionItems) { index, item ->

            OptionListItem(
                item = item,
                itemSelected = itemSelected
            )

            if (index < optionItems.lastIndex) {
                HorizontalDivider(
                    thickness = 1.dp,
                )
            }
        }
    }
}

@Composable
private fun <T : ViewEvent> OptionListItem(
    item: ModalOptionUi<T>,
    itemSelected: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SIZE_SMALL.dp))
            .background(bottomSheetDefaultBackgroundColor)
            .throttledClickable {
                itemSelected(item.event)
            }
            .padding(
                vertical = SPACING_MEDIUM.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(SPACING_SMALL.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item.leadingIcon?.let { safeLeadingIcon ->
            WrapIcon(
                modifier = Modifier.size(DEFAULT_ICON_SIZE.dp),
                iconData = safeLeadingIcon,
                customTint = item.leadingIconTint,
            )
        }

        Text(
            modifier = Modifier.weight(1f),
            text = item.title,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = bottomSheetDefaultTextColor
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        item.trailingIcon?.let { safeTrailingIcon ->
            WrapIcon(
                modifier = Modifier.size(DEFAULT_ICON_SIZE.dp),
                iconData = safeTrailingIcon,
                customTint = item.trailingIconTint,
            )
        }
    }
}

@Composable
private fun BottomSheetDefaultHandle() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bottomSheetDefaultBackgroundColor)
            .padding(vertical = SPACING_MEDIUM.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WrapIcon(
            iconData = AppIcons.HandleBar,
            customTint = MaterialTheme.colorScheme.divider
        )
    }
}

@ThemeModePreviews
@Composable
private fun BottomSheetDefaultHandlePreview() {
    PreviewTheme {
        BottomSheetDefaultHandle()
    }
}

@ThemeModePreviews
@Composable
private fun SimpleBottomSheetPreview() {
    PreviewTheme {
        SimpleBottomSheet(
            textData = BottomSheetTextData(
                title = "Title",
                message = "Message",
            )
        )
    }
}

@ThemeModePreviews
@Composable
private fun SimpleBottomSheetWithLeadingIconPreview() {
    PreviewTheme {
        SimpleBottomSheet(
            textData = BottomSheetTextData(
                title = "Title",
                message = "Message",
            ),
            leadingIcon = AppIcons.Warning,
            leadingIconTint = MaterialTheme.colorScheme.warning,
        )
    }
}

@ThemeModePreviews
@Composable
private fun DialogBottomSheetPreview() {
    PreviewTheme {
        DialogBottomSheet(
            textData = BottomSheetTextData(
                title = "Title",
                message = "Message",
                positiveButtonText = "OK",
                negativeButtonText = "Cancel"
            )
        )
    }
}

private data object DummyEventForPreview : ViewEvent

@ThemeModePreviews
@Composable
private fun BottomSheetWithOptionsListPreview() {
    PreviewTheme {
        BottomSheetWithOptionsList(
            textData = BottomSheetTextData(
                title = "Title",
                message = "Message"
            ),
            options = buildList {
                addAll(
                    listOf(
                        ModalOptionUi(
                            title = "Option with no icons",
                            event = DummyEventForPreview,
                        ),
                        ModalOptionUi(
                            title = "Option with leading icon",
                            leadingIcon = AppIcons.Verified,
                            leadingIconTint = MaterialTheme.colorScheme.primary,
                            event = DummyEventForPreview,
                        ),
                        ModalOptionUi(
                            title = "Option with leading icon",
                            trailingIcon = AppIcons.Edit,
                            trailingIconTint = MaterialTheme.colorScheme.primary,
                            event = DummyEventForPreview,
                        ),
                        ModalOptionUi(
                            title = "Option with leading and trailing icon",
                            leadingIcon = AppIcons.Add,
                            leadingIconTint = MaterialTheme.colorScheme.primary,
                            trailingIcon = AppIcons.ClockTimer,
                            trailingIconTint = MaterialTheme.colorScheme.primary,
                            event = DummyEventForPreview,
                        ),
                        ModalOptionUi(
                            title = "Option with leading and trailing icon and really really really really really long text",
                            leadingIcon = AppIcons.Add,
                            leadingIconTint = MaterialTheme.colorScheme.primary,
                            trailingIcon = AppIcons.ClockTimer,
                            trailingIconTint = MaterialTheme.colorScheme.primary,
                            event = DummyEventForPreview,
                        ),
                    )
                )
            },
            onEventSent = {}
        )
    }
}
