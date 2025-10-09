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

package eu.europa.ec.onboardingfeature.ui.qrscanintro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.BulletHolder
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.StickyBottomConfig
import eu.europa.ec.uilogic.component.wrap.StickyBottomType
import eu.europa.ec.uilogic.component.wrap.TextConfig
import eu.europa.ec.uilogic.component.wrap.WrapImage
import eu.europa.ec.uilogic.component.wrap.WrapStickyBottomContent
import eu.europa.ec.uilogic.component.wrap.WrapText

@Composable
fun QRScanIntroScreen(controller: NavController, viewModel: QRScanIntroViewModel) {

    val state by viewModel.viewState.collectAsStateWithLifecycle()

    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = ScreenNavigateAction.NONE,
        onBack = { viewModel.setEvent(Event.OnBackPressed) },
        stickyBottom = { paddingValues ->
            ActionButtons(
                onBack = { viewModel.setEvent(Event.OnBackPressed) },
                onStart = { viewModel.setEvent(Event.OnStartProcedure) },
                paddingValues = paddingValues
            )
        }
    ) { paddingValues ->
        Content(paddingValues = paddingValues)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            handleEffect(effect, controller)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.setEvent(Event.Init)
    }

}

@Composable
private fun Content(paddingValues: PaddingValues) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            WrapImage(
                modifier = Modifier.size(64.dp),
                iconData = AppIcons.QrScanner,
            )
        }

        VSpacer.ExtraLarge()

        WrapText(
            text = stringResource(R.string.qr_scan_intro_enrollment_method),
            textConfig = TextConfig(
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            ),
        )

        VSpacer.ExtraSmall()

        WrapText(
            text = stringResource(R.string.qr_scan_intro_title),
            textConfig = TextConfig(
                style = MaterialTheme.typography.titleLarge,
            ),
        )

        VSpacer.Large()

        WrapText(
            text = stringResource(R.string.qr_scan_intro_description),
            textConfig = TextConfig(
                style = MaterialTheme.typography.bodyLarge,
                maxLines = Int.MAX_VALUE
            ),
        )

        VSpacer.Large()

        BulletHolder(
            stringResource(R.string.qr_scan_intro_step_first),
            stringResource(R.string.qr_scan_intro_step_second)
        )
    }
}

@Composable
private fun ActionButtons(onBack: () -> Unit, onStart: () -> Unit, paddingValues: PaddingValues) {

    val buttons = StickyBottomType.TwoButtons(
        primaryButtonConfig = ButtonConfig(
            type = ButtonType.SECONDARY,
            onClick = { onBack() }
        ),
        secondaryButtonConfig = ButtonConfig(
            type = ButtonType.PRIMARY,
            onClick = { onStart() }
        )
    )

    WrapStickyBottomContent(
        stickyBottomModifier = Modifier
            .fillMaxWidth()
            .padding(paddingValues),
        stickyBottomConfig = StickyBottomConfig(type = buttons, showDivider = false)
    ) {
        when (it?.type) {
            ButtonType.PRIMARY -> Text(text = stringResource(R.string.landing_screen_primary_button_label_scan))
            ButtonType.SECONDARY -> Text(text = stringResource(R.string.passport_scan_intro_back_button))
            else -> {}
        }
    }
}

private fun handleEffect(effect: Effect, controller: NavController) {
    when (effect) {
        is Effect.Navigation.GoBack -> {
            controller.popBackStack()
        }

        is Effect.Navigation.SwitchScreen -> {
            controller.navigate(effect.screenRoute)
        }
    }
}

@Composable
@ThemeModePreviews
private fun QRScanIntroScreenPreview() {
    PreviewTheme {
        PreviewTheme {
            ContentScreen(
                isLoading = false,
                navigatableAction = ScreenNavigateAction.NONE,
                onBack = {},
                stickyBottom = { paddingValues ->
                    ActionButtons(
                        onBack = {},
                        onStart = {},
                        paddingValues = paddingValues
                    )
                }
            ) { paddingValues ->
                Content(paddingValues = paddingValues)
            }
        }
    }
}