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

package eu.europa.ec.onboardingfeature.di

import android.content.Context
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractor
import eu.europa.ec.corelogic.controller.PassportScanningDocumentsController
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.onboardingfeature.controller.FaceMatchController
import eu.europa.ec.onboardingfeature.controller.FaceMatchControllerImpl
import eu.europa.ec.onboardingfeature.interactor.ConsentInteractor
import eu.europa.ec.onboardingfeature.interactor.ConsentInteractorImpl
import eu.europa.ec.onboardingfeature.interactor.EnrollmentInteractor
import eu.europa.ec.onboardingfeature.interactor.EnrollmentInteractorImpl
import eu.europa.ec.onboardingfeature.interactor.PassportConsentInteractor
import eu.europa.ec.onboardingfeature.interactor.PassportConsentInteractorImpl
import eu.europa.ec.onboardingfeature.interactor.PassportIdentificationInteractor
import eu.europa.ec.onboardingfeature.interactor.PassportIdentificationInteractorImpl
import eu.europa.ec.onboardingfeature.interactor.PassportLiveVideoInteractor
import eu.europa.ec.onboardingfeature.interactor.PassportLiveVideoInteractorImpl
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.serializer.UiSerializer
import kl.open.fmandroid.FaceMatchSDK
import kl.open.fmandroid.FaceMatchSdkImpl
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("eu.europa.ec.onboardingfeature")
class FeatureOnboardingModule

@Factory
fun provideConsentInteractor(): ConsentInteractor = ConsentInteractorImpl()

@Factory
fun provideEnrollmentInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    resourceProvider: ResourceProvider,
    uiSerializer: UiSerializer,
): EnrollmentInteractor = EnrollmentInteractorImpl(
    walletCoreDocumentsController,
    deviceAuthenticationInteractor,
    resourceProvider,
    uiSerializer
)

@Single
fun provideFaceMatchSDK(context: Context): FaceMatchSDK {
    return FaceMatchSdkImpl(context.applicationContext).apply {
        val configJson = context.assets.open("keyless_config.json")
            .bufferedReader()
            .use { it.readText() }
        init(configJson)
    }
}

@Factory
fun provideFaceMatchController(
    faceMatchSDK: FaceMatchSDK,
): FaceMatchController = FaceMatchControllerImpl(faceMatchSDK)

@Factory
fun providePassportLiveVideoInteractor(
    faceMatchController: FaceMatchController,
    resourceProvider: ResourceProvider,
    logController: LogController,
): PassportLiveVideoInteractor = PassportLiveVideoInteractorImpl(
    faceMatchController,
    resourceProvider,
    logController
)

@Factory
fun providePassportIdentificationInteractor(
    resourceProvider: ResourceProvider,
    logController: LogController,
): PassportIdentificationInteractor = PassportIdentificationInteractorImpl(
    resourceProvider,
    logController
)

@Factory
fun providePassportConsentInteractor(
    passportScanningDocumentsController: PassportScanningDocumentsController,
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    resourceProvider: ResourceProvider,
    uiSerializer: UiSerializer,
): PassportConsentInteractor = PassportConsentInteractorImpl(
    passportScanningDocumentsController,
    deviceAuthenticationInteractor,
    resourceProvider,
    uiSerializer
)
