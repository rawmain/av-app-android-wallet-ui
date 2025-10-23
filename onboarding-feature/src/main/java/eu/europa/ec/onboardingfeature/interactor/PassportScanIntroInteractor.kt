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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "PassportScanIntroInteractor"

interface PassportScanIntroInteractor {
    suspend fun init(context: Context)
}

class PassportScanIntroInteractorImpl(
    private val faceMatchController: FaceMatchController,
    private val logController: LogController,
) : PassportScanIntroInteractor {

    private val interactorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun init(context: Context) {
        logController.d(TAG) { "Starting face match SDK initialization..." }

        // Fire and forget - just trigger initialization
        interactorScope.launch {
            faceMatchController.init(context).collect { status ->
                logController.d(TAG) { "SDK init status: $status" }
            }
        }
    }
}
