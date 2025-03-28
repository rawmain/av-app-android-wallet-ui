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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import eu.europa.ec.onboardingfeature.ui.components.TopStepBar
import eu.europa.ec.uilogic.component.content.ContentScreen
import eu.europa.ec.uilogic.component.content.ScreenNavigateAction
import eu.europa.ec.uilogic.component.wrap.ButtonConfig
import eu.europa.ec.uilogic.component.wrap.ButtonType
import eu.europa.ec.uilogic.component.wrap.StickyBottomConfig
import eu.europa.ec.uilogic.component.wrap.StickyBottomType
import eu.europa.ec.uilogic.component.wrap.WrapStickyBottomContent

@Composable
fun ConsentScreen(navController: NavController, viewModel: ConsentViewModel) {
    val nextScreenRoute = viewModel.getQuickPinConfig()
    val config = ButtonConfig(
        type = ButtonType.PRIMARY,
        onClick = { navController.navigate(nextScreenRoute) })

    ContentScreen(isLoading = false,
        navigatableAction = ScreenNavigateAction.NONE,
        onBack = { },
        stickyBottom = { paddingValues ->
            WrapStickyBottomContent(
                stickyBottomModifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues),
                stickyBottomConfig = StickyBottomConfig(
                    type = StickyBottomType.OneButton(config = config),
                    showDivider = false
                )
            ) {
                Text(text = "Confirm") // string resources TBD in the next PR
            }
        }) { _ ->
        Content()
    }
}

@Composable
private fun Content() {
    TopStepBar(
        currentStep = 1
    )
}
