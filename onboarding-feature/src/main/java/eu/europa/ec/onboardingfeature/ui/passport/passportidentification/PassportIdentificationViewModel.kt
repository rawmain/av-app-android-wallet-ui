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
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.passportscanner.SmartScannerActivity
import eu.europa.ec.passportscanner.scanner.config.CaptureOptions
import eu.europa.ec.passportscanner.scanner.config.CaptureType
import eu.europa.ec.passportscanner.scanner.config.Config
import eu.europa.ec.passportscanner.scanner.config.ScannerOptions
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewEvent
import eu.europa.ec.uilogic.mvi.ViewSideEffect
import eu.europa.ec.uilogic.mvi.ViewState
import org.koin.android.annotation.KoinViewModel

data class State(
    val isLoading: Boolean = false,
) : ViewState

sealed class Event : ViewEvent {
    data object Init : Event()
    data object OnBackPressed : Event()
    data object OnStartPassportScan : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data object GoBack : Navigation()
        data class SwitchScreen(val screenRoute: String, val inclusive: Boolean) : Navigation()
        data class StartMRZScanner(val intent: Intent) : Navigation()
    }
}

@KoinViewModel
class PassportIdentificationViewModel(
    private val resourceProvider: ResourceProvider,
    private val logController: LogController,
    private val context: Context
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State = State()

    override fun handleEvents(event: Event) {
        when(event) {

            Event.Init -> logController.i { "Init -- PassportIdentificationViewModel " }
            Event.OnBackPressed -> setEffect { Effect.Navigation.GoBack }

            Event.OnStartPassportScan -> {
                val intent = Intent(context, SmartScannerActivity::class.java)
                intent.putExtra(
                    SmartScannerActivity.SCANNER_OPTIONS,
                    ScannerOptions(
                        config = Config(
                            header = resourceProvider.getString(eu.europa.ec.resourceslogic.R.string.passport_identification_capture),
                            subHeader = resourceProvider.getString(eu.europa.ec.resourceslogic.R.string.passport_identification_title),
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
                setEffect { Effect.Navigation.StartMRZScanner(intent) }
            }
        }
    }
}
