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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import eu.europa.ec.businesslogic.controller.log.LogController
import eu.europa.ec.businesslogic.extension.toErrorType
import eu.europa.ec.passportscanner.worker.ModelDownloadWorker
import kl.open.fmandroid.NativeBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

/**
 * Implementation of AVFaceMatchSDK for passport face verification
 * Integrates with native face matching library for liveness detection and face comparison
 */
class AVFaceMatchSdkImpl(
    private val context: Context,
    private val logController: LogController,
    private val nativeBridge: NativeBridge,
    private val avCameraCallbackHolder: AVCameraCallbackHolder,
) : AVFaceMatchSDK {

    private val sdkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observeJob: Job? = null
    @Volatile
    private var sdkInitialized = false

    @Volatile
    private var pendingConfig: FaceMatchConfig? = null

    private val _initStatus = MutableStateFlow<SdkInitStatus>(SdkInitStatus.NotInitialized)
    override val initStatusFlow: StateFlow<SdkInitStatus> = _initStatus.asStateFlow()

    companion object {
        private const val TAG = "AVFaceMatchSdk"

        init {
            System.loadLibrary("avfacelib") // Load the native library
        }
    }

    override fun init(
        config: FaceMatchConfig,
        context: Context,
    ): Flow<SdkInitStatus> {
        // Check if already initialized
        if (_initStatus.value is SdkInitStatus.Ready) {
            logController.d(TAG) { "init: SDK already initialized, returning existing flow" }
            return initStatusFlow
        }

        // Check if initialization is actively running
        if (observeJob?.isActive == true) {
            logController.d(TAG) { "init: Initialization already in progress, returning existing flow" }
            return initStatusFlow
        }

        logController.d(TAG) { "init: Starting SDK initialization via WorkManager" }
        _initStatus.value = SdkInitStatus.NotInitialized
        pendingConfig = config

        val embeddingSource = config.embeddingExtractorModel as? FaceMatchModelSource.Remote
        if (embeddingSource == null) {
            _initStatus.value = SdkInitStatus.Error("Embedding model must be a Remote source")
            return initStatusFlow
        }

        // Optionally: request POST_NOTIFICATIONS runtime permission here (required on API 33+)
        //  for the foreground service notification to be visible during download.

        val inputData = workDataOf(
            ModelDownloadWorker.KEY_INPUT_EMBEDDING_URL to embeddingSource.url,
            ModelDownloadWorker.KEY_INPUT_EMBEDDING_SHA256 to embeddingSource.sha256Hex,
        )

        val workRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(inputData)
            .build()

        val workManager = WorkManager.getInstance(context.applicationContext)
        _initStatus.value = SdkInitStatus.Preparing(0)

        workManager.enqueueUniqueWork(
            ModelDownloadWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            workRequest,
        )

        observeJob = sdkScope.launch {
            workManager.getWorkInfoByIdFlow(workRequest.id)
                .collect { workInfo ->
                    if (workInfo == null) return@collect
                    handleWorkInfo(workInfo, context)
                }
        }

        return initStatusFlow
    }

    private fun handleWorkInfo(workInfo: WorkInfo, context: Context) {
        when (workInfo.state) {
            WorkInfo.State.RUNNING -> {
                val progress = workInfo.progress.getInt(ModelDownloadWorker.KEY_PROGRESS, 0)
                _initStatus.value = SdkInitStatus.Preparing(progress)
            }

            WorkInfo.State.SUCCEEDED -> {
                logController.d(TAG) { "init: Worker succeeded, performing native init" }
                _initStatus.value = SdkInitStatus.Initializing
                performNativeInit(workInfo, context)
            }

            WorkInfo.State.FAILED -> {
                val error = workInfo.outputData.getString(ModelDownloadWorker.KEY_ERROR)
                    ?: "Model download failed"
                logController.e(TAG) { "init: Worker failed: $error" }
                _initStatus.value = SdkInitStatus.Error(error)
            }

            WorkInfo.State.CANCELLED -> {
                logController.d(TAG) { "init: Worker cancelled" }
                _initStatus.value = SdkInitStatus.NotInitialized
            }

            WorkInfo.State.ENQUEUED,
            WorkInfo.State.BLOCKED -> {
                // Waiting to run — keep current state
            }
        }
    }

    private fun performNativeInit(workInfo: WorkInfo, context: Context) {
        val embeddingFilename = workInfo.outputData.getString(ModelDownloadWorker.KEY_EMBEDDING)
        if (embeddingFilename == null) {
            _initStatus.value = SdkInitStatus.Error("Missing embedding filename from worker output")
            return
        }

        val config = pendingConfig ?: run {
            _initStatus.value = SdkInitStatus.Error("Configuration lost during initialization")
            return
        }

        try {
            val modelBasePath = context.applicationContext.filesDir.absolutePath

            val liveness0Filename = extractAssetModel(config.livenessModel0, modelBasePath)
            val liveness1Filename = extractAssetModel(config.livenessModel1, modelBasePath)
            val faceDetectorFilename = extractAssetModel(config.faceDetectorModel, modelBasePath)

            if (liveness0Filename == null || liveness1Filename == null || faceDetectorFilename == null) {
                _initStatus.value = SdkInitStatus.Error("Failed to extract asset models")
                return
            }

            val configJson = JSONObject().apply {
                put("face_detector_model", faceDetectorFilename)
                put("liveness_model0", liveness0Filename)
                put("liveness_model1", liveness1Filename)
                put("liveness_threshold", config.livenessThreshold)
                put("matching_threshold", config.matchingThreshold)
                put("embedding_extractor_model", embeddingFilename)
            }

            logController.d(TAG) { "init: Calling native initialization..." }
            val result = nativeBridge.safeInit(configJson.toString(), modelBasePath)
            logController.d(TAG) { "init: Native initialization result: $result" }

            if (result) {
                sdkInitialized = true
                _initStatus.value = SdkInitStatus.Ready
                logController.i(TAG) { "init: SDK initialization completed successfully" }
            } else {
                _initStatus.value = SdkInitStatus.Error(
                    "Native SDK initialization failed. Check that all model files are valid and compatible."
                )
            }
        } catch (e: Exception) {
            logController.e(TAG, e) { "init: Exception during native initialization" }
            _initStatus.value = SdkInitStatus.Error(
                "Initialization failed: ${e.message}",
                e.toErrorType()
            )
        }
    }

    private fun extractAssetModel(source: FaceMatchModelSource, destDir: String): String? {
        val assetSource = source as? FaceMatchModelSource.Asset ?: return null
        if (assetSource.filename.isEmpty()) return null

        val destFile = File(destDir, assetSource.filename)
        if (!destFile.exists()) {
            try {
                context.assets.open(assetSource.filename).use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                logController.e(TAG, e) { "extractAssetModel: Failed to copy ${assetSource.filename}" }
                return null
            }
        }
        return assetSource.filename
    }

    override fun captureAndMatch(referenceImageBytes: ByteArray, onResult: (AVMatchResult) -> Unit) {
        logController.d(TAG) { "captureAndMatch: Starting with ${referenceImageBytes.size} bytes" }

        if (referenceImageBytes.isEmpty()) {
            logController.e(TAG) { "captureAndMatch: Empty reference image bytes" }
            onResult(
                AVMatchResult(
                    processed = true,
                    referenceIsValid = false,
                    capturedIsLive = false,
                    isSameSubject = false,
                )
            )
            return
        }

        logController.d(TAG) { "captureAndMatch: Processing reference image..." }

        try {
            val originalResult = nativeBridge.safeProcessEncode(referenceImageBytes, true)
            logController.d(TAG) { "captureAndMatch: embeddingExtracted=${originalResult.embeddingExtracted}, faceDetected=${originalResult.faceDetected}" }

            val referenceResult = AVProcessResult(
                livenessChecked = originalResult.livenessChecked,
                isLive = originalResult.isLive,
                faceDetected = originalResult.faceDetected,
                embeddingExtracted = originalResult.embeddingExtracted,
                embedding = originalResult.embedding
            )

            if (!referenceResult.embeddingExtracted) {
                logController.e(TAG) { "captureAndMatch: Failed to extract embedding from reference image" }
                // Fail fast if reference image is invalid
                onResult(
                    AVMatchResult(
                        processed = true,
                        referenceIsValid = false,
                        capturedIsLive = false,
                        isSameSubject = false,
                    )
                )
                return
            }

            logController.d(TAG) { "captureAndMatch: Reference image processed successfully, embedding size: ${referenceResult.embedding.size}" }

            val decisor = AVDecisor(numSamples = 3, logController)
            logController.d(TAG) { "captureAndMatch: Created decisor with ${decisor.getSampleCount()}/${3} samples" }

            // Set up callback holder for camera activity
            avCameraCallbackHolder.referenceResult = referenceResult
            avCameraCallbackHolder.decisor = decisor
            avCameraCallbackHolder.onFinalResult = { result ->
                logController.d(TAG) { "captureAndMatch: Final callback triggered with result: $result" }
                onResult(result)
            }

            logController.d(TAG) { "captureAndMatch: Callback holder setup complete, ready: ${avCameraCallbackHolder.isReady()}" }

            // Start camera activity for live capture
            logController.d(TAG) { "captureAndMatch: Starting CameraActivity..." }
            val intent = Intent(context, kl.open.fmandroid.CameraActivity::class.java)
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            logController.d(TAG) { "captureAndMatch: CameraActivity started" }

        } catch (e: Exception) {
            logController.e(TAG, e) { "captureAndMatch: Exception occurred" }
            onResult(
                AVMatchResult(
                    processed = false,
                    referenceIsValid = false,
                    capturedIsLive = false,
                    isSameSubject = false,
                )
            )
        }
    }

    override fun cancelInit() {
        logController.d(TAG) { "cancelInit: Cancelling SDK initialization" }
        observeJob?.cancel()
        observeJob = null
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(ModelDownloadWorker.WORK_NAME)
        _initStatus.value = SdkInitStatus.NotInitialized
        logController.d(TAG) { "cancelInit: SDK initialization cancelled" }
    }

    override fun reset() {
        logController.d(TAG) { "reset: Resetting SDK state" }
        avCameraCallbackHolder.reset()
        nativeBridge.jni_release()
        sdkInitialized = false
        _initStatus.value = SdkInitStatus.NotInitialized
        logController.d(TAG) { "reset: SDK reset complete" }
    }

}
