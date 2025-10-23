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

package eu.europa.ec.onboardingfeature.ui.passport.passportscanintro

import android.content.Context
import androidx.lifecycle.viewModelScope
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.onboardingfeature.interactor.PassportScanIntroInteractor
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.OnboardingScreens
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

private const val TAG = "PassportScanIntroViewModel"

data class State(
    val isLoading: Boolean = false,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object OnBackPressed : Event()
    data class OnStartProcedure(val context: Context) : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object GoBack : Navigation()
        data class SwitchScreen(val screenRoute: String, val inclusive: Boolean) : Navigation()
    }
}

@KoinViewModel
class PassportScanIntroViewModel(
    private val logController: LogController,
    private val passportScanIntroInteractor: PassportScanIntroInteractor,
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State = State()

    override fun handleEvents(event: Event) {
        when (event) {

            is Event.Init -> logController.i(TAG) { "Init - Event invoked" }
            is Event.OnBackPressed -> setEffect { Effect.Navigation.GoBack }

            is Event.OnStartProcedure -> {
                viewModelScope.launch {
                    passportScanIntroInteractor.init(event.context)
                }

                setEffect {
                    Effect.Navigation.SwitchScreen(
                        OnboardingScreens.DocumentIdentification.screenRoute,
                        inclusive = false
                    )
                }
            }
        }
    }
}
