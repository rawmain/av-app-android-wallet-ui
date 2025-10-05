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
import android.graphics.Bitmap
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.onboardingfeature.config.PassportLiveVideoUiConfig
import eu.europa.ec.onboardingfeature.interactor.PassportIdentificationInteractor
import eu.europa.ec.onboardingfeature.interactor.PassportValidationState
import eu.europa.ec.onboardingfeature.ui.passport.passportidentification.Effect.Navigation.GoBack
import eu.europa.ec.onboardingfeature.ui.passport.passportidentification.Effect.Navigation.StartMRZScanner
import eu.europa.ec.onboardingfeature.ui.passport.passportidentification.Effect.Navigation.StartPassportLiveCheck
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.OnboardingScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import org.koin.android.annotation.KoinViewModel
import java.io.File
import java.io.FileOutputStream

data class State(
    val isLoading: Boolean = false,
    val scanComplete: Boolean = false,
    val passportData: PassportData? = null,
    val error: ContentErrorConfig? = null,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object OnBackPressed : Event()
    data object OnStartPassportScan : Event()
    data object OnProcessRestartRequest : Event()
    data object OnPassportVerificationCompletion : Event()
    data class OnPassportScanSuccessful(val passportData: PassportData) : Event()
    data class OnPassportScanFailed(val errorMessage: String) : Event()
    data object OnRetry : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object GoBack : Navigation()
        data object StartMRZScanner : Navigation()
        data class StartPassportLiveCheck(val screenRoute: String) : Navigation()
    }
}

@KoinViewModel
class PassportIdentificationViewModel(
    private val context: Context,
    private val logController: LogController,
    private val uiSerializer: UiSerializer,
    private val passportIdentificationInteractor: PassportIdentificationInteractor,
    private val resourceProvider: ResourceProvider,
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State = State()

    override fun handleEvents(event: Event) {
        when (event) {
            Event.Init -> logController.i { "Init -- PassportIdentificationViewModel " }
            Event.OnBackPressed -> setEffect { GoBack }
            Event.OnStartPassportScan -> setEffect { StartMRZScanner }
            Event.OnPassportVerificationCompletion -> handlePassportVerification()
            Event.OnProcessRestartRequest -> setEffect { GoBack }
            is Event.OnPassportScanSuccessful -> setState {
                copy(
                    scanComplete = true,
                    passportData = event.passportData
                )
            }

            is Event.OnPassportScanFailed -> {
                logController.e { "Passport scan failed: ${event.errorMessage}" }
                setState { copy(scanComplete = false, passportData = null) }
            }

            Event.OnRetry -> setState { copy(error = null) }
        }
    }

    private fun handlePassportVerification() {
        val passportData = viewState.value.passportData
        if (passportData == null ||
            passportData.dateOfBirth.isNullOrEmpty() ||
            passportData.expiryDate.isNullOrEmpty() ||
            passportData.faceImage == null
        ) {
            logController.e { "Passport data is incomplete, cannot verify" }
            showError(resourceProvider.getString(R.string.passport_validation_error_incomplete_data))
            return
        }

        setState { copy(isLoading = true, error = null) }

        when (val validationResult = passportIdentificationInteractor.validatePassport(
            dateOfBirth = passportData.dateOfBirth,
            expiryDate = passportData.expiryDate
        )) {
            is PassportValidationState.Success -> {
                logController.i { "Passport validation successful, navigating to live video" }
                setState { copy(isLoading = false) }
                setEffect { generatePasswordLiveLink(passportData) }
            }

            is PassportValidationState.Failure -> {
                logController.e { "Passport validation failed: ${validationResult.error}" }
                setState { copy(isLoading = false) }
                showError(validationResult.error)
            }
        }
    }

    private fun generatePasswordLiveLink(passportData: PassportData): StartPassportLiveCheck =
        StartPassportLiveCheck(
            generateComposableNavigationLink(
                OnboardingScreens.PassportLiveVideo,
                generateComposableArguments(
                    mapOf(
                        PassportLiveVideoUiConfig.serializedKeyName to uiSerializer.toBase64(
                            generateUiConfig(passportData),
                            PassportLiveVideoUiConfig.Parser
                        )
                    )
                )
            )
        )

    private fun generateUiConfig(passportData: PassportData): PassportLiveVideoUiConfig {
        val tempFile = File(context.cacheDir, "passport_face_${System.currentTimeMillis()}.png")
        FileOutputStream(tempFile).use { fos ->
            passportData.faceImage!!.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        return PassportLiveVideoUiConfig(
            dateOfBirth = passportData.dateOfBirth!!,
            expiryDate = passportData.expiryDate!!,
            faceImageTempPath = tempFile.absolutePath,
        )
    }

    private fun showError(errorMessage: String) {
        setState {
            copy(
                error = ContentErrorConfig(
                    errorSubTitle = errorMessage,
                    onCancel = { setEvent(Event.OnBackPressed) },
                    onRetry = { setEvent(Event.OnRetry) }
                )
            )
        }
    }
}
