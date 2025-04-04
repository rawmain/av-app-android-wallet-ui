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

package eu.europa.ec.onboardingfeature.ui.consent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.TopStepBar
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_LARGE
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.CheckboxWithTextData
import eu.europa.ec.uilogic.component.wrap.StickyBottomConfig
import eu.europa.ec.uilogic.component.wrap.StickyBottomType
import eu.europa.ec.uilogic.component.wrap.TextConfig
import eu.europa.ec.uilogic.component.wrap.WrapCheckboxWithText
import eu.europa.ec.uilogic.component.wrap.WrapLink
import eu.europa.ec.uilogic.component.wrap.WrapLinkData
import eu.europa.ec.uilogic.component.wrap.WrapStickyBottomContent
import eu.europa.ec.uilogic.component.wrap.WrapText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach

@Composable
fun ConsentScreen(navController: NavController, viewModel: ConsentViewModel) {
    val state = viewModel.viewState.collectAsStateWithLifecycle()

    val tosCheckBoxData = CheckboxWithTextData(
        isChecked = state.value.tosAccepted,
        onCheckedChange = { viewModel.setEvent(Event.TosSelected) },
        text = stringResource(R.string.consent_screen_tos_checkbox),
    )

    val dataProtectionCheckBoxData = CheckboxWithTextData(
        isChecked = state.value.dataProtectionAccepted,
        onCheckedChange = { viewModel.setEvent(Event.DataProtectionSelected) },
        text = stringResource(R.string.consent_screen_data_protection_checkbox),
    )

    val config = ButtonConfig(
        type = ButtonType.PRIMARY,
        onClick = { viewModel.setEvent(Event.GoNext) },
        enabled = state.value.isButtonEnabled
    )

    ContentScreen(
        isLoading = false,
        navigatableAction = ScreenNavigateAction.NONE,
        onBack = { viewModel.setEvent(Event.GoBack) },
        stickyBottom = { paddingValues ->
            WrapStickyBottomContent(
                stickyBottomModifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues),
                stickyBottomConfig = StickyBottomConfig(
                    type = StickyBottomType.OneButton(config = config), showDivider = false
                )
            ) {
                Text(text = stringResource(R.string.consent_screen_confirm_button))
            }
        }) { paddingValues ->
        Content(
            paddingValues = paddingValues,
            tosCheckBoxData = tosCheckBoxData,
            dataProtectionCheckBoxData = dataProtectionCheckBoxData,
            effectFlow = viewModel.effect,
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(navigationEffect, navController)
            })
    }
}

@Composable
private fun Content(
    paddingValues: PaddingValues,
    tosCheckBoxData: CheckboxWithTextData,
    dataProtectionCheckBoxData: CheckboxWithTextData,
    effectFlow: Flow<Effect>,
    onNavigationRequested: (Effect.Navigation) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(paddingValues)
    ) {
        TopStepBar(currentStep = 1)
        ConsentAndTosSection(
            tosCheckBoxData = tosCheckBoxData,
            dataProtectionCheckBoxData = dataProtectionCheckBoxData
        )
    }

    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation -> onNavigationRequested(effect)
            }
        }.collect()
    }
}

private fun handleNavigationEffect(
    navigationEffect: Effect.Navigation,
    navController: NavController
) {
    when (navigationEffect) {
        is Effect.Navigation.SwitchScreen -> {
            navController.navigate(navigationEffect.screenRoute)
        }
        is Effect.Navigation.Pop -> {
            navController.popBackStack()
        }
    }
}

@Composable
fun ConsentAndTosSection(
    tosCheckBoxData: CheckboxWithTextData, dataProtectionCheckBoxData: CheckboxWithTextData
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 0.dp, vertical = SPACING_EXTRA_LARGE.dp),
    ) {
        WrapText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.consent_screen_title),
            textConfig = TextConfig(
                style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Start
            )
        )

        VSpacer.XXLarge()

        WrapCheckboxWithText(checkboxData = tosCheckBoxData)
        WrapCheckboxWithText(checkboxData = dataProtectionCheckBoxData)

        VSpacer.Small()

        WrapLink(
            data = WrapLinkData(textId = R.string.consent_screen_tos_button),
            onClick = { /* Handle link click */ },
        )
        VSpacer.Small()
        WrapLink(
            data = WrapLinkData(textId = R.string.consent_screen_data_protection_button),
            onClick = { /* Handle link click */ },
        )
    }
}


@ThemeModePreviews
@Composable
private fun ContentPreview() {
    PreviewTheme {
        val tosCheckBoxData = CheckboxWithTextData(
            isChecked = true,
            onCheckedChange = {},
            text = "I accept the Terms of Service",
        )

        val dataProtectionCheckBoxData = CheckboxWithTextData(
            isChecked = false,
            onCheckedChange = {},
            text = "I accept the Data Protection Information",
        )

        Content(
            paddingValues = PaddingValues(0.dp),
            tosCheckBoxData = tosCheckBoxData,
            dataProtectionCheckBoxData = dataProtectionCheckBoxData,
            effectFlow = flowOf(),
            onNavigationRequested = {})
    }
}
