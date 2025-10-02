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

package eu.europa.ec.onboardingfeature.ui.passport.passportidentification

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.onboardingfeature.ui.passport.passportidentification.Effect.Navigation
import eu.europa.ec.onboardingfeature.ui.passport.passportidentification.Event.OnPassportScanSuccessful
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.theme.values.elevated
import eu.europa.ec.uilogic.component.BulletHolder
import eu.europa.ec.uilogic.component.PassportVerificationStepBar
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.preview.PreviewTheme
import eu.europa.ec.uilogic.component.preview.ThemeModePreviews
import eu.europa.ec.uilogic.component.utils.SPACING_EXTRA_SMALL
import eu.europa.ec.uilogic.component.utils.SPACING_LARGE
import eu.europa.ec.uilogic.component.utils.SPACING_SMALL
import eu.europa.ec.uilogic.component.utils.VSpacer
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.StickyBottomConfig
import eu.europa.ec.uilogic.component.wrap.StickyBottomType
import eu.europa.ec.uilogic.component.wrap.TextConfig
import eu.europa.ec.uilogic.component.wrap.WrapStickyBottomContent
import eu.europa.ec.uilogic.component.wrap.WrapText
import eu.europa.ec.uilogic.navigation.OnboardingScreens.PassportLiveVideo

@Composable
fun PassportIdentificationScreen(
    controller: NavController,
    viewModel: PassportIdentificationViewModel
) {

    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val mrzScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                result.data?.let { intent ->
                    val passportData =
                        PassportScannerIntentHelper.extractPassportDataFromIntent(intent)
                    if (passportData.dateOfBirth == null && passportData.expiryDate == null && passportData.faceImage == null) {
                        viewModel.setEvent(Event.OnPassportScanFailed("No passport data received"))
                    } else {
                        viewModel.setEvent(OnPassportScanSuccessful(passportData))
                    }
                }
            }

            Activity.RESULT_CANCELED -> {
                viewModel.setEvent(Event.OnPassportScanFailed("Passport scan was cancelled"))
            }

            else -> {
                viewModel.setEvent(Event.OnPassportScanFailed("Unknown error occurred during passport scan"))
            }
        }
    }

    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = ScreenNavigateAction.NONE,
        onBack = { viewModel.setEvent(Event.OnBackPressed) },
        stickyBottom = { paddingValues ->
            ActionButtons(
                state = state,
                onBack = { viewModel.setEvent(Event.OnBackPressed) },
                onCapture = { viewModel.setEvent(Event.OnStartPassportScan) },
                onTryAgain = { viewModel.setEvent(Event.OnProcessRestartRequest) },
                onNext = { viewModel.setEvent(Event.OnPassportVerificationCompletion) },
                paddings = paddingValues
            )
        }
    ) { paddingValues ->
        if (state.scanComplete) {
            VerifyYourDataContent(passportData = state.passportData!!, paddingValues = paddingValues)
        } else {
            Content(paddingValues = paddingValues)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            handleEffect(effect, controller, context, mrzScannerLauncher)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.setEvent(Event.Init)
    }
}

@Composable
private fun ActionButtons(
    state: State,
    onNext: () -> Unit = {},
    onBack: () -> Unit = {},
    onCapture: () -> Unit = {},
    onTryAgain: () -> Unit = {},
    paddings: PaddingValues,
) {

    val buttons = StickyBottomType.TwoButtons(
        primaryButtonConfig = ButtonConfig(
        type = ButtonType.SECONDARY,
        onClick = {
            if (state.scanComplete && state.passportData == null)
                onTryAgain()
            else
                onBack()
        }),
        secondaryButtonConfig = ButtonConfig(
            type = ButtonType.PRIMARY,
            onClick = { if (state.scanComplete) onNext() else onCapture() }))

    val primaryButtonText = if (state.scanComplete) {
        R.string.passport_biometrics_next
    } else {
        R.string.passport_identification_capture
    }

    val secondaryButtonText = if (state.scanComplete && state.passportData == null) {
        R.string.passport_biometrics_try_again
    } else {
        R.string.passport_identification_back
    }

    WrapStickyBottomContent(
        stickyBottomModifier = Modifier
            .fillMaxWidth()
            .padding(paddings),
        stickyBottomConfig = StickyBottomConfig(type = buttons, showDivider = false)
    ) { buttonConfigs ->
        buttonConfigs?.let { buttonConfig ->
            when (buttonConfig.type) {
                ButtonType.PRIMARY -> Text(stringResource(primaryButtonText))
                ButtonType.SECONDARY -> Text(stringResource (secondaryButtonText))
            }
        }
    }
}

private fun handleEffect(
    effect: Effect,
    hostNavController: NavController,
    context: Context,
    mrzScannerLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    when (effect) {
        is Navigation.GoBack -> hostNavController.popBackStack()
        is Navigation.StartMRZScanner -> mrzScannerLauncher.launch(
            PassportScannerIntentHelper.createMrzScannerIntent(
                context
            )
        )

        is Navigation.StartPassportLiveCheck -> {
            hostNavController.navigate(effect.screenRoute)
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

        PassportVerificationStepBar(0)

        VSpacer.ExtraLarge()
        WrapText(
            text = stringResource(R.string.passport_identification_title),
            textConfig = TextConfig(
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            ),
        )

        VSpacer.ExtraLarge()
        WrapText(
            text = stringResource(R.string.passport_identification_description),
            textConfig = TextConfig(
                style = MaterialTheme.typography.bodyLarge
            )
        )

        VSpacer.Large()
        BulletHolder(
            stringResource(R.string.passport_identification_step_first),
            stringResource(R.string.passport_identification_step_second)
        )

        VSpacer.Large()
        WrapText(
            text = stringResource(R.string.passport_identification_footer),
            textConfig = TextConfig(
                style = MaterialTheme.typography.bodyLarge,
                maxLines = Int.MAX_VALUE
            )
        )
    }
}


@Composable
fun VerifyYourDataContent(passportData: PassportData, paddingValues: PaddingValues) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {

        PassportVerificationStepBar(1)

        VSpacer.ExtraLarge()
        WrapText(
            text = stringResource(R.string.passport_biometrics_verify_data),
            textConfig = TextConfig(
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            ),
        )

        VSpacer.Large()
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = SPACING_EXTRA_SMALL.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.elevated),
            shape = RoundedCornerShape(size = SPACING_SMALL.dp)
        ) {

            Column(modifier = Modifier.padding(all = SPACING_LARGE.dp)) {

                WrapText(
                    text = stringResource(R.string.passport_biometrics_passport),
                    textConfig = TextConfig(
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    ),
                )

                VSpacer.ExtraLarge()

                PassportImageOrFallback(passportData)

                VSpacer.ExtraLarge()
                val labelId = R.string.passport_biometrics_dob
                PassportInfoWithLabel(labelId, passportData?.dateOfBirth)

                VSpacer.ExtraLarge()
                val labelExpiry = R.string.passport_biometrics_doe
                PassportInfoWithLabel(labelExpiry, passportData?.expiryDate)
            }
        }
    }
}

@Composable
private fun PassportInfoWithLabel(
    labelId: Int,
    text: String?
) {
    WrapText(
        text = stringResource(labelId),
        textConfig = TextConfig(
            style = MaterialTheme.typography.bodyMedium
        )
    )

    VSpacer.Small()
    WrapText(
        text = text ?: "Not available",
        textConfig = TextConfig(
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold
            )
        )
    )
}

@Composable
private fun PassportImageOrFallback(passportData: PassportData) {
    passportData?.faceImage?.let { faceImage ->
        Image(
            painter = BitmapPainter(faceImage.asImageBitmap()),
            contentDescription = "Passport Face Image",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .size(150.dp),
            contentScale = ContentScale.Fit
        )
    } ?: Image(
        painter = painterResource(R.drawable.ic_logo_plain),
        contentDescription = "Placeholder Image",
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .size(150.dp),
        contentScale = ContentScale.Fit
    )
}

@Composable
@ThemeModePreviews
fun PassportIdentificationScreenPreview() {
    PreviewTheme {
        ContentScreen(
            navigatableAction = ScreenNavigateAction.NONE,
            stickyBottom = { paddingValues ->
                ActionButtons(
                    state = State(scanComplete = false),
                    paddings = paddingValues,
                )
            }
        ) { paddingValues ->
            Content(paddingValues = paddingValues)
        }
    }
}

@Composable
@ThemeModePreviews
fun VerifyYourDataContentPreview() {
    PreviewTheme {
        ContentScreen(
            navigatableAction = ScreenNavigateAction.NONE,
            stickyBottom = { paddingValues ->
                ActionButtons(
                    state = State(scanComplete = true),
                    paddings = paddingValues
                )
            }
        ) { paddingValues ->
            VerifyYourDataContent(passportData = PassportData("10/28/1983", "1/31/2033", null), paddingValues = paddingValues)
        }
    }
}
