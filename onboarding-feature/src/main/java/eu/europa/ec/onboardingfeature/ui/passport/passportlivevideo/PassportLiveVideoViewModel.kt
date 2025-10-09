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

import android.content.Context
import androidx.lifecycle.viewModelScope
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.onboardingfeature.config.PassportCredentialIssuanceUiConfig
import eu.europa.ec.onboardingfeature.config.PassportLiveVideoUiConfig
import eu.europa.ec.onboardingfeature.interactor.FaceMatchPartialState
import eu.europa.ec.onboardingfeature.interactor.PassportLiveVideoInteractor
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

private const val TAG = "PassportLiveVideoViewModel"

data class State(
    val isLoading: Boolean = false,
    val config: PassportLiveVideoUiConfig? = null,
    val sdkInitProgress: Int = 0, // 0-100 percentage
    val sdkInitMessage: String = "",
    val isSdkInitializing: Boolean = false,
    val isSdkReady: Boolean = false,
    val error: ContentErrorConfig? = null,
) : ViewState

sealed class Event : ViewEvent {
    data object OnBackPressed : Event()
    data object OnLiveVideoCapture : Event()
    data object OnRetry : Event()
    data class InitializeSdk(val context: Context) : Event()
    data class UpdateSdkInitProgress(val progress: Int, val message: String) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object GoBack : Navigation()
        data class SwitchScreen(val screenRoute: String, val inclusive: Boolean) : Navigation()
    }

    data class Failure(val message: String) : Effect()
    data class CaptureSuccess(val message: String) : Effect()
}

@KoinViewModel
class PassportLiveVideoViewModel(
    private val uiSerializer: UiSerializer,
    private val logController: LogController,
    private val passportLiveVideoInteractor: PassportLiveVideoInteractor,
    private val resourceProvider: ResourceProvider,
    @InjectedParam private val passportLiveVideoSerializedConfig: String,
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State {
        logController.i(TAG) { "get the following param=$passportLiveVideoSerializedConfig" }
        val config: PassportLiveVideoUiConfig? = uiSerializer.fromBase64(
            passportLiveVideoSerializedConfig,
            PassportLiveVideoUiConfig::class.java,
            PassportLiveVideoUiConfig.Parser
        )
        return State(config = config)
    }

    override fun handleEvents(event: Event) {
        when (event) {
            Event.OnBackPressed -> setEffect {
                Effect.Navigation.GoBack
            }

            Event.OnLiveVideoCapture -> {
                logController.i(tag = TAG) { "Event invoked OnLiveVideoCapture" }
                handleLiveVideoCapture()
            }

            is Event.InitializeSdk -> {
                initializeSdk(event.context)
            }

            is Event.UpdateSdkInitProgress -> {
                setState {
                    copy(
                        sdkInitProgress = event.progress,
                        sdkInitMessage = event.message
                    )
                }
            }
            Event.OnRetry -> {
                logController.i(tag = TAG) { "Event invoked OnRetry" }
                setState { copy(error = null) }
            }
        }
    }

    private fun initializeSdk(context: Context) {
        setState { copy(isSdkInitializing = true, error = null) }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    logController.d(TAG) { "Initializing SDK via interactor..." }

                    val success = passportLiveVideoInteractor.init(context) { progress, message ->
                        logController.d(TAG) { "Init progress: $progress% - $message" }
                        setEvent(Event.UpdateSdkInitProgress(progress, message))
                    }

                    if (success) {
                        setState {
                            copy(
                                isSdkInitializing = false,
                                isSdkReady = true
                            )
                        }
                    } else {
                        setState { copy(isSdkInitializing = false) }
                        setEffect { Effect.Failure("SDK initialization failed") }
                    }
                }
            } catch (e: Exception) {
                logController.e(TAG) { "Exception during SDK initialization: ${e.javaClass.simpleName}: ${e.message}" }
                setState { copy(isSdkInitializing = false) }
                setEffect { Effect.Failure("Failed to initialize SDK: ${e.message}") }
            }
        }
    }

    private fun handleLiveVideoCapture() {
        val config = viewState.value.config
        if (config == null) {
            logController.e(TAG) { "Config is null, cannot proceed with live video capture" }
            showError(resourceProvider.getString(R.string.generic_error_retry))
            return
        }

        setState { copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                when (val matchResult = passportLiveVideoInteractor.captureAndMatchFace(
                    faceImageTempPath = config.faceImageTempPath
                )) {
                    is FaceMatchPartialState.Success -> {
                        logController.i(TAG) { "Face match successful, navigating to consent screen" }
                        setState { copy(isLoading = false) }
                        navigateToConsentScreen(config.faceImageTempPath)
                    }

                    is FaceMatchPartialState.Failure -> {
                        logController.e(TAG) { "Face match failed: ${matchResult.error}" }
                        setState { copy(isLoading = false) }
                        showError(matchResult.error)
                    }
                }
            } catch (e: Exception) {
                logController.e(TAG) { "Exception during live video capture: ${e.message}" }
                setState { copy(isLoading = false) }
                showError(e.message ?: "An unexpected error occurred")
            }
        }
    }

    private fun navigateToConsentScreen(faceImageTempPath: String) {
        logController.i(TAG) { "Building navigation to consent screen" }
        val screenRoute = generateComposableNavigationLink(
            screen = OnboardingScreens.PassportCredentialIssuance,
            arguments = generateComposableArguments(
                mapOf(
                    PassportCredentialIssuanceUiConfig.serializedKeyName to uiSerializer.toBase64(
                        model = PassportCredentialIssuanceUiConfig(
                            faceImageTempPath = faceImageTempPath
                        ),
                        parser = PassportCredentialIssuanceUiConfig.Parser
                    ).orEmpty()
                )
            )
        )
        logController.i(TAG) { "Setting navigation effect to screenRoute: $screenRoute" }
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = screenRoute,
                inclusive = false
            )
        }
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
