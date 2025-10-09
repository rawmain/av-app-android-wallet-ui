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
import androidx.navigation.NavType.Companion.StringType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import eu.europa.ec.onboardingfeature.BuildConfig
import eu.europa.ec.onboardingfeature.config.PassportCredentialIssuanceUiConfig
import eu.europa.ec.onboardingfeature.config.PassportLiveVideoUiConfig
import eu.europa.ec.onboardingfeature.ui.consent.ConsentScreen
import eu.europa.ec.onboardingfeature.ui.enrollment.EnrollmentScreen
import eu.europa.ec.onboardingfeature.ui.passport.passportbiometrics.PassportBiometricScreen
import eu.europa.ec.onboardingfeature.ui.passport.passportcredentialissuance.PassportCredentialIssuanceScreen
import eu.europa.ec.onboardingfeature.ui.passport.passportidentification.PassportIdentificationScreen
import eu.europa.ec.onboardingfeature.ui.passport.passportlivevideo.PassportLiveVideoScreen
import eu.europa.ec.onboardingfeature.ui.passport.passportscanintro.PassportScanIntroScreen
import eu.europa.ec.onboardingfeature.ui.qrscanintro.QRScanIntroScreen
import eu.europa.ec.onboardingfeature.ui.welcome.WelcomeScreen
import eu.europa.ec.uilogic.navigation.ModuleRoute
import eu.europa.ec.uilogic.navigation.OnboardingScreens
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

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

        composable(
            route = OnboardingScreens.PassportLiveVideo.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + OnboardingScreens.PassportLiveVideo.screenRoute
                },
            ),
            arguments = listOf(
                navArgument(PassportLiveVideoUiConfig.serializedKeyName) {
                    type = StringType
                }
            )
        ) {
            PassportLiveVideoScreen(
                navController, koinViewModel(
                parameters = {
                    parametersOf(
                        it.arguments?.getString(PassportLiveVideoUiConfig.serializedKeyName)
                            .orEmpty()
                    )
                }
            ))
        }

        composable(
            route = OnboardingScreens.PassportCredentialIssuance.screenRoute,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern =
                        BuildConfig.DEEPLINK + OnboardingScreens.PassportCredentialIssuance.screenRoute
                },
            ),
            arguments = listOf(
                navArgument(PassportCredentialIssuanceUiConfig.serializedKeyName) {
                    type = StringType
                }
            )
        ) {
            PassportCredentialIssuanceScreen(
                navController, koinViewModel(
                    parameters = {
                        parametersOf(
                            it.arguments?.getString(PassportCredentialIssuanceUiConfig.serializedKeyName)
                                .orEmpty()
                        )
                    }
                ))
        }

        composable(route = OnboardingScreens.QRScanIntro.screenRoute) {
            QRScanIntroScreen(navController, koinViewModel())
        }
    }
}