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
import android.net.Uri
import androidx.lifecycle.viewModelScope
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.commonfeature.config.IssuanceSuccessUiConfig
import eu.europa.ec.commonfeature.config.OfferUiConfig
import eu.europa.ec.commonfeature.config.PresentationMode
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.corelogic.di.getOrCreatePresentationScope
import eu.europa.ec.onboardingfeature.config.PassportLiveVideoUiConfig
import eu.europa.ec.onboardingfeature.interactor.EnrollmentInteractor
import eu.europa.ec.onboardingfeature.interactor.EnrollmentInteractorPartialState
import eu.europa.ec.onboardingfeature.interactor.FaceMatchPartialState
import eu.europa.ec.onboardingfeature.interactor.PassportLiveVideoInteractor
import eu.europa.ec.onboardingfeature.interactor.PassportValidationState
import eu.europa.ec.uilogic.component.content.ContentErrorConfig
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.IssuanceScreens
import eu.europa.ec.uilogic.navigation.LandingScreens
import eu.europa.ec.uilogic.navigation.OnboardingScreens
import eu.europa.ec.uilogic.navigation.PresentationScreens
import eu.europa.ec.uilogic.navigation.helper.DeepLinkType
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.navigation.helper.hasDeepLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import java.io.File

private const val TAG = "PassportLiveVideoViewModel"

data class State(
    val isLoading: Boolean = false,
    val config: PassportLiveVideoUiConfig? = null,
    val error: ContentErrorConfig? = null,
) : ViewState

sealed class Event : ViewEvent {
    data class Init(val deepLink: Uri?) : Event()
    data object OnBackPressed : Event()
    data class OnLiveVideoCapture(val context: Context) : Event()
    data object OnRetry : Event()
    data object OnPause : Event()
    data class OnResumeIssuance(val uri: String) : Event()
    data class OnDynamicPresentation(val uri: String) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object GoBack : Navigation()
        data object StartVideoLiveCapture : Navigation()
        data class SwitchScreen(val screenRoute: String, val inclusive: Boolean) : Navigation()
        data class OpenDeepLinkAction(val deepLinkUri: Uri, val arguments: String?) : Navigation()
    }
}

@KoinViewModel
class PassportLiveVideoViewModel(
    private val uiSerializer: UiSerializer,
    private val logController: LogController,
    private val passportLiveVideoInteractor: PassportLiveVideoInteractor,
    private val enrollmentInteractor: EnrollmentInteractor,
    @InjectedParam private val passportLiveVideoSerializedConfig: String,
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State {
        logController.i { "get the following param=$passportLiveVideoSerializedConfig" }
        val config: PassportLiveVideoUiConfig? = uiSerializer.fromBase64(
            passportLiveVideoSerializedConfig,
            PassportLiveVideoUiConfig::class.java,
            PassportLiveVideoUiConfig.Parser
        )
        return State(config = config)
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.Init -> {
                logController.i(TAG) { "Event invoked Init with deepLink: ${event.deepLink}" }
                handleDeepLink(event.deepLink)
            }

            Event.OnBackPressed -> setEffect {
                logController.i(TAG) { "Event invoked OnBackPressed" }
                Effect.Navigation.GoBack
            }

            is Event.OnLiveVideoCapture -> {
                logController.i(tag = TAG) { "Event invoked OnLiveVideoCapture" }
                handleLiveVideoCapture(event.context)
            }

            Event.OnRetry -> {
                logController.i(tag = TAG) { "Event invoked OnRetry" }
                setState { copy(error = null) }
            }

            is Event.OnPause -> {
                logController.i(TAG) { "Event invoked OnPause" }
                setState { copy(isLoading = false) }
            }

            is Event.OnResumeIssuance -> {
                logController.i(TAG) { "Event invoked OnResumeIssuance with uri: ${event.uri}" }
                setState { copy(isLoading = true) }
                enrollmentInteractor.resumeOpenId4VciWithAuthorization(event.uri)
            }

            is Event.OnDynamicPresentation -> {
                logController.i(TAG) { "Event invoked OnDynamicPresentation with uri: ${event.uri}" }
                handleDynamicPresentation(event.uri)
            }
        }
    }

    private fun handleLiveVideoCapture(context: Context) {
        val config = viewState.value.config
        if (config == null) {
            logController.e(TAG) { "Config is null, cannot proceed with live video capture" }
            showError("Configuration error. Please try again.")
            return
        }

        setState { copy(isLoading = true, error = null) }
        setEffect { Effect.Navigation.StartVideoLiveCapture }

        viewModelScope.launch {
            try {
                // Step 1: Validate passport age and expiration
                when (val validationResult = passportLiveVideoInteractor.validatePassport(
                    dateOfBirth = config.dateOfBirth,
                    expiryDate = config.expiryDate
                )) {
                    is PassportValidationState.Success -> {
                        logController.i(TAG) { "Passport validation successful, proceeding to face match" }
                        // Step 2: Capture and match face
                        when (val matchResult = passportLiveVideoInteractor.captureAndMatchFace(
                            context = context,
                            faceImageTempPath = config.faceImageTempPath
                        )) {
                            is FaceMatchPartialState.Success -> {
                                logController.i(TAG) { "Face match successful, proceeding with document issuance" }
                                // Step 3: Issue document
                                issueDocument(context, config.faceImageTempPath)
                            }

                            is FaceMatchPartialState.Failure -> {
                                logController.e(TAG) { "Face match failed: ${matchResult.error}" }
                                setState { copy(isLoading = false) }
                                showError(matchResult.error)
                            }
                        }
                    }

                    is PassportValidationState.Failure -> {
                        logController.e(TAG) { "Passport validation failed: ${validationResult.error}" }
                        setState { copy(isLoading = false) }
                        showError(validationResult.error)
                    }
                }
            } catch (e: Exception) {
                logController.e(TAG) { "Exception during live video capture: ${e.message}" }
                setState { copy(isLoading = false) }
                showError(e.message ?: "An unexpected error occurred")
            }
        }
    }

    private fun issueDocument(context: Context, faceImageTempPath: String) {
        logController.i(TAG) { "Starting document issuance flow using EnrollmentInteractor" }
        viewModelScope.launch {
            try {
                enrollmentInteractor.issueNationalEID(context).collect { issueState ->
                    logController.i(TAG) { "Received issueState: ${issueState::class.simpleName}" }
                    when (issueState) {
                        is EnrollmentInteractorPartialState.Success -> {
                            logController.i(TAG) { "Document issued successfully: ${issueState.documentId}" }
                            setState { copy(isLoading = false) }
                            // Delete temp file after successful issuance
                            deleteTempFile(faceImageTempPath)
                            logController.i(TAG) { "Navigating to success screen with documentId: ${issueState.documentId}" }
                            navigateToSuccessScreen(issueState.documentId)
                        }

                        is EnrollmentInteractorPartialState.DeferredSuccess -> {
                            logController.i(TAG) { "Document issuance deferred, navigating to success route: ${issueState.successRoute}" }
                            setState { copy(isLoading = false) }
                            // Delete temp file after deferred success
                            deleteTempFile(faceImageTempPath)
                            navigateToDeferredSuccessScreen(issueState.successRoute)
                        }

                        is EnrollmentInteractorPartialState.UserAuthRequired -> {
                            logController.i(TAG) { "User authentication required for document issuance" }
                            // Keep loading state, authentication is part of the issuance flow
                            enrollmentInteractor.handleUserAuth(
                                context = context,
                                crypto = issueState.crypto,
                                notifyOnAuthenticationFailure = true,
                                resultHandler = issueState.resultHandler
                            )
                        }

                        is EnrollmentInteractorPartialState.Failure -> {
                            logController.e(TAG) { "Document issuance failed: ${issueState.error}" }
                            setState { copy(isLoading = false) }
                            showError(issueState.error)
                        }
                    }
                }
            } catch (e: Exception) {
                logController.e(TAG) { "Exception in issueDocument flow: ${e.message}" }
                setState { copy(isLoading = false) }
                showError(e.message ?: "Document issuance failed unexpectedly")
            }
        }
    }

    private fun deleteTempFile(filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    logController.i(TAG) { "Successfully deleted temp file: $filePath" }
                } else {
                    logController.w(TAG) { "Failed to delete temp file: $filePath" }
                }
            } else {
                logController.w(TAG) { "Temp file does not exist: $filePath" }
            }
        } catch (e: Exception) {
            logController.e(TAG) { "Exception while deleting temp file: ${e.message}" }
        }
    }

    private fun navigateToSuccessScreen(documentId: String) {
        logController.i(TAG) { "Building navigation to success screen for documentId: $documentId" }
        val onSuccessNavigation = ConfigNavigation(
            navigationType = NavigationType.PushScreen(
                screen = LandingScreens.Landing,
                popUpToScreen = OnboardingScreens.Enrollment
            )
        )

        val screenRoute = generateComposableNavigationLink(
            screen = IssuanceScreens.DocumentIssuanceSuccess,
            arguments = generateComposableArguments(
                mapOf(
                    IssuanceSuccessUiConfig.serializedKeyName to uiSerializer.toBase64(
                        model = IssuanceSuccessUiConfig(
                            documentIds = listOf(documentId),
                            onSuccessNavigation = onSuccessNavigation,
                        ),
                        parser = IssuanceSuccessUiConfig.Parser
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

    private fun navigateToDeferredSuccessScreen(route: String) {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = route,
                inclusive = true
            )
        }
    }

    private fun handleDynamicPresentation(uri: String) {
        logController.i(TAG) { "Handling dynamic presentation for uri: $uri" }
        getOrCreatePresentationScope()
        setEffect {
            Effect.Navigation.SwitchScreen(
                generateComposableNavigationLink(
                    PresentationScreens.PresentationRequest,
                    generateComposableArguments(
                        mapOf(
                            RequestUriConfig.serializedKeyName to uiSerializer.toBase64(
                                RequestUriConfig(
                                    PresentationMode.OpenId4Vp(
                                        uri,
                                        IssuanceScreens.AddDocument.screenRoute
                                    )
                                ),
                                RequestUriConfig
                            )
                        )
                    )
                ),
                inclusive = false
            )
        }
    }

    private fun handleDeepLink(deepLinkUri: Uri?) {
        logController.i(TAG) { "Handling deepLink: $deepLinkUri" }
        deepLinkUri?.let { uri ->
            hasDeepLink(uri)?.let {
                logController.i(TAG) { "DeepLink detected with type: ${it.type}" }
                when (it.type) {
                    DeepLinkType.CREDENTIAL_OFFER -> {
                        logController.i(TAG) { "Handling CREDENTIAL_OFFER deeplink" }
                        setEffect {
                            Effect.Navigation.OpenDeepLinkAction(
                                deepLinkUri = uri,
                                arguments = generateComposableArguments(
                                    mapOf(
                                        OfferUiConfig.serializedKeyName to uiSerializer.toBase64(
                                            OfferUiConfig(
                                                offerURI = it.link.toString(),
                                                onSuccessNavigation = ConfigNavigation(
                                                    navigationType = NavigationType.PushScreen(
                                                        screen = LandingScreens.Landing,
                                                        popUpToScreen = IssuanceScreens.AddDocument
                                                    )
                                                ),
                                                onCancelNavigation = ConfigNavigation(
                                                    navigationType = NavigationType.Pop
                                                )
                                            ),
                                            OfferUiConfig.Parser
                                        )
                                    )
                                )
                            )
                        }
                    }

                    DeepLinkType.EXTERNAL -> {
                        logController.i(TAG) { "Handling EXTERNAL deeplink" }
                        setEffect {
                            Effect.Navigation.OpenDeepLinkAction(
                                deepLinkUri = uri,
                                arguments = null
                            )
                        }
                    }

                    else -> {
                        logController.i(TAG) { "Unhandled deeplink type: ${it.type}" }
                    }
                }
            }
        }
    }

    private fun showError(errorMessage: String) {
        setState {
            copy(
                error = ContentErrorConfig(
                    errorTitle = "Verification Failed",
                    errorSubTitle = errorMessage,
                    onCancel = { setEvent(Event.OnBackPressed) },
                    onRetry = { setEvent(Event.OnRetry) }
                )
            )
        }
    }
}
