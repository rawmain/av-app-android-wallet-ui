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

package eu.europa.ec.onboardingfeature.ui.passport.passportlivevideo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.onboardingfeature.ui.passport.passportlivevideo.Effect.Navigation
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.BulletHolder
import eu.europa.ec.uilogic.component.PassportVerificationStepBar
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.LifecycleEffect
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.StickyBottomConfig
import eu.europa.ec.uilogic.component.wrap.StickyBottomType
import eu.europa.ec.uilogic.component.wrap.TextConfig
import eu.europa.ec.uilogic.component.wrap.WrapStickyBottomContent
import eu.europa.ec.uilogic.component.wrap.WrapText
import eu.europa.ec.uilogic.extension.getPendingDeepLink
import eu.europa.ec.uilogic.navigation.OnboardingScreens
import eu.europa.ec.uilogic.navigation.helper.handleDeepLinkAction
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun PassportLiveVideoScreen(
    controller: NavController,
    viewModel: PassportLiveVideoViewModel
) {

    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = ScreenNavigateAction.NONE,
        onBack = { viewModel.setEvent(Event.OnBackPressed) },
        contentErrorConfig = state.error,
        stickyBottom = { paddingValues ->
            ActionButtons(
                paddings = paddingValues,
                onBack = { viewModel.setEvent(Event.OnBackPressed) },
                onLiveVideo = { viewModel.setEvent(Event.OnLiveVideoCapture(context)) }
            )
        }
    ) { paddingValues -> Content(paddingValues = paddingValues) }

    LaunchedEffect(Unit) {
        viewModel.effect.onEach { effect ->
            handleEffect(effect, controller)
        }.collect()
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_PAUSE
    ) {
        viewModel.setEvent(Event.OnPause)
    }

    LifecycleEffect(
        lifecycleOwner = LocalLifecycleOwner.current,
        lifecycleEvent = Lifecycle.Event.ON_RESUME
    ) {
        viewModel.setEvent(Event.Init(context.getPendingDeepLink()))
    }
}

private fun handleEffect(
    effect: Effect,
    controller: NavController,
) {
    when (effect) {
        is Navigation.GoBack -> controller.popBackStack()

        is Navigation.SwitchScreen -> {
            android.util.Log.i(
                "PassportLiveVideoScreen",
                "Navigating to: ${effect.screenRoute}, inclusive: ${effect.inclusive}"
            )
            controller.navigate(effect.screenRoute) {
                popUpTo(OnboardingScreens.PassportLiveVideo.screenRoute) {
                    inclusive = effect.inclusive
                }
            }
        }

        is Navigation.OpenDeepLinkAction -> {
            android.util.Log.i(
                "PassportLiveVideoScreen",
                "Handling deeplink: ${effect.deepLinkUri}"
            )
            handleDeepLinkAction(
                controller,
                effect.deepLinkUri,
                effect.arguments
            )
        }
    }
}

@Composable
private fun ActionButtons(onBack: () -> Unit, onLiveVideo: () -> Unit, paddings: PaddingValues) {

    val buttons = StickyBottomType.TwoButtons(
        primaryButtonConfig = ButtonConfig(
            type = ButtonType.SECONDARY,
            onClick = onBack
        ),
        secondaryButtonConfig = ButtonConfig(
            type = ButtonType.PRIMARY,
            onClick = onLiveVideo
        )
    )

    WrapStickyBottomContent(
        stickyBottomModifier = Modifier
            .fillMaxWidth()
            .padding(paddings),
        stickyBottomConfig = StickyBottomConfig(type = buttons, showDivider = false)
    ) { buttonConfigs ->
        buttonConfigs?.let { buttonConfig ->
            when (buttonConfig.type) {
                ButtonType.PRIMARY -> Text(stringResource(R.string.passport_live_video_live_capture))
                ButtonType.SECONDARY -> Text(stringResource(R.string.passport_live_video_back))
            }
        }
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

        PassportVerificationStepBar(2)

        VSpacer.ExtraLarge()
        WrapText(
            text = stringResource(R.string.passport_live_video_header),
            textConfig = TextConfig(
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            ),
        )

        VSpacer.ExtraLarge()
        WrapText(
            text = stringResource(R.string.passport_live_video_description),
            textConfig = TextConfig(
                style = MaterialTheme.typography.bodyLarge
            )
        )

        VSpacer.Large()
        BulletHolder(
            stringResource(R.string.passport_live_video_step_first),
            stringResource(R.string.passport_live_video_step_second),
            stringResource(R.string.passport_live_video_step_third)
        )

        VSpacer.Large()
        WrapText(
            text = stringResource(R.string.passport_live_video_footer),
            textConfig = TextConfig(
                style = MaterialTheme.typography.bodyLarge,
                maxLines = Int.MAX_VALUE
            )
        )
    }
}

@Composable
@ThemeModePreviews
private fun PassportLiveVideoScreenPreview() {
    PreviewTheme {
        ContentScreen(
            isLoading = false,
            navigatableAction = ScreenNavigateAction.NONE,
            onBack = {},
            stickyBottom = { paddingValues ->
                ActionButtons(
                    paddings = paddingValues,
                    onBack = {},
                    onLiveVideo = {}
                )
            }
        ) { paddingValues -> Content(paddingValues = paddingValues) }
    }
}
