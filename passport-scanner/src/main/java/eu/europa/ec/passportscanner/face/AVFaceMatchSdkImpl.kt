/*
 * MIT License
 *
 * Copyright (c) 2025 Keyless Tech
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package eu.europa.ec.passportscanner.face

import android.content.Context
import android.content.Intent
import android.util.Log
import kl.open.fmandroid.NativeBridge
import org.json.JSONObject
import java.io.File

/**
 * Implementation of AVFaceMatchSDK for passport face verification
 * Integrates with native face matching library for liveness detection and face comparison
 */
class AVFaceMatchSdkImpl(private val context: Context) : AVFaceMatchSDK {

    private val modelDownloader = ModelDownloader(context)

    companion object {
        private const val TAG = "AVFaceMatchSdk"

        init {
            System.loadLibrary("avfacelib") // Load the native library
        }
    }

    override suspend fun init(configJson: String, onProgress: ((Int, String) -> Unit)?): Boolean {
        Log.d(TAG, "init: Starting SDK initialization")

        val modelBasePath = context.filesDir.absolutePath
        Log.d(TAG, "init: Model base path: $modelBasePath")

        val parsedConfig = JSONObject(configJson)

        // Extract model file names/URLs from configuration
        val livenessModel0 = parsedConfig.optString("liveness_model0")
        val livenessModel1 = parsedConfig.optString("liveness_model1")
        val faceDetectorModel = parsedConfig.optString("face_detector_model")
        val embeddingModel = parsedConfig.optString("embedding_extractor_model")

        Log.d(TAG, "init: Models - liveness0: $livenessModel0, liveness1: $livenessModel1, faceDetector: $faceDetectorModel, embedding: $embeddingModel")
        val embeddingOutputFilename = "embedding.onnx"

        // Prepare models (download from URL or copy from assets) to internal storage
        onProgress?.invoke(0, "Preparing models...")
        modelDownloader.prepareModel(livenessModel0, modelBasePath)
        modelDownloader.prepareModel(livenessModel1, modelBasePath)
        modelDownloader.prepareModel(faceDetectorModel, modelBasePath)
        modelDownloader.prepareModel(embeddingModel, modelBasePath, embeddingOutputFilename, onProgress)

        // Update config to use local filename for embedding model (since it's downloaded from URL)
        if (embeddingModel.startsWith("http")|| embeddingModel.startsWith("https")) {
            parsedConfig.put("embedding_extractor_model", embeddingOutputFilename)
            Log.d(TAG, "init: Updated embedding model path to $embeddingOutputFilename")
        }

        // Set debug save path for development builds
//        NativeBridge.jni_setDebugSavePath(context.cacheDir.absolutePath)

        Log.d(TAG, "init: Calling native initialization...")
        val result = NativeBridge.safeInit(parsedConfig.toString(), modelBasePath)
        Log.d(TAG, "init: Native initialization result: $result")

        return result
    }

    override fun captureAndMatch(referenceImagePath: String, onResult: (AVMatchResult) -> Unit) {
        Log.d(TAG, "captureAndMatch: Starting with reference image: $referenceImagePath")

        // Validate reference image path
        if (referenceImagePath.isEmpty() || !File(referenceImagePath).exists()) {
            Log.e(TAG, "captureAndMatch: Invalid reference image path: $referenceImagePath")
            onResult(
                AVMatchResult(
                    processed = true,
                    referenceIsValid = false,
                    capturedIsLive = false,
                    isSameSubject = false,
                    capturedPath = null
                )
            )
            return
        }

        Log.d(TAG, "captureAndMatch: Processing reference image...")

        try {
            // Process reference image first
            val originalResult = NativeBridge.safeProcess(referenceImagePath, true)
            Log.d(TAG, "captureAndMatch: Reference processing result - embeddingExtracted: ${originalResult.embeddingExtracted}, faceDetected: ${originalResult.faceDetected}")

            val referenceResult = AVProcessResult(
                livenessChecked = originalResult.livenessChecked,
                isLive = originalResult.isLive,
                faceDetected = originalResult.faceDetected,
                embeddingExtracted = originalResult.embeddingExtracted,
                embedding = originalResult.embedding
            )

            if (!referenceResult.embeddingExtracted) {
                Log.e(TAG, "captureAndMatch: Failed to extract embedding from reference image")
                // Fail fast if reference image is invalid
                onResult(
                    AVMatchResult(
                        processed = true,
                        referenceIsValid = false,
                        capturedIsLive = false,
                        isSameSubject = false,
                        capturedPath = null
                    )
                )
                return
            }

            Log.d(TAG, "captureAndMatch: Reference image processed successfully, embedding size: ${referenceResult.embedding.size}")

            // Initialize decision maker for multiple samples
            val decisor = AVDecisor(numSamples = 3)
            Log.d(TAG, "captureAndMatch: Created decisor with ${decisor.getSampleCount()}/${3} samples")

            // Set up callback holder for camera activity
            AVCameraCallbackHolder.referenceResult = referenceResult
            AVCameraCallbackHolder.decisor = decisor
            AVCameraCallbackHolder.onFinalResult = { result ->
                Log.d(TAG, "captureAndMatch: Final callback triggered with result: $result")
                onResult(result)
            }

            Log.d(TAG, "captureAndMatch: Callback holder setup complete, ready: ${AVCameraCallbackHolder.isReady()}")

            // Start camera activity for live capture
            Log.d(TAG, "captureAndMatch: Starting CameraActivity...")
            val intent = Intent(context, kl.open.fmandroid.CameraActivity::class.java)
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Log.d(TAG, "captureAndMatch: CameraActivity started")

        } catch (e: Exception) {
            Log.e(TAG, "captureAndMatch: Exception occurred", e)
            onResult(
                AVMatchResult(
                    processed = false,
                    referenceIsValid = false,
                    capturedIsLive = false,
                    isSameSubject = false,
                    capturedPath = null
                )
            )
        }
    }

    override fun reset() {
        Log.d(TAG, "reset: Resetting SDK state")
        AVCameraCallbackHolder.reset()
        NativeBridge.jni_release()
        Log.d(TAG, "reset: SDK reset complete")
    }

    /**
     * Get SDK version information
     * @return Version string from native library
     */
    fun getVersion(): String {
        return try {
            NativeBridge.jni_getVersion()
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Match two face embeddings directly
     * @param embedding1 First face embedding
     * @param embedding2 Second face embedding
     * @return true if embeddings match (same subject)
     */
    fun matchEmbeddings(embedding1: FloatArray, embedding2: FloatArray): Boolean {
        return NativeBridge.safeMatch(embedding1, embedding2)
    }

    /**
     * Test method to process two images directly without camera
     * @param referenceImagePath Path to reference image (from passport)
     * @param capturedImagePath Path to captured image (selfie)
     * @return AVMatchResult with comparison results
     */
    fun testDirectMatch(referenceImagePath: String, capturedImagePath: String): AVMatchResult {
        // Validate paths
        if (referenceImagePath.isEmpty() || !File(referenceImagePath).exists()) {
            return AVMatchResult(
                processed = true,
                referenceIsValid = false,
                capturedIsLive = false,
                isSameSubject = false,
                capturedPath = null
            )
        }

        if (capturedImagePath.isEmpty() || !File(capturedImagePath).exists()) {
            return AVMatchResult(
                processed = true,
                referenceIsValid = true,
                capturedIsLive = false,
                isSameSubject = false,
                capturedPath = null
            )
        }

        try {
            // Process reference image
            val referenceResult = NativeBridge.jni_process(referenceImagePath, true)
            if (!referenceResult.embeddingExtracted) {
                return AVMatchResult(
                    processed = true,
                    referenceIsValid = false,
                    capturedIsLive = false,
                    isSameSubject = false,
                    capturedPath = null
                )
            }

            // Process captured image
            val capturedResult = NativeBridge.jni_process(capturedImagePath, false)
            if (!capturedResult.embeddingExtracted) {
                return AVMatchResult(
                    processed = true,
                    referenceIsValid = true,
                    capturedIsLive = false,
                    isSameSubject = false,
                    capturedPath = capturedImagePath
                )
            }

            // Match embeddings
            val isSameSubject = matchEmbeddings(referenceResult.embedding, capturedResult.embedding)

            return AVMatchResult(
                processed = true,
                referenceIsValid = true,
                capturedIsLive = capturedResult.isLive,
                isSameSubject = isSameSubject,
                capturedPath = capturedImagePath
            )

        } catch (e: Exception) {
            Log.e("AVFaceMatchSdk", "Error in testDirectMatch", e)
            return AVMatchResult(
                processed = false,
                referenceIsValid = false,
                capturedIsLive = false,
                isSameSubject = false,
                capturedPath = null
            )
        }
    }
}
