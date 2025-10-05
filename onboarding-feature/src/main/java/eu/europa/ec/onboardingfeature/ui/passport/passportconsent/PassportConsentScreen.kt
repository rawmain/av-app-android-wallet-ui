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

package eu.europa.ec.onboardingfeature.ui.passport.passportconsent

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import eu.europa.ec.corelogic.util.CoreActions
import eu.europa.ec.onboardingfeature.ui.passport.passportconsent.Effect.Navigation
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.uilogic.component.PassportVerificationStepBar
import eu.europa.ec.uilogic.component.content.BroadcastAction
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.LifecycleEffect
import eu.europa.ec.uilogic.component.utils.SIZE_EXTRA_LARGE
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
import eu.europa.ec.uilogic.extension.getPendingDeepLink
import eu.europa.ec.uilogic.navigation.OnboardingScreens
import eu.europa.ec.uilogic.navigation.helper.handleDeepLinkAction
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun PassportConsentScreen(
    controller: NavController,
    viewModel: PassportConsentViewModel,
) {

    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = ScreenNavigateAction.NONE,
        onBack = { viewModel.setEvent(Event.OnBackPressed) },
        contentErrorConfig = state.error,
        broadcastAction = BroadcastAction(
            intentFilters = listOf(
                CoreActions.VCI_RESUME_ACTION,
                CoreActions.VCI_DYNAMIC_PRESENTATION
            ),
            callback = {
                when (it?.action) {
                    CoreActions.VCI_RESUME_ACTION -> it.extras?.getString("uri")?.let { link ->
                        viewModel.setEvent(Event.OnResumeIssuance(link))
                    }

                    CoreActions.VCI_DYNAMIC_PRESENTATION -> it.extras?.getString("uri")
                        ?.let { link ->
                            viewModel.setEvent(Event.OnDynamicPresentation(link))
                        }
                }
            }
        ),
        stickyBottom = { paddingValues ->
            ActionButtons(
                paddings = paddingValues,
                onReject = { viewModel.setEvent(Event.OnBackPressed) },
                onConsent = { viewModel.setEvent(Event.OnConsentClicked(context)) },
                isConsentEnabled = state.isConsentChecked
            )
        }
    ) { paddingValues ->
        Content(
            paddingValues = paddingValues,
            isConsentChecked = state.isConsentChecked,
            onConsentChecked = { viewModel.setEvent(Event.OnConsentChecked(it)) },
            onMoreInfoClicked = { viewModel.setEvent(Event.OnMoreInfoClicked) }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.effect.onEach { effect ->
            handleEffect(effect, controller, context)
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
    context: Context,
) {
    when (effect) {
        is Navigation.GoBack -> controller.popBackStack()

        is Navigation.SwitchScreen -> {
            controller.navigate(effect.screenRoute) {
                popUpTo(OnboardingScreens.PassportConsent.screenRoute) {
                    inclusive = effect.inclusive
                }
            }
        }

        is Navigation.OpenDeepLinkAction -> {
            handleDeepLinkAction(
                controller,
                effect.deepLinkUri,
                effect.arguments
            )
        }

        is Navigation.OpenExternalLink -> {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(effect.url))
            context.startActivity(intent)
        }
    }
}

@Composable
private fun ActionButtons(
    onReject: () -> Unit,
    onConsent: () -> Unit,
    isConsentEnabled: Boolean,
    paddings: PaddingValues,
) {

    val buttons = StickyBottomType.TwoButtons(
        primaryButtonConfig = ButtonConfig(
            type = ButtonType.SECONDARY,
            onClick = onReject
        ),
        secondaryButtonConfig = ButtonConfig(
            type = ButtonType.PRIMARY,
            onClick = onConsent,
            enabled = isConsentEnabled
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
                ButtonType.PRIMARY -> Text(stringResource(R.string.passport_consent_consent_button))
                ButtonType.SECONDARY -> Text(stringResource(R.string.passport_consent_reject_button))
            }
        }
    }
}

@Composable
private fun Content(
    paddingValues: PaddingValues,
    isConsentChecked: Boolean,
    onConsentChecked: (Boolean) -> Unit,
    onMoreInfoClicked: () -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {

        PassportVerificationStepBar(3)

        VSpacer.ExtraLarge()
        WrapText(
            text = stringResource(R.string.passport_consent_title),
            textConfig = TextConfig(
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            ),
        )

        VSpacer.Custom(SIZE_EXTRA_LARGE)
        WrapText(
            text = stringResource(R.string.passport_consent_description),
            textConfig = TextConfig(
                style = MaterialTheme.typography.bodyLarge,
                maxLines = Int.MAX_VALUE
            )
        )

        VSpacer.Large()
        WrapLink(
            data = WrapLinkData(
                textId = R.string.passport_consent_more_info,
                isExternal = true
            ),
            onClick = onMoreInfoClicked
        )

        VSpacer.Custom(SIZE_EXTRA_LARGE)
        WrapCheckboxWithText(
            checkboxData = CheckboxWithTextData(
                isChecked = isConsentChecked,
                onCheckedChange = onConsentChecked,
                text = stringResource(R.string.passport_consent_checkbox)
            )
        )
    }
}

@Composable
@ThemeModePreviews
private fun PassportConsentScreenPreview() {
    PreviewTheme {
        ContentScreen(
            isLoading = false,
            navigatableAction = ScreenNavigateAction.NONE,
            onBack = {},
            stickyBottom = { paddingValues ->
                ActionButtons(
                    paddings = paddingValues,
                    onReject = {},
                    onConsent = {},
                    isConsentEnabled = true
                )
            }
        ) { paddingValues ->
            Content(
                paddingValues = paddingValues,
                isConsentChecked = false,
                onConsentChecked = {},
                onMoreInfoClicked = {}
            )
        }
    }
}
