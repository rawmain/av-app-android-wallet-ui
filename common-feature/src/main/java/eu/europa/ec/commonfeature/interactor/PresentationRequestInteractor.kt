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

package eu.europa.ec.commonfeature.interactor

import android.content.Intent
import eu.europa.ec.commonfeature.config.RequestUriConfig
import eu.europa.ec.commonfeature.ui.request.model.RequestDocumentItemUi
import kotlinx.coroutines.flow.Flow


sealed class PresentationRequestInteractorPartialState {
    data class Success(
        val verifierName: String?,
        val verifierIsTrusted: Boolean,
        val requestDocuments: List<RequestDocumentItemUi>
    ) : PresentationRequestInteractorPartialState()

    data class NoData(
        val verifierName: String?,
        val verifierIsTrusted: Boolean,
    ) : PresentationRequestInteractorPartialState()

    data class Failure(val error: String) : PresentationRequestInteractorPartialState()
    data object Disconnect : PresentationRequestInteractorPartialState()
}


interface PresentationRequestInteractor {
    fun getRequestDocuments(): Flow<PresentationRequestInteractorPartialState>
    fun stopPresentation()
    fun updateRequestedDocuments(items: List<RequestDocumentItemUi>)
    fun setConfig(config: RequestUriConfig)
    fun startDCAPIPresentation(intent: Intent)
}
