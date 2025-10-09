/*
 * Copyright (c) 2025 European Commission
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

package eu.europa.ec.landingfeature.ui.settings

import androidx.lifecycle.viewModelScope
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.commonfeature.model.PinFlow
import eu.europa.ec.landingfeature.interactor.DeleteAgeDocumentsPartialState
import eu.europa.ec.landingfeature.interactor.SettingsInteractor
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.OnboardingScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

data class State(
    val isLoading: Boolean = false,
    val showDeleteProofsDialog: Boolean = false,
) : ViewState

sealed class Event : ViewEvent {
    data object DeleteProofsClicked : Event()
    data object ChangePinClicked : Event()
    data object GoBack : Event()
    sealed class BottomSheetEvent : Event() {
        data object DeleteProofsConfirmed : BottomSheetEvent()
        data object DeleteProofsDismissed : BottomSheetEvent()
    }
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(val screenRoute: String) : Navigation()
        data object Pop : Navigation()
    }
}

@KoinViewModel
class SettingsViewModel(
    private val settingsInteractor: SettingsInteractor,
    private val logController: LogController,
) : MviViewModel<Event, State, Effect>() {

    private var job: Job? = null

    override fun setInitialState(): State = State()

    override fun handleEvents(event: Event) {
        when (event) {

            Event.GoBack -> {
                setEffect { Effect.Navigation.Pop }
            }

            Event.DeleteProofsClicked -> {
                setState { copy(showDeleteProofsDialog = true) }
            }

            Event.BottomSheetEvent.DeleteProofsConfirmed -> {
                performDocumentDeletion()
            }

            Event.BottomSheetEvent.DeleteProofsDismissed -> {
                setState { copy(showDeleteProofsDialog = false) }
            }

            Event.ChangePinClicked -> {
                setEffect { Effect.Navigation.SwitchScreen(getPinChangeRoute()) }
            }
        }
    }

    private fun performDocumentDeletion() {
        job?.cancel()
        job = viewModelScope.launch {
            setState { copy(isLoading = true, showDeleteProofsDialog = false) }
            settingsInteractor.deleteAllDocuments().collect { state ->
                when (state) {
                    is DeleteAgeDocumentsPartialState.Success -> {
                        logController.d("SettingsViM") { "Successfully deleted documents" }
                        switchToEnrollmentPage()
                    }

                    is DeleteAgeDocumentsPartialState.Failure -> {
                        closeDialog()
                        logController.e("SettingsVM") { "Error deleting documents: ${state.errorMessage}" }
                        // Optionally, show an error dialog or toast here
                    }

                    is DeleteAgeDocumentsPartialState.NoDocuments -> {
                        switchToEnrollmentPage()
                    }
                }
            }
        }
    }

    private fun switchToEnrollmentPage() {
        setEffect { Effect.Navigation.SwitchScreen(OnboardingScreens.Enrollment.screenRoute) }
    }

    private fun closeDialog() {
        setState { copy(isLoading = false, showDeleteProofsDialog = false) }
    }

    private fun getPinChangeRoute(): String {
        return generateComposableNavigationLink(
            screen = CommonScreens.QuickPin,
            arguments = generateComposableArguments(mapOf("pinFlow" to PinFlow.UPDATE))
        )
    }
}
