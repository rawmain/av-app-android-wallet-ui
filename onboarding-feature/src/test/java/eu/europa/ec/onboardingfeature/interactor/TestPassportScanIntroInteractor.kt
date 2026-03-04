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
import eu.europa.ec.businesslogic.model.ErrorType
import eu.europa.ec.onboardingfeature.controller.FaceMatchController
import eu.europa.ec.passportscanner.face.SdkInitStatus
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TestPassportScanIntroInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var faceMatchController: FaceMatchController

    @Mock
    private lateinit var context: Context

    private lateinit var interactor: PassportScanIntroInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = PassportScanIntroInteractorImpl(
            faceMatchController = faceMatchController
        )
    }

    @After
    fun after() {
        closeable.close()
    }

    //region initFaceMatchSDK

    @Test
    fun `Given SDK initialization, When initFaceMatchSDK is called, Then emits all status updates`() {
        coroutineRule.runTest {
            // Given
            val expectedStatuses = listOf(
                SdkInitStatus.NotInitialized,
                SdkInitStatus.Preparing(50),
                SdkInitStatus.Initializing,
                SdkInitStatus.Ready
            )
            whenever(faceMatchController.init(any())).thenReturn(flowOf(*expectedStatuses.toTypedArray()))

            // When
            val result = interactor.initFaceMatchSDK(context).toList()

            // Then
            verify(faceMatchController).init(context)
            assertEquals(expectedStatuses, result)
        }
    }

    @Test
    fun `Given SDK initialization fails, When initFaceMatchSDK is called, Then emits error status`() {
        coroutineRule.runTest {
            // Given
            val expectedStatuses = listOf(
                SdkInitStatus.NotInitialized,
                SdkInitStatus.Error("Init failed")
            )
            whenever(faceMatchController.init(any())).thenReturn(flowOf(*expectedStatuses.toTypedArray()))

            // When
            val result = interactor.initFaceMatchSDK(context).toList()

            // Then
            verify(faceMatchController).init(context)
            assertEquals(expectedStatuses, result)
        }
    }

    @Test
    fun `Given SDK initialization fails with no connection, When initFaceMatchSDK called, Then emits error status with NO_CONNECTION`() {
        coroutineRule.runTest {
            // Given
            val expectedStatuses = listOf(
                SdkInitStatus.NotInitialized,
                SdkInitStatus.Error(
                    message = "Initialization failed: Unable to resolve host",
                    errorType = ErrorType.NO_CONNECTION
                )
            )
            whenever(faceMatchController.init(any())).thenReturn(flowOf(*expectedStatuses.toTypedArray()))

            // When
            val result = interactor.initFaceMatchSDK(context).toList()

            // Then
            verify(faceMatchController).init(context)
            assertEquals(expectedStatuses, result)
        }
    }

    //endregion
}
