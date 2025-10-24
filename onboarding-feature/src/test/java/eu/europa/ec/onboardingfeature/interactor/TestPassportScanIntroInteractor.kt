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
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.onboardingfeature.controller.FaceMatchController
import eu.europa.ec.passportscanner.face.SdkInitStatus
import eu.europa.ec.testlogic.extension.runTest
import eu.europa.ec.testlogic.rule.CoroutineTestRule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TestPassportScanIntroInteractor {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var faceMatchController: FaceMatchController

    @Mock
    private lateinit var logController: LogController

    @Mock
    private lateinit var context: Context

    private lateinit var interactor: PassportScanIntroInteractor

    private lateinit var closeable: AutoCloseable

    @Before
    fun before() {
        closeable = MockitoAnnotations.openMocks(this)

        interactor = PassportScanIntroInteractorImpl(
            faceMatchController = faceMatchController,
            logController = logController
        )
    }

    @After
    fun after() {
        closeable.close()
    }

    //region init

    @Test
    fun `Given SDK initialization, When init is called, Then triggers controller init and completes successfully`() {
        coroutineRule.runTest {
            // Given
            val initFlow = flowOf(
                SdkInitStatus.NotInitialized,
                SdkInitStatus.Preparing(50),
                SdkInitStatus.Initializing,
                SdkInitStatus.Ready
            )
            whenever(faceMatchController.init(any())).thenReturn(initFlow)

            // When
            interactor.init(context)

            // Then - verify init was called (with timeout since it's fire-and-forget)
            // Give some time for the launched coroutine to start
            delay(100)
            verify(faceMatchController, timeout(1000)).init(context)
        }
    }

    @Test
    fun `Given SDK initialization fails, When init is called, Then still completes without throwing`() {
        coroutineRule.runTest {
            // Given
            val initFlow = flowOf(
                SdkInitStatus.NotInitialized,
                SdkInitStatus.Error("Init failed")
            )
            whenever(faceMatchController.init(any())).thenReturn(initFlow)

            // When - should not throw
            interactor.init(context)

            // Then - verify init was called (with timeout since it's fire-and-forget)
            // Give some time for the launched coroutine to start
            delay(100)
            verify(faceMatchController, timeout(1000)).init(context)
        }
    }

    //endregion
}
