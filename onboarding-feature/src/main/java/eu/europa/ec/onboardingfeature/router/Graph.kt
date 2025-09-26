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

package eu.europa.ec.onboardingfeature.router

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import eu.europa.ec.onboardingfeature.ui.consent.ConsentScreen
import eu.europa.ec.onboardingfeature.ui.enrollment.EnrollmentScreen
import eu.europa.ec.onboardingfeature.ui.passport.passportbiometrics.PassportBiometricScreen
import eu.europa.ec.onboardingfeature.ui.passport.passportidentification.PassportIdentificationScreen
import eu.europa.ec.onboardingfeature.ui.passport.passportscanintro.PassportScanIntroScreen
import eu.europa.ec.onboardingfeature.ui.qrscanintro.QRScanIntroScreen
import eu.europa.ec.onboardingfeature.ui.welcome.WelcomeScreen
import eu.europa.ec.uilogic.navigation.ModuleRoute
import eu.europa.ec.uilogic.navigation.OnboardingScreens
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.featureOnboardingGraph(navController: NavController) {
    navigation(
        startDestination = OnboardingScreens.Welcome.screenRoute,
        route = ModuleRoute.OnboardingModule.route
    ) {

        composable(route = OnboardingScreens.Welcome.screenRoute) {
            WelcomeScreen(navController, koinViewModel())
        }

        composable(route = OnboardingScreens.Consent.screenRoute) {
            ConsentScreen(navController, koinViewModel())
        }

        composable(route = OnboardingScreens.Enrollment.screenRoute) {
            EnrollmentScreen(navController, koinViewModel())
        }

        composable(route = OnboardingScreens.PassportScanIntro.screenRoute) {
            PassportScanIntroScreen(navController, koinViewModel())
        }

        composable(route = OnboardingScreens.PassportIdentification.screenRoute) {
            PassportIdentificationScreen(navController, koinViewModel())
        }

        composable(route = OnboardingScreens.PassportBiometrics.screenRoute) {
            PassportBiometricScreen(navController, koinViewModel())
        }

        composable(route = OnboardingScreens.QRScanIntro.screenRoute) {
            QRScanIntroScreen(navController, koinViewModel())
        }
    }
}