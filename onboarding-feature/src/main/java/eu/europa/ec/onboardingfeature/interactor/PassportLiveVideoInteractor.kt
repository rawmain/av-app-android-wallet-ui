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
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.onboardingfeature.controller.FaceMatchController
import eu.europa.ec.onboardingfeature.controller.FaceMatchResult
import eu.europa.ec.resourceslogic.R
import eu.europa.ec.resourceslogic.provider.ResourceProvider

private const val TAG = "PassportLiveVideoInteractor"

sealed class FaceMatchPartialState {
    data object Success : FaceMatchPartialState()
    data class Failure(val error: String) : FaceMatchPartialState()
}

interface PassportLiveVideoInteractor {
    suspend fun captureAndMatchFace(
        context: Context,
        faceImageTempPath: String,
    ): FaceMatchPartialState
}

class PassportLiveVideoInteractorImpl(
    private val faceMatchController: FaceMatchController,
    private val resourceProvider: ResourceProvider,
    private val logController: LogController,
) : PassportLiveVideoInteractor {

    private val genericErrorMsg
        get() = resourceProvider.genericErrorMessage()

    override suspend fun captureAndMatchFace(
        context: Context,
        faceImageTempPath: String,
    ): FaceMatchPartialState {
        logController.d(TAG) { "Starting face capture and match using image: $faceImageTempPath" }

        val matchResult: FaceMatchResult =
            faceMatchController.captureAndMatch(context, faceImageTempPath)

        logController.d(TAG) {
            "Face match result: processed=${matchResult.processed}," +
                    " isLive=${matchResult.capturedIsLive}, isSame=${matchResult.isSameSubject}"
        }

        return if (matchResult.processed && matchResult.capturedIsLive && matchResult.isSameSubject) {
            logController.i(TAG) { "Face match successful" }
            FaceMatchPartialState.Success
        } else {
            val errorMessage = when {
                !matchResult.processed -> resourceProvider.getString(R.string.passport_live_video_error_not_processed)
                !matchResult.capturedIsLive -> resourceProvider.getString(R.string.passport_live_video_error_not_live)
                else -> resourceProvider.getString(R.string.passport_live_video_error_not_matching)
            }
            logController.w(TAG) { "Face match failed: $errorMessage" }
            FaceMatchPartialState.Failure(errorMessage)
        }
    }
}

