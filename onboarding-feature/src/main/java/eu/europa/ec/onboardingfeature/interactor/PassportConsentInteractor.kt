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

package eu.europa.ec.onboardingfeature.interactor

import android.content.Context
import eu.europa.ec.authenticationlogic.controller.authentication.BiometricsAvailability
import eu.europa.ec.authenticationlogic.controller.authentication.DeviceAuthenticationResult
import eu.europa.ec.authenticationlogic.model.BiometricCrypto
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.commonfeature.config.SuccessUIConfig
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractor
import eu.europa.ec.corelogic.controller.FetchScopedDocumentsPartialState
import eu.europa.ec.corelogic.controller.IssuanceMethod
import eu.europa.ec.corelogic.controller.IssueDocumentPartialState
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.model.ScopedDocument
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.resourceslogic.theme.values.ThemeColors
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.component.utils.PERCENTAGE_25
import eu.europa.ec.uilogic.config.ConfigNavigation
import eu.europa.ec.uilogic.config.NavigationType
import eu.europa.ec.uilogic.navigation.CommonScreens
import eu.europa.ec.uilogic.navigation.LandingScreens
import eu.europa.ec.uilogic.navigation.helper.generateComposableArguments
import eu.europa.ec.uilogic.navigation.helper.generateComposableNavigationLink
import eu.europa.ec.uilogic.serializer.UiSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val TAG = "PassportConsentInteractor"

sealed class PassportConsentPartialState {
    data class Success(val documentId: String) : PassportConsentPartialState()
    data class DeferredSuccess(val successRoute: String) : PassportConsentPartialState()
    data class UserAuthRequired(
        val crypto: BiometricCrypto,
        val resultHandler: DeviceAuthenticationResult,
    ) : PassportConsentPartialState()

    data class Failure(val error: String) : PassportConsentPartialState()
}

interface PassportConsentInteractor {
    fun issueDocument(context: Context): Flow<PassportConsentPartialState>
    fun handleUserAuth(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult,
    )

    fun resumeOpenId4VciWithAuthorization(uri: String)
}

class PassportConsentInteractorImpl(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    private val resourceProvider: ResourceProvider,
    private val uiSerializer: UiSerializer,
    private val logController: LogController,
) : PassportConsentInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override fun issueDocument(context: Context): Flow<PassportConsentPartialState> = flow {
        logController.i(TAG) { "Starting document issuance flow" }
        when (val state =
            walletCoreDocumentsController.getScopedDocuments(resourceProvider.getLocale())) {
            is FetchScopedDocumentsPartialState.Success -> {
                val ageVerificationDocument: ScopedDocument? = state.documents
                    .firstOrNull { it.isAgeVerification }

                if (ageVerificationDocument == null) {
                    logController.e(TAG) { "No age verification document found" }
                    emit(PassportConsentPartialState.Failure(genericErrorMsg))
                    return@flow
                }

                logController.i(TAG) { "Issuing age verification document: ${ageVerificationDocument.configurationId}" }
                walletCoreDocumentsController.issueDocument(
                    issuanceMethod = IssuanceMethod.OPENID4VCI,
                    configId = ageVerificationDocument.configurationId
                ).collect { issueState ->
                    logController.i(TAG) { "Received issueState: ${issueState::class.simpleName}" }
                    when (issueState) {
                        is IssueDocumentPartialState.Success -> {
                            logController.i(TAG) { "Document issued successfully: ${issueState.documentId}" }
                            emit(PassportConsentPartialState.Success(issueState.documentId))
                        }

                        is IssueDocumentPartialState.DeferredSuccess -> {
                            logController.i(TAG) { "Document issuance deferred" }
                            val successRoute = buildGenericSuccessRouteForDeferred()
                            emit(PassportConsentPartialState.DeferredSuccess(successRoute))
                        }

                        is IssueDocumentPartialState.UserAuthRequired -> {
                            logController.i(TAG) { "User authentication required for document issuance" }
                            emit(
                                PassportConsentPartialState.UserAuthRequired(
                                    issueState.crypto,
                                    issueState.resultHandler
                                )
                            )
                        }

                        is IssueDocumentPartialState.Failure -> {
                            logController.e(TAG) { "Document issuance failed: ${issueState.errorMessage}" }
                            emit(PassportConsentPartialState.Failure(issueState.errorMessage))
                        }
                    }
                }
            }

            is FetchScopedDocumentsPartialState.Failure -> {
                logController.e(TAG) { "Failed to fetch scoped documents: ${state.errorMessage}" }
                emit(PassportConsentPartialState.Failure(state.errorMessage))
            }
        }
    }

    private fun buildGenericSuccessRouteForDeferred(): String {
        val navigation = ConfigNavigation(
            navigationType = NavigationType.PopTo(
                screen = LandingScreens.Landing
            )
        )
        val successScreenArguments = getSuccessScreenArgumentsForDeferred(navigation)
        return generateComposableNavigationLink(
            screen = CommonScreens.Success,
            arguments = successScreenArguments
        )
    }

    private fun getSuccessScreenArgumentsForDeferred(
        navigation: ConfigNavigation,
    ): String {
        val (textElementsConfig, imageConfig, buttonText) = Triple(
            first = SuccessUIConfig.TextElementsConfig(
                text = resourceProvider.getString(R.string.issuance_add_document_deferred_success_text),
                description = resourceProvider.getString(R.string.issuance_add_document_deferred_success_description),
                color = ThemeColors.pending
            ),
            second = SuccessUIConfig.ImageConfig(
                type = SuccessUIConfig.ImageConfig.Type.Drawable(icon = AppIcons.InProgress),
                tint = ThemeColors.primary,
                screenPercentageSize = PERCENTAGE_25,
            ),
            third = resourceProvider.getString(R.string.issuance_add_document_deferred_success_primary_button_text)
        )

        return generateComposableArguments(
            mapOf(
                SuccessUIConfig.serializedKeyName to uiSerializer.toBase64(
                    SuccessUIConfig(
                        textElementsConfig = textElementsConfig,
                        imageConfig = imageConfig,
                        buttonConfig = listOf(
                            SuccessUIConfig.ButtonConfig(
                                text = buttonText,
                                style = SuccessUIConfig.ButtonConfig.Style.PRIMARY,
                                navigation = navigation
                            )
                        ),
                        onBackScreenToNavigate = navigation,
                    ),
                    SuccessUIConfig.Parser
                ).orEmpty()
            )
        )
    }

    override fun handleUserAuth(
        context: Context,
        crypto: BiometricCrypto,
        notifyOnAuthenticationFailure: Boolean,
        resultHandler: DeviceAuthenticationResult,
    ) {
        deviceAuthenticationInteractor.getBiometricsAvailability {
            when (it) {
                is BiometricsAvailability.CanAuthenticate -> {
                    deviceAuthenticationInteractor.authenticateWithBiometrics(
                        context = context,
                        crypto = crypto,
                        notifyOnAuthenticationFailure = notifyOnAuthenticationFailure,
                        resultHandler = resultHandler
                    )
                }

                is BiometricsAvailability.NonEnrolled -> {
                    deviceAuthenticationInteractor.launchBiometricSystemScreen()
                }

                is BiometricsAvailability.Failure -> {
                    resultHandler.onAuthenticationFailure()
                }
            }
        }
    }

    override fun resumeOpenId4VciWithAuthorization(uri: String) {
        logController.i(TAG) { "Resuming OpenId4VCI with authorization: $uri" }
        walletCoreDocumentsController.resumeOpenId4VciWithAuthorization(uri)
    }
}
