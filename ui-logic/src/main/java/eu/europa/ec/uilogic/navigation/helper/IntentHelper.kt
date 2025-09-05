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

package eu.europa.ec.uilogic.navigation.helper

import android.content.Intent
import android.util.Log
import androidx.navigation.NavController
import eu.europa.ec.uilogic.navigation.PresentationScreens
import eu.europa.ec.uilogic.navigation.Screen

data class IntentAction(val intent: Intent, val type: IntentType)

enum class IntentType {
    DC_API_PRESENTATION
}

fun handleIntentAction(
    navController: NavController,
    action: IntentAction
) {
    val screen: Screen

    when (action.type) {
        IntentType.DC_API_PRESENTATION -> {
            screen = PresentationScreens.DcApiPresentationRequest
        }
    }

    Log.d("Intent handling", "Navigating to: ${screen.screenName} with intent")
    navController.currentBackStackEntry?.savedStateHandle?.set("intentToHandle", action.intent)
    navController.navigate("${screen.screenRoute}")
}
