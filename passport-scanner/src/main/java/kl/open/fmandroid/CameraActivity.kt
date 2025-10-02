package kl.open.fmandroid

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Size
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import eu.europa.ec.passportscanner.face.AVCameraCallbackHolder
import eu.europa.ec.passportscanner.face.AVMatchResult
import java.io.File

class CameraActivity : AppCompatActivity() {

    private lateinit var imageCapture: ImageCapture
    private lateinit var previewView: PreviewView

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            AVCameraCallbackHolder.triggerCallback(MatchResult.error())
            finish()
        }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val planeProxy = imageProxy.planes[0]
        val buffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("CameraActivity", "onCreate: Starting camera activity")

        previewView = PreviewView(this)
        previewView.scaleX = -1f // Mirror the preview
        setContentView(previewView)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        android.util.Log.d("CameraActivity", "startCamera: Initializing camera")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1280, 720),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                    )
                )
                .build()

            imageCapture = ImageCapture.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            android.util.Log.d("CameraActivity", "startCamera: Camera bound, starting capture")
            // Start the first frame capture
            captureFrame()

        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureFrame() {
        android.util.Log.d("CameraActivity", "captureFrame: Capturing frame")
        val finalFile = File(getExternalFilesDir(null), "captured_frame_${System.currentTimeMillis()}.png")

        val imageCaptureCallback = object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                android.util.Log.d("CameraActivity", "captureFrame: Image captured successfully")
                // Convert raw ImageProxy to Bitmap
                val bitmap = imageProxyToBitmap(imageProxy)
                // Read the correct rotation from the ImageProxy metadata
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees.toFloat()
                // Close the ImageProxy early to free camera resources
                imageProxy.close()

                // If we got a valid Bitmap, rotate and save as PNG
                if (bitmap != null) {
                    // Rotate the bitmap to upright orientation
                    val matrix = Matrix().apply { if (rotationDegrees != 0f) postRotate(rotationDegrees) }
                    val rotatedBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                    )

                    // Save as lossless PNG
                    finalFile.outputStream().use { out ->
                        rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    android.util.Log.d("CameraActivity", "captureFrame: Image saved to ${finalFile.absolutePath}")
                    // Pass the saved file path into the processing pipeline
                    processCapturedImage(finalFile.absolutePath)
                } else {
                    android.util.Log.e("CameraActivity", "captureFrame: Failed to convert imageProxy to bitmap")
                    // On error, report a failure result and clean up
                    AVCameraCallbackHolder.triggerCallback(MatchResult.error())
                    finish()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                android.util.Log.e("CameraActivity", "captureFrame: Image capture failed", exception)
                AVCameraCallbackHolder.triggerCallback(MatchResult.error())
                finish()
            }
        }

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            imageCaptureCallback
        )
    }

    fun processCapturedImage(imagePath: String) {
        android.util.Log.d("CameraActivity", "processCapturedImage: Processing $imagePath")
        try {
            val capturedResult = NativeBridge.safeProcess(imagePath, false)
            android.util.Log.d("CameraActivity", "processCapturedImage: Face detected: ${capturedResult.faceDetected}, Live: ${capturedResult.isLive}")

            if (capturedResult.faceDetected && capturedResult.embeddingExtracted) {
                val referenceEmbedding = AVCameraCallbackHolder.referenceResult?.embedding
                if (referenceEmbedding != null) {
                    val isSameSubject = NativeBridge.safeMatch(referenceEmbedding, capturedResult.embedding)

                    android.util.Log.d("CameraActivity", "processCapturedImage: Same subject: $isSameSubject")

                    AVCameraCallbackHolder.decisor?.addResult(isSameSubject)

                    if (AVCameraCallbackHolder.decisor?.hasEnoughSamples() == true) {
                        val finalDecision = AVCameraCallbackHolder.decisor?.getFinalDecision() ?: false
                        android.util.Log.d("CameraActivity", "processCapturedImage: Final decision: $finalDecision")
                        finishCapture(imagePath, capturedResult.isLive, finalDecision)
                    } else {
                        android.util.Log.d("CameraActivity", "processCapturedImage: Need more samples, capturing next frame")
                        captureFrame()
                    }
                } else {
                    android.util.Log.e("CameraActivity", "processCapturedImage: No reference embedding available")
                    AVCameraCallbackHolder.triggerCallback(MatchResult.error())
                    finish()
                }
            } else {
                android.util.Log.d("CameraActivity", "processCapturedImage: No face detected, retrying")
                // No face detected, just retry another capture
                captureFrame()
            }
        } catch (e: Exception) {
            android.util.Log.e("CameraActivity", "processCapturedImage: Exception during processing", e)
            AVCameraCallbackHolder.triggerCallback(MatchResult.error())
            finish()
        }
    }

    private fun finishCapture(imagePath: String, isLive: Boolean, isSameSubject: Boolean) {
        android.util.Log.d("CameraActivity", "finishCapture: Creating final result")
        val finalResult = AVMatchResult(
            processed = true,
            referenceIsValid = true,
            capturedIsLive = isLive,
            isSameSubject = isSameSubject,
            capturedPath = imagePath
        )

        AVCameraCallbackHolder.triggerCallback(finalResult)
        finish()
    }
}

/**
 * Helper object for creating error results
 */
object MatchResult {
    fun error(): AVMatchResult {
        return AVMatchResult(
            processed = false,
            referenceIsValid = false,
            capturedIsLive = false,
            isSameSubject = false,
            capturedPath = null
        )
    }
}