/*
 * Copyright (c) 2025 European Commission
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
import eu.europa.ec.commonfeature.interactor.DeviceAuthenticationInteractor
import eu.europa.ec.corelogic.controller.FetchScopedDocumentsPartialState
import eu.europa.ec.corelogic.controller.IssuanceMethod
import eu.europa.ec.corelogic.controller.IssueDocumentPartialState
import eu.europa.ec.corelogic.controller.PassportScanningDocumentsController
import eu.europa.ec.corelogic.model.ScopedDocumentDomain
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.util.mockedDefaultLocale
import eu.europa.ec.testfeature.util.mockedGenericErrorMessage
import eu.europa.ec.testfeature.util.mockedNotifyOnAuthenticationFailure
import eu.europa.ec.testfeature.util.mockedPlainFailureMessage
import eu.europa.ec.testfeature.util.mockedUriPath1
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.extension.toFlow
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import eu.europa.ec.uilogic.component.AppIcons
import eu.europa.ec.uilogic.serializer.UiSerializer
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TestPassportCredentialIssuanceInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var passportScanningDocumentsController: PassportScanningDocumentsController

    @Mock
    private lateinit var deviceAuthenticationInteractor: DeviceAuthenticationInteractor

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var uiSerializer: UiSerializer

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var resultHandler: DeviceAuthenticationResult

    private lateinit var interactor: PassportCredentialIssuanceInteractor

    private lateinit var closeable: AutoCloseable

    private lateinit var crypto: BiometricCrypto

    private val mockedAgeVerificationDocument = ScopedDocumentDomain(
        name = "Age Verification",
        configurationId = "age-verification-config",
        credentialIssuerId = "issuer-id",
        formatType = null,
        isPid = false,
        isAgeVerification = true
    )

    private val mockedScopedDocumentsWithAgeVerification = listOf(
        mockedAgeVerificationDocument,
        ScopedDocumentDomain(
            name = "Other Document",
            configurationId = "other-config",
            credentialIssuerId = "issuer-id",
            formatType = null,
            isPid = false,
            isAgeVerification = false
        )
    )

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = PassportCredentialIssuanceInteractorImpl(
            passportScanningDocumentsController = passportScanningDocumentsController,
            deviceAuthenticationInteractor = deviceAuthenticationInteractor,
            resourceProvider = resourceProvider,
            uiSerializer = uiSerializer
        )

        crypto = BiometricCrypto(cryptoObject = null)

        whenever(resourceProvider.genericErrorMessage()).thenReturn(mockedGenericErrorMessage)
        whenever(resourceProvider.getLocale()).thenReturn(mockedDefaultLocale)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region issuePassportScanningDocument

    // Case 1: Success case with age verification document
    @Test
    fun `Given age verification document exists, When issuePassportScanningDocument is called, Then Success state is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(
                passportScanningDocumentsController.getPassportScanningScopedDocuments(any())
            ).thenReturn(
                FetchScopedDocumentsPartialState.Success(mockedScopedDocumentsWithAgeVerification)
            )
            whenever(
                passportScanningDocumentsController.issuePassportScanningDocument(
                    issuanceMethod = eq(IssuanceMethod.OPENID4VCI),
                    configId = any()
                )
            ).thenReturn(IssueDocumentPartialState.Success("documentId").toFlow())

            // When
            interactor.issuePassportScanningDocument(context).runFlowTest {
                // Then
                assertEquals(
                    CredentialIssuancePartialState.Success("documentId"),
                    awaitItem()
                )
            }
        }
    }

    // Case 2: Failure case when no age verification document exists
    @Test
    fun `Given no age verification document exists, When issuePassportScanningDocument is called, Then Failure state is returned`() {
        coroutineRule.runTest {
            // Given
            val documentsWithoutAgeVerification = listOf(
                ScopedDocumentDomain(
                    name = "Other Document",
                    configurationId = "other-config",
                    credentialIssuerId = "issuer-id",
                    formatType = null,
                    isPid = false,
                    isAgeVerification = false
                )
            )
            whenever(
                passportScanningDocumentsController.getPassportScanningScopedDocuments(any())
            ).thenReturn(
                FetchScopedDocumentsPartialState.Success(documentsWithoutAgeVerification)
            )

            // When
            interactor.issuePassportScanningDocument(context).runFlowTest {
                // Then
                assertEquals(
                    CredentialIssuancePartialState.Failure(mockedGenericErrorMessage),
                    awaitItem()
                )
            }
        }
    }

    // Case 3: Failure case when getPassportScanningScopedDocuments fails
    @Test
    fun `Given getPassportScanningScopedDocuments fails, When issuePassportScanningDocument is called, Then Failure state is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(
                passportScanningDocumentsController.getPassportScanningScopedDocuments(any())
            ).thenReturn(
                FetchScopedDocumentsPartialState.Failure(mockedPlainFailureMessage)
            )

            // When
            interactor.issuePassportScanningDocument(context).runFlowTest {
                // Then
                assertEquals(
                    CredentialIssuancePartialState.Failure(mockedPlainFailureMessage),
                    awaitItem()
                )
            }
        }
    }

    // Case 4: DeferredSuccess case
    @Test
    fun `Given document issuance is deferred, When issuePassportScanningDocument is called, Then DeferredSuccess state is returned`() {
        coroutineRule.runTest {
            // Given
            mockDocumentIssuanceStrings()
            whenever(
                passportScanningDocumentsController.getPassportScanningScopedDocuments(any())
            ).thenReturn(
                FetchScopedDocumentsPartialState.Success(mockedScopedDocumentsWithAgeVerification)
            )
            whenever(
                passportScanningDocumentsController.issuePassportScanningDocument(
                    issuanceMethod = eq(IssuanceMethod.OPENID4VCI),
                    configId = any()
                )
            ).thenReturn(
                IssueDocumentPartialState.DeferredSuccess(
                    mapOf(
                        Pair(
                            "documentId",
                            "formatType"
                        )
                    )
                ).toFlow()
            )

            // When
            interactor.issuePassportScanningDocument(context).runFlowTest {
                // Then
                val item = awaitItem()
                assert(item is CredentialIssuancePartialState.DeferredSuccess)
                assert(
                    (item as CredentialIssuancePartialState.DeferredSuccess).successRoute.contains(
                        "SUCCESS"
                    )
                )
            }
        }
    }

    // Case 5: UserAuthRequired case
    @Test
    fun `Given user authentication is required, When issuePassportScanningDocument is called, Then UserAuthRequired state is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(
                passportScanningDocumentsController.getPassportScanningScopedDocuments(any())
            ).thenReturn(
                FetchScopedDocumentsPartialState.Success(mockedScopedDocumentsWithAgeVerification)
            )
            whenever(
                passportScanningDocumentsController.issuePassportScanningDocument(
                    issuanceMethod = eq(IssuanceMethod.OPENID4VCI),
                    configId = any()
                )
            ).thenReturn(IssueDocumentPartialState.UserAuthRequired(crypto, resultHandler).toFlow())

            // When
            interactor.issuePassportScanningDocument(context).runFlowTest {
                // Then
                assertEquals(
                    CredentialIssuancePartialState.UserAuthRequired(crypto, resultHandler),
                    awaitItem()
                )
            }
        }
    }

    // Case 6: Failure case when issuePassportScanningDocument fails
    @Test
    fun `Given issuePassportScanningDocument fails, When issuePassportScanningDocument is called, Then Failure state is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(
                passportScanningDocumentsController.getPassportScanningScopedDocuments(any())
            ).thenReturn(
                FetchScopedDocumentsPartialState.Success(mockedScopedDocumentsWithAgeVerification)
            )
            whenever(
                passportScanningDocumentsController.issuePassportScanningDocument(
                    issuanceMethod = eq(IssuanceMethod.OPENID4VCI),
                    configId = any()
                )
            ).thenReturn(IssueDocumentPartialState.Failure(mockedPlainFailureMessage).toFlow())

            // When
            interactor.issuePassportScanningDocument(context).runFlowTest {
                // Then
                assertEquals(
                    CredentialIssuancePartialState.Failure(mockedPlainFailureMessage),
                    awaitItem()
                )
            }
        }
    }
    //endregion

    //region handleUserAuth

    // Case 1: CanAuthenticate
    @Test
    fun `Given biometrics can authenticate, When handleUserAuth is called, Then authenticateWithBiometrics is called`() {
        // Given
        mockBiometricsAvailabilityResponse(
            response = BiometricsAvailability.CanAuthenticate
        )

        // When
        interactor.handleUserAuth(
            context = context,
            crypto = crypto,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            resultHandler = resultHandler
        )

        // Then
        verify(deviceAuthenticationInteractor, times(1))
            .authenticateWithBiometrics(
                context,
                crypto,
                mockedNotifyOnAuthenticationFailure,
                resultHandler
            )
    }

    // Case 2: NonEnrolled
    @Test
    fun `Given biometrics not enrolled, When handleUserAuth is called, Then launchBiometricSystemScreen is called`() {
        // Given
        mockBiometricsAvailabilityResponse(
            response = BiometricsAvailability.NonEnrolled
        )

        // When
        interactor.handleUserAuth(
            context = context,
            crypto = crypto,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            resultHandler = resultHandler
        )

        // Then
        verify(deviceAuthenticationInteractor, times(1))
            .launchBiometricSystemScreen()
    }

    // Case 3: Failure
    @Test
    fun `Given biometrics availability check fails, When handleUserAuth is called, Then onAuthenticationFailure is called`() {
        // Given
        mockBiometricsAvailabilityResponse(
            response = BiometricsAvailability.Failure(mockedPlainFailureMessage)
        )
        whenever(resultHandler.onAuthenticationFailure).thenReturn {}

        // When
        interactor.handleUserAuth(
            context = context,
            crypto = crypto,
            notifyOnAuthenticationFailure = mockedNotifyOnAuthenticationFailure,
            resultHandler = resultHandler
        )

        // Then
        verify(resultHandler, times(1))
            .onAuthenticationFailure
    }
    //endregion

    //region resumeOpenId4VciWithAuthorization

    @Test
    fun `When resumeOpenId4VciWithAuthorization is called, Then passportScanningDocumentsController resumeOpenId4VciWithAuthorization is called`() {
        // When
        interactor.resumeOpenId4VciWithAuthorization(mockedUriPath1)

        // Then
        verify(passportScanningDocumentsController, times(1))
            .resumePassportScanningOpenId4VciWithAuthorization(mockedUriPath1)
    }
    //endregion

    //region helper functions
    private fun mockBiometricsAvailabilityResponse(response: BiometricsAvailability) {
        whenever(deviceAuthenticationInteractor.getBiometricsAvailability(listener = any()))
            .thenAnswer {
                val bioAvailability = it.getArgument<(BiometricsAvailability) -> Unit>(0)
                bioAvailability(response)
            }
    }

    private fun mockDocumentIssuanceStrings() {
        whenever(resourceProvider.getString(R.string.issuance_add_document_deferred_success_text))
            .thenReturn("Success text")
        whenever(resourceProvider.getString(R.string.issuance_add_document_deferred_success_primary_button_text))
            .thenReturn("Primary button text")
        whenever(resourceProvider.getString(AppIcons.InProgress.contentDescriptionId))
            .thenReturn("Content description")
        whenever(resourceProvider.getString(R.string.issuance_add_document_deferred_success_description))
            .thenReturn("Success description")
    }
    //endregion
}
