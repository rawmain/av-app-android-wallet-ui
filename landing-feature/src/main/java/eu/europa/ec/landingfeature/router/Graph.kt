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

package eu.europa.ec.landingfeature.router

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import eu.europa.ec.landingfeature.BuildConfig
import eu.europa.ec.landingfeature.ui.dashboard.LandingScreen
import eu.europa.ec.uilogic.navigation.LandingScreens
import eu.europa.ec.uilogic.navigation.ModuleRoute
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.featureLandingGraph(navController: NavController) {
    navigation(
        startDestination = LandingScreens.Landing.screenRoute,
        route = ModuleRoute.LandingModule.route
    ) {
        composable(
            route = LandingScreens.Landing.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + LandingScreens.Landing.screenRoute
                }
            )
        ) {
            LandingScreen(
                hostNavController = navController,
                viewModel = koinViewModel(),
            )
        }
    }
}