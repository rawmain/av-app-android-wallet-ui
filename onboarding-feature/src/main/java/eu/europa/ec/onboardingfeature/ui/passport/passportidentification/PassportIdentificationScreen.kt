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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import eu.europa.ec.onboardingfeature.ui.passport.passportidentification.Effect.Navigation
import eu.europa.ec.passportscanner.SmartScannerActivity
import eu.europa.ec.passportscanner.scanner.config.CaptureOptions
import eu.europa.ec.passportscanner.scanner.config.CaptureType
import eu.europa.ec.passportscanner.scanner.config.Config
import eu.europa.ec.passportscanner.scanner.config.ScannerOptions
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
fun PassportIdentificationScreen(controller: NavController, viewModel: PassportIdentificationViewModel) {

    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // TODO Handle MRZ scanner result here - Passport should be in bundle
    // You can process the result.data and result.resultCode
    val mrzScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result -> viewModel.setEvent(Event.OnPassportScanSuccessful("passportPicture")) }

    ContentScreen(
        isLoading = state.isLoading,
        navigatableAction = ScreenNavigateAction.NONE,
        onBack = { viewModel.setEvent(Event.OnBackPressed) },
        stickyBottom = { paddingValues ->
            ActionButtons(state = state,
                onBack = { viewModel.setEvent(Event.OnBackPressed) },
                onCapture = { viewModel.setEvent(Event.OnStartPassportScan) },
                onTryAgain = { viewModel.setEvent(Event.OnProcessRestartRequest) },
                onNext = { viewModel.setEvent(Event.OnPassportVerificationCompletion) },
                paddings = paddingValues
            )
        }
    ) { paddingValues ->
        Content(paddingValues = paddingValues)
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
    onNext : () -> Unit = {},
    onBack: () -> Unit = {},
    onCapture: () -> Unit = {},
    onTryAgain : () -> Unit = {},
    paddings: PaddingValues,
) {

    val buttons = StickyBottomType.TwoButtons(
        primaryButtonConfig = ButtonConfig(
            type = ButtonType.SECONDARY,
            onClick = { if (state.scanComplete) onTryAgain() else onBack() }
        ),
        secondaryButtonConfig = ButtonConfig(
            type = ButtonType.PRIMARY,
            onClick = { if (state.scanComplete) onNext() else onCapture() }
        )
    )

    WrapStickyBottomContent(stickyBottomModifier = Modifier.fillMaxWidth().padding(paddings),
        stickyBottomConfig = StickyBottomConfig(type = buttons, showDivider = false)
    ) { buttonConfigs ->
        buttonConfigs?.let { buttonConfig ->
            when (buttonConfig.type) {
                ButtonType.PRIMARY -> Text(stringResource(R.string.passport_identification_capture))
                ButtonType.SECONDARY -> {
                    Text(if (state.scanComplete) stringResource(R.string.passport_biometrics_try_again)
                    else stringResource(R.string.passport_identification_back))
                }
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
        is Navigation.StartMRZScanner -> mrzScannerLauncher.launch(createMrzScannerIntent(context))
        is Navigation.StartPassportLiveCheck -> hostNavController.navigate(PassportLiveVideo.screenRoute)
    }
}

private fun createMrzScannerIntent(context: Context): Intent =
    Intent(context, SmartScannerActivity::class.java).apply {
        putExtra(
            SmartScannerActivity.SCANNER_OPTIONS,
            ScannerOptions(
                config = Config(
                    header = context.getString(R.string.passport_identification_capture),
                    subHeader = context.getString(R.string.passport_identification_title),
                    isManualCapture = false,
                    showGuide = true,
                    showSettings = false
                ),
                captureOptions = CaptureOptions(
                    type = CaptureType.DOCUMENT.value,
                    height = 180,
                    width = 285
                )
            )
        )
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
fun VerifyYourDataContent(state: State, paddingValues: PaddingValues) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {

        PassportVerificationStepBar(0)

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
        ElevatedCard(modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = SPACING_EXTRA_SMALL.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.elevated),
            shape = RoundedCornerShape(size = SPACING_SMALL.dp)) {

            Column(modifier = Modifier.padding(all = SPACING_LARGE.dp)) {

                WrapText(
                    text = stringResource(R.string.passport_biometrics_passport),
                    textConfig = TextConfig(
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    ),
                )

                //If your bitmap comes from a ByteArray
                //val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                //BitmapImage(bitmap)

                VSpacer.ExtraLarge()
                Image(
                    painter = painterResource(R.drawable.ic_logo_plain), // BitmapPainter(bitmap.asImageBitmap()),,
                    contentDescription = "Bitmap Image",
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).size(150.dp),
                    contentScale = ContentScale.Fit
                )

                VSpacer.ExtraLarge()
                WrapText(
                    text = stringResource(R.string.passport_biometrics_dob),
                    textConfig = TextConfig(
                        style = MaterialTheme.typography.bodyLarge
                    )
                )

                VSpacer.Small()
                WrapText(
                    text = "13 May 2001",
                    textConfig = TextConfig(
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                )

                VSpacer.ExtraLarge()
                WrapText(
                    text = stringResource(R.string.passport_biometrics_doe),
                    textConfig = TextConfig(
                        style = MaterialTheme.typography.bodyLarge
                    )
                )

                VSpacer.Small()
                WrapText(
                    text = "8 May 2031",
                    textConfig = TextConfig(
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                )
            }
        }
    }
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
            VerifyYourDataContent(state = State(), paddingValues = paddingValues)
        }
    }
}
