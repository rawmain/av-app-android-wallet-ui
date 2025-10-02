package eu.europa.ec.passportscanner.face

import android.content.Context
import android.content.Intent
import kl.open.fmandroid.NativeBridge
import org.json.JSONObject
import java.io.File

/**
 * Implementation of AVFaceMatchSDK for passport face verification
 * Integrates with native face matching library for liveness detection and face comparison
 */
class AVFaceMatchSdkImpl(private val context: Context) : AVFaceMatchSDK {

    companion object {
        private const val TAG = "AVFaceMatchSdk"

        init {
            System.loadLibrary("avfacelib") // Load the native library
        }
    }

    override fun init(configJson: String): Boolean {
        android.util.Log.d(TAG, "init: Starting SDK initialization")

        val modelBasePath = context.filesDir.absolutePath
        android.util.Log.d(TAG, "init: Model base path: $modelBasePath")

        val parsedConfig = JSONObject(configJson)

        // Extract model file names from configuration
        val livenessModel0 = parsedConfig.optString("liveness_model0")
        val livenessModel1 = parsedConfig.optString("liveness_model1")
        val faceDetectorModel = parsedConfig.optString("face_detector_model")
        val embeddingModel = parsedConfig.optString("embedding_extractor_model")

        android.util.Log.d(TAG, "init: Models - liveness0: $livenessModel0, liveness1: $livenessModel1, faceDetector: $faceDetectorModel, embedding: $embeddingModel")

        // Copy models from assets to internal storage if needed
        copyAssetIfNeeded(livenessModel0, modelBasePath)
        copyAssetIfNeeded(livenessModel1, modelBasePath)
        copyAssetIfNeeded(faceDetectorModel, modelBasePath)
        copyAssetIfNeeded(embeddingModel, modelBasePath)

        // Set debug save path for development builds
//        NativeBridge.jni_setDebugSavePath(context.cacheDir.absolutePath)

        android.util.Log.d(TAG, "init: Calling native initialization...")
        val result = NativeBridge.safeInit(configJson, modelBasePath)
        android.util.Log.d(TAG, "init: Native initialization result: $result")

        return result
    }

    override fun captureAndMatch(referenceImagePath: String, onResult: (AVMatchResult) -> Unit) {
        android.util.Log.d(TAG, "captureAndMatch: Starting with reference image: $referenceImagePath")

        // Validate reference image path
        if (referenceImagePath.isEmpty() || !File(referenceImagePath).exists()) {
            android.util.Log.e(TAG, "captureAndMatch: Invalid reference image path: $referenceImagePath")
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

        android.util.Log.d(TAG, "captureAndMatch: Processing reference image...")

        try {
            // Process reference image first
            val originalResult = NativeBridge.safeProcess(referenceImagePath, true)
            android.util.Log.d(TAG, "captureAndMatch: Reference processing result - embeddingExtracted: ${originalResult.embeddingExtracted}, faceDetected: ${originalResult.faceDetected}")

            val referenceResult = AVProcessResult(
                livenessChecked = originalResult.livenessChecked,
                isLive = originalResult.isLive,
                faceDetected = originalResult.faceDetected,
                embeddingExtracted = originalResult.embeddingExtracted,
                embedding = originalResult.embedding
            )

            if (!referenceResult.embeddingExtracted) {
                android.util.Log.e(TAG, "captureAndMatch: Failed to extract embedding from reference image")
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

            android.util.Log.d(TAG, "captureAndMatch: Reference image processed successfully, embedding size: ${referenceResult.embedding.size}")

            // Initialize decision maker for multiple samples
            val decisor = AVDecisor(numSamples = 3)
            android.util.Log.d(TAG, "captureAndMatch: Created decisor with ${decisor.getSampleCount()}/${3} samples")

            // Set up callback holder for camera activity
            AVCameraCallbackHolder.referenceResult = referenceResult
            AVCameraCallbackHolder.decisor = decisor
            AVCameraCallbackHolder.onFinalResult = { result ->
                android.util.Log.d(TAG, "captureAndMatch: Final callback triggered with result: $result")
                onResult(result)
            }

            android.util.Log.d(TAG, "captureAndMatch: Callback holder setup complete, ready: ${AVCameraCallbackHolder.isReady()}")

            // Start camera activity for live capture
            android.util.Log.d(TAG, "captureAndMatch: Starting CameraActivity...")
            val intent = Intent(context, kl.open.fmandroid.CameraActivity::class.java)
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            android.util.Log.d(TAG, "captureAndMatch: CameraActivity started")

        } catch (e: Exception) {
            android.util.Log.e(TAG, "captureAndMatch: Exception occurred", e)
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
        android.util.Log.d(TAG, "reset: Resetting SDK state")
        AVCameraCallbackHolder.reset()
        NativeBridge.jni_release()
        android.util.Log.d(TAG, "reset: SDK reset complete")
    }

    /**
     * Copy asset file to internal storage if it doesn't exist
     * @param assetName Name of the asset file
     * @param destDir Destination directory path
     */
    private fun copyAssetIfNeeded(assetName: String, destDir: String) {
        if (assetName.isEmpty()) return

        val destFile = File(destDir, assetName)
        if (!destFile.exists()) {
            try {
                context.assets.open(assetName).use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                // Log error but don't fail initialization
                android.util.Log.e("AVFaceMatchSdk", "Failed to copy asset: $assetName", e)
            }
        }
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
            android.util.Log.e("AVFaceMatchSdk", "Error in testDirectMatch", e)
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
