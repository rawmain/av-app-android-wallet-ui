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

import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.serializer.UiSerializer
import org.koin.android.annotation.KoinViewModel

private const val TAG = "PassportLiveVideoViewModel"

data class State(
    val isLoading: Boolean = false,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object OnBackPressed : Event()
    data object OnLiveVideoCapture : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object GoBack : Navigation()
        data class SwitchScreen(val screenRoute: String, val inclusive: Boolean) : Navigation()
    }
}

@KoinViewModel
class PassportLiveVideoViewModel(
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
    private val logController: LogController
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State = State()

    override fun handleEvents(event: Event) {
        when(event) {

            // Do pre-processing of screen
            Event.Init -> { logController.i(TAG) { "Event invoked Init" } }

            // Navigate to back since user fired back button
            Event.OnBackPressed -> setEffect {
                logController.i(TAG) { "Event invoked OnBackPressed " }
                Effect.Navigation.GoBack
            }

            Event.OnLiveVideoCapture -> {
                logController.i(TAG) { "Event invoked OnLiveVideoCapture" }
                processRequestedNavigation()
            }
        }
    }

    private fun processRequestedNavigation() {
        setEffect {
            Effect.Navigation.SwitchScreen(
                // fixme : replace the TAG with the generateComposableNavigationLink
                TAG, false
            )
        }
    }
}
