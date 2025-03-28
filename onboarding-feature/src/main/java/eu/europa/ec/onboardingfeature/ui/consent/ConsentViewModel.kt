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

package eu.europa.ec.onboardingfeature.ui.consent

import eu.europa.ec.commonfeature.model.PinFlow
import eu.europa.ec.uilogic.mvi.MviViewModel
import eu.europa.ec.uilogic.mvi.ViewState
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import org.koin.android.annotation.KoinViewModel

data class State(
    val placeholder: Boolean = false
) : ViewState

@KoinViewModel
class ConsentViewModel : MviViewModel<Nothing, State, Nothing>() {
    override fun setInitialState(): State = State()

    override fun handleEvents(event: Nothing) {
        // No-op for now
    }

    fun getQuickPinConfig(): String {
        return generateComposableNavigationLink(
            screen = CommonScreens.QuickPin,
            arguments = generateComposableArguments(mapOf("pinFlow" to PinFlow.CREATE))
        )
    }
}