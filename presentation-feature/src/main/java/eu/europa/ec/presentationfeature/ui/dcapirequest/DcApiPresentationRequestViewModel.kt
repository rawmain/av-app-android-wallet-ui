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

package eu.europa.ec.presentationfeature.ui.dcapirequest

import android.content.Intent
import eu.europa.ec.commonfeature.config.PresentationMode
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.commonfeature.interactor.PresentationRequestInteractor
import eu.europa.ec.commonfeature.ui.request.RequestViewModel
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.uilogic.navigation.PresentationScreens
import eu.europa.ec.uilogic.serializer.UiSerializer
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam

@KoinViewModel
class DcApiPresentationRequestViewModel(
    interactor: PresentationRequestInteractor,
    resourceProvider: ResourceProvider,
    uiSerializer: UiSerializer,
    @InjectedParam private val intent: Intent
) : RequestViewModel(interactor, resourceProvider, uiSerializer) {

    override fun init() {
        handleIntent(intent)
    }

    override fun getNextScreen(): String {
        return createBiometricScreen(PresentationScreens.DcApiPresentationRequest)
    }
    override fun requestUriConfig(): RequestUriConfig {
        return RequestUriConfig(mode = PresentationMode.DcApi)
    }

    fun handleIntent(intent: Intent) {
        interactor.startDCAPIPresentation(intent)
    }
}
