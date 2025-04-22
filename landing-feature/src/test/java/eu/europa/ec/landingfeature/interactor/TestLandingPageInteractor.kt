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

package eu.europa.ec.landingfeature.interactor

import eu.europa.ec.commonfeature.util.transformPathsToDomainClaims
import eu.europa.ec.corelogic.controller.WalletCoreDocumentsController
import eu.europa.ec.corelogic.extension.toClaimPaths
import eu.europa.ec.landingfeature.interactor.LandingPageInteractor.GetAgeCredentialPartialState
import eu.europa.ec.landingfeature.model.AgeCredentialUi
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider
import eu.europa.ec.testfeature.mockedAgeVerificationDocument
import eu.europa.ec.testfeature.mockedDefaultLocale
import eu.europa.ec.testfeature.mockedExceptionWithMessage
import eu.europa.ec.testfeature.mockedExceptionWithNoMessage
import eu.europa.ec.testfeature.mockedGenericErrorMessage
import eu.europa.ec.testfeature.mockedPlainFailureMessage
import eu.europa.ec.testlogic.extension.runFlowTest
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class TestLandingPageInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var walletCoreDocumentsController: WalletCoreDocumentsController

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var interactor: LandingPageInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = LandingPageInteractorImpl(
            walletCoreDocumentsController = walletCoreDocumentsController,
            resourceProvider = resourceProvider,
        )

        whenever(resourceProvider.genericErrorMessage()).thenReturn(mockedGenericErrorMessage)
        whenever(resourceProvider.getString(R.string.landing_screen_no_age_credential_found)).thenReturn(
            mockedPlainFailureMessage
        )
        whenever(resourceProvider.getLocale()).thenReturn(mockedDefaultLocale)
    }

    @After
    fun after() {
        closeable.close()
    }

    //region getAgeCredential

    // Case 1:
    // 1. walletCoreDocumentsController.getAllDocuments() returns a list containing an age credential document.
    // 2. The document is successfully transformed into an AgeCredentialUi.

    // Case 1 Expected Result:
    // LandingPageInteractorGetAgeCredentialPartialState.Success state with the transformed AgeCredentialUi.
    @Test
    fun `Given Case 1, When getAgeCredential is called, Then Case 1 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getAgeOver18IssuedDocument())
                .thenReturn(mockedAgeVerificationDocument)

            whenever(resourceProvider.getString(any())).thenReturn("mockedString")

            // When
            interactor.getAgeCredential().runFlowTest {
                // Then
                val expectedState = GetAgeCredentialPartialState.Success(
                    ageCredentialUi = AgeCredentialUi(
                        docId = mockedAgeVerificationDocument.id,
                        claims = transformPathsToDomainClaims(
                            paths = mockedAgeVerificationDocument.data.claims.flatMap { it.toClaimPaths() },
                            claims = mockedAgeVerificationDocument.data.claims,
                            metadata = mockedAgeVerificationDocument.metadata,
                            resourceProvider = resourceProvider,
                        )
                    )
                )
                assertEquals(expectedState, awaitItem())
            }
        }
    }

    // Case 2:
    // 1. walletCoreDocumentsController.getAllDocuments() returns an empty list.

    // Case 2 Expected Result:
    // LandingPageInteractorGetAgeCredentialPartialState.Failure state with the "no age credential found" message.
    @Test
    fun `Given Case 2, When getAgeCredential is called, Then Case 2 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getAllDocuments())
                .thenReturn(emptyList())

            // When
            interactor.getAgeCredential().runFlowTest {
                // Then
                assertEquals(
                    GetAgeCredentialPartialState.Failure(
                        error = mockedPlainFailureMessage
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 3:
    // 1. walletCoreDocumentsController.getAllDocuments() throws an exception with a message.

    // Case 3 Expected Result:
    // LandingPageInteractorGetAgeCredentialPartialState.Failure state with the exception's localized message.
    @Test
    fun `Given Case 3, When getAgeCredential is called, Then Case 3 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getAgeOver18IssuedDocument())
                .thenThrow(mockedExceptionWithMessage)

            // When
            interactor.getAgeCredential().runFlowTest {
                // Then
                assertEquals(
                    GetAgeCredentialPartialState.Failure(
                        error = mockedExceptionWithMessage.localizedMessage!!
                    ),
                    awaitItem()
                )
            }
        }
    }

    // Case 4:
    // 1. walletCoreDocumentsController.getAllDocuments() throws an exception with no message.

    // Case 4 Expected Result:
    // LandingPageInteractorGetAgeCredentialPartialState.Failure state with the generic error message.
    @Test
    fun `Given Case 4, When getAgeCredential is called, Then Case 4 Expected Result is returned`() {
        coroutineRule.runTest {
            // Given
            whenever(walletCoreDocumentsController.getAgeOver18IssuedDocument())
                .thenThrow(mockedExceptionWithNoMessage)

            // When
            interactor.getAgeCredential().runFlowTest {
                // Then
                assertEquals(
                    GetAgeCredentialPartialState.Failure(
                        error = mockedGenericErrorMessage
                    ),
                    awaitItem()
                )
            }
        }
    }
    //endregion
} 