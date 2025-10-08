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
import eu.europa.ec.onboardingfeature.config.PassportLiveVideoUiConfig
import eu.europa.ec.passportscanner.face.AVFaceMatchSDK
import eu.europa.ec.passportscanner.face.AVFaceMatchSdkImpl
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
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
) : ViewState

sealed class Event : ViewEvent {
    data object OnBackPressed : Event()
    data object OnLiveVideoCapture : Event()
    data object InitializeSdk : Event()
    data class UpdateSdkInitProgress(val progress: Int, val message: String) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object GoBack : Navigation()
        data object StartVideoLiceCapture : Navigation()
    }

    data class Failure(val message: String) : Effect()
}

@KoinViewModel
class PassportLiveVideoViewModel(
    private val context: Context,
    private val uiSerializer: UiSerializer,
    private val logController: LogController,
    @InjectedParam private val passportLiveVideoSerializedConfig: String,
) : MviViewModel<Event, State, Effect>() {

    private var faceMatchSdk: AVFaceMatchSDK? = null

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
                if (faceMatchSdk != null && viewState.value.isSdkReady) {
                    setEffect { Effect.Navigation.StartVideoLiceCapture }
                } else {
                    logController.e(TAG) { "Cannot start capture: SDK not initialized" }
                    setEffect { Effect.Failure("SDK not ready, please wait...") }
                }
            }

            Event.InitializeSdk -> {
                initializeSdk()
            }

            is Event.UpdateSdkInitProgress -> {
                setState {
                    copy(
                        sdkInitProgress = event.progress,
                        sdkInitMessage = event.message
                    )
                }
            }
        }
    }

    private fun initializeSdk() {
        setState { copy(isSdkInitializing = true) }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    logController.d(TAG) { "Initialising AVFaceMatchSdkImpl..." }
                    val sdk = AVFaceMatchSdkImpl(context.applicationContext)

                    val success = sdk.init { progress, message ->
                        logController.d(TAG) { "Init progress: $progress% - $message" }
                        setEvent(Event.UpdateSdkInitProgress(progress, message))
                    }

                    logController.d(TAG) { "SDK initialization completed with result: $success" }
                    if (success) {
                        faceMatchSdk = sdk
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

    fun getSdk(): AVFaceMatchSDK? = faceMatchSdk
}
