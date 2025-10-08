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

package eu.europa.ec.onboardingfeature.controller

import android.content.Context
import eu.europa.ec.passportscanner.face.AVFaceMatchSDK
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class FaceMatchResult(
    val processed: Boolean,
    val capturedIsLive: Boolean,
    val isSameSubject: Boolean,
)

interface FaceMatchController {
    suspend fun captureAndMatch(context: Context, faceImagePath: String): FaceMatchResult
}

class FaceMatchControllerImpl(
    private val faceMatchSDK: AVFaceMatchSDK,
) : FaceMatchController {

    override suspend fun captureAndMatch(context: Context, faceImagePath: String): FaceMatchResult {
        return suspendCancellableCoroutine { continuation ->
            faceMatchSDK.captureAndMatch(faceImagePath) { result ->
                continuation.resume(
                    FaceMatchResult(
                        processed = result.processed,
                        capturedIsLive = result.capturedIsLive,
                        isSameSubject = result.isSameSubject
                    )
                )
            }
        }
    }
}
