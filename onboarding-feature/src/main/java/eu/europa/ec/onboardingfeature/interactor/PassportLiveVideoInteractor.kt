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
    suspend fun init(context: Context, onProgress: ((Int, String) -> Unit)? = null): Boolean
    suspend fun captureAndMatchFace(faceImageTempPath: String): FaceMatchPartialState
}

class PassportLiveVideoInteractorImpl(
    private val faceMatchController: FaceMatchController,
    private val resourceProvider: ResourceProvider,
    private val logController: LogController,
) : PassportLiveVideoInteractor {

    private var sdkInitialized = false

    override suspend fun init(context: Context, onProgress: ((Int, String) -> Unit)?): Boolean {
        if (sdkInitialized) {
            logController.d(TAG) { "SDK already initialized" }
            return true
        }

        logController.d(TAG) { "Initializing SDK in interactor..." }

        return try {
            val success = faceMatchController.init(context, onProgress)

            if (success) {
                sdkInitialized = true
                logController.i(TAG) { "SDK initialization complete" }
                true
            } else {
                logController.e(TAG) { "SDK initialization failed" }
                false
            }
        } catch (e: Exception) {
            logController.e(TAG) { "Exception during SDK initialization: ${e.message}" }
            false
        }
    }

    override suspend fun captureAndMatchFace(faceImageTempPath: String): FaceMatchPartialState {
        if (!sdkInitialized) {
            logController.e(TAG) { "SDK not initialized" }
            return FaceMatchPartialState.Failure(
                resourceProvider.getString(R.string.generic_error_retry)
            )
        }

        logController.d(TAG) { "Starting face capture and match using image: $faceImageTempPath" }

        val matchResult: FaceMatchResult = try {
            faceMatchController.captureAndMatch(faceImageTempPath)
        } catch (e: IllegalStateException) {
            logController.e(TAG) { "SDK not initialized when calling captureAndMatch: ${e.message}" }
            return FaceMatchPartialState.Failure(
                resourceProvider.getString(R.string.generic_error_retry)
            )
        }

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
