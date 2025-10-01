/*
 * Copyright (C) 2020 Newlogic Pte. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package eu.europa.ec.passportscanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.view.View.GONE
import android.view.View.OnClickListener
import android.view.View.VISIBLE
import android.view.ViewTreeObserver
import android.view.Window
import android.widget.TextView
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.Guideline
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import eu.europa.ec.passportscanner.api.ScannerConstants
import eu.europa.ec.passportscanner.nfc.NFCScanAnalyzer
import eu.europa.ec.passportscanner.scanner.BaseActivity
import eu.europa.ec.passportscanner.scanner.ImageResult
import eu.europa.ec.passportscanner.scanner.SmartScannerException
import eu.europa.ec.passportscanner.scanner.config.CaptureOptions
import eu.europa.ec.passportscanner.scanner.config.CaptureType
import eu.europa.ec.passportscanner.scanner.config.Config
import eu.europa.ec.passportscanner.scanner.config.ImageResultType
import eu.europa.ec.passportscanner.scanner.config.Orientation
import eu.europa.ec.passportscanner.scanner.config.ScannerOptions
import eu.europa.ec.passportscanner.scanner.config.ScannerSize
import eu.europa.ec.passportscanner.utils.CameraUtils.isLedFlashAvailable
import eu.europa.ec.passportscanner.utils.extension.cacheImagePath
import eu.europa.ec.passportscanner.utils.extension.cacheImageToLocal
import eu.europa.ec.passportscanner.utils.extension.empty
import eu.europa.ec.passportscanner.utils.extension.encodeBase64
import eu.europa.ec.passportscanner.utils.extension.px
import eu.europa.ec.passportscanner.utils.extension.toPx
import eu.europa.ec.passportscanner.utils.transform.CropTransformation
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt


class SmartScannerActivity : BaseActivity(), OnClickListener {

    companion object {
        val TAG: String = SmartScannerActivity::class.java.simpleName
        const val SCANNER_OPTIONS = "scanner_options"
        const val SCANNER_RESULT = "scanner_result"
        const val SCANNER_INTENT_EXTRAS = "scanner_intent_extras"
        const val SCANNER_IMAGE_TYPE = "scanner_image_type"
        const val SCANNER_SETTINGS_CALL = "scanner_settings"
    }

    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUEST_CODE_PERMISSIONS_VERSION_R = 2296
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA
    )
    private var config: Config? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var scannerOptions: ScannerOptions? = null
    private var captureOptions: CaptureOptions? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var orientation: String? = null

    private var flashButton: View? = null
    private var settingsButton: View? = null
    private var closeButton: View? = null
    private var rectangle: View? = null
    private var rectangleGuide: View? = null
    private var guideContainer: View? = null
    private var guideWidth: View? = null
    private var xGuideView: View? = null
    private var yGuideView: View? = null
    private var manualCapture: View? = null
    private var captureLabelText: TextView? = null
    private var captureHeaderText: TextView? = null
    private var captureSubHeaderText: TextView? = null

    private lateinit var modelLayoutView: View
    private lateinit var coordinatorLayoutView: View
    private lateinit var viewFinder: PreviewView
    private lateinit var cameraExecutor: ExecutorService

    private val orientationEventListener by lazy {
        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return
                }

                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                imageAnalyzer?.targetRotation = rotation
                imageCapture?.targetRotation = rotation
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_smart_scanner)

        // assign view ids
        coordinatorLayoutView = findViewById(R.id.coordinator_layout)
        modelLayoutView = findViewById(R.id.view_layout)
        viewFinder = findViewById(R.id.view_finder)
        flashButton = findViewById(R.id.flash_button)
        settingsButton = findViewById(R.id.settings_button)
        closeButton = findViewById(R.id.close_button)
        rectangle = findViewById(R.id.rect_image)
        rectangleGuide = findViewById(R.id.scanner_overlay)
        guideContainer = findViewById(R.id.guide_layout)
        guideWidth = findViewById(R.id.guide_width)
        xGuideView = findViewById(R.id.x_guide)
        yGuideView = findViewById(R.id.y_guide)
        manualCapture = findViewById(R.id.manual_capture)
        captureLabelText = findViewById(R.id.capture_label_text)
        captureHeaderText = findViewById(R.id.capture_header_text)
        captureSubHeaderText = findViewById(R.id.capture_sub_header_text)

        // Scanner setup from intent
        hideActionBar()
        if (intent.action != null) {
            scannerOptions = ScannerOptions.defaultForODK()
        } else {
            // Use scanner options directly if no scanner type is called
            val options: ScannerOptions? = intent.getParcelableExtra(SCANNER_OPTIONS)
            options?.let {
                Log.d(TAG, "scannerOptions: $it")
                scannerOptions = options
            } ?: run {
                throw SmartScannerException("Please set proper scanner options to be able to use ID PASS Smart Scanner.")
            }
        }
        config = scannerOptions?.config ?: Config.default
        // Set orientation to PORTRAIT as default
        orientation = config?.orientation ?: Orientation.PORTRAIT.value
        // Request camera permissions
        if (allPermissionsGranted()) {
            setupConfiguration()
        } else {
            requestPermissions()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupConfiguration() {
        runOnUiThread {
            checkGuideView()

            val nfcOptions = scannerOptions?.nfcOptions
            val analyzer = NFCScanAnalyzer(
                activity = this,
                intent = intent,
                imageResultType = config?.imageResultType ?: ImageResultType.PATH.value,
                label = nfcOptions?.label,
                locale = nfcOptions?.locale
                    ?: intent.getStringExtra(ScannerConstants.NFC_LOCALE),
                isShowGuide = config?.showGuide
            )
            viewFinder.visibility = VISIBLE
            // Set Analyzer and start camera
            startCamera(analyzer)
        }
        setupViews()
    }

    override fun onStart() {
        super.onStart()
        orientationEventListener.enable()
    }

    override fun onStop() {
        super.onStop()
        orientationEventListener.disable()
    }

    @SuppressLint("ClickableViewAccessibility", "UnsafeOptInUsageError")
    private fun startCamera(analyzer: ImageAnalysis.Analyzer? = null) {
        viewFinder.post {
            if (viewFinder.display == null) return@post
            this.getSystemService(CAMERA_SERVICE) as CameraManager
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener({
                val resolution = Size(640, 480)
                val rotation = viewFinder.display.rotation
                // Used to bind the lifecycle of cameras to the lifecycle owner
                cameraProvider = cameraProviderFuture.get()
                // Preview
                preview = Preview.Builder()
                    .setTargetResolution(resolution)
                    .setTargetRotation(rotation)
                    .build()
                val imageAnalysisBuilder = ImageAnalysis.Builder()

                imageAnalyzer = imageAnalysisBuilder
                    .setTargetResolution(resolution)
                    .setTargetRotation(rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        analyzer?.let { analysis -> it.setAnalyzer(cameraExecutor, analysis) }
                    }

                // Create configuration object for the image capture use case
                imageCapture = ImageCapture.Builder()
                    .setTargetResolution(Size(1080, 1920))
                    .setTargetRotation(Surface.ROTATION_0)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()
                // Select back camera
                val cameraSelector =
                    CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()
                try {
                    // Unbind use cases before rebinding
                    cameraProvider?.unbindAll()
                    // Bind use cases to camera
                    camera = if (analyzer != null) {
                        cameraProvider?.bindToLifecycle(
                            this,
                            cameraSelector,
                            preview,
                            imageAnalyzer,
                            imageCapture
                        )
                    } else {
                        cameraProvider?.bindToLifecycle(
                            this,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    }
                    // Adjust initial zoom ratio of camera to aid high resolution capture of Pdf417 or QR Code or ID PASS Lite
                    preview?.surfaceProvider = viewFinder.surfaceProvider
                    Log.d(
                        TAG,
                        "Measured size: ${viewFinder.width}x${viewFinder.height}"
                    )
                    // Autofocus modes and Tap to focus
                    val camera2InterOp = Camera2Interop.Extender(imageAnalysisBuilder)
                    camera2InterOp.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_AUTO
                    )
                    camera2InterOp.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON
                    )
                    viewFinder.afterMeasured {
                        viewFinder.setOnTouchListener { _, event ->
                            return@setOnTouchListener when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    true
                                }

                                MotionEvent.ACTION_UP -> {
                                    val factory: MeteringPointFactory =
                                        SurfaceOrientedMeteringPointFactory(
                                            viewFinder.width.toFloat(), viewFinder.height.toFloat()
                                        )
                                    val autoFocusPoint = factory.createPoint(event.x, event.y)
                                    try {
                                        camera?.cameraControl?.startFocusAndMetering(
                                            FocusMeteringAction.Builder(
                                                autoFocusPoint,
                                                FocusMeteringAction.FLAG_AF
                                            ).apply {
                                                //focus only when the user tap the preview
                                                disableAutoCancel()
                                            }.build()
                                        )
                                    } catch (e: CameraInfoUnavailableException) {
                                        Log.d("ERROR", "cannot access camera", e)
                                    }
                                    true
                                }

                                else -> false // Unhandled event.
                            }
                        }
                    }

                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(this))
        }
    }

    private fun setupViews() {
        // scanner layout size
        val topGuideline = findViewById<Guideline>(R.id.top)
        val bottomGuideline = findViewById<Guideline>(R.id.bottom)
        // scanner sizes available for Portrait only
        if (orientation == Orientation.PORTRAIT.value) {
            when (scannerOptions?.scannerSize) {
                ScannerSize.LARGE.value -> {
                    bottomGuideline.setGuidelinePercent(0.7F)
                    topGuideline.setGuidelinePercent(0.25F)
                }

                ScannerSize.SMALL.value -> {
                    bottomGuideline.setGuidelinePercent(0.6F)
                    topGuideline.setGuidelinePercent(0.375F)
                }

                else -> {
                    bottomGuideline.setGuidelinePercent(0.625F)
                    topGuideline.setGuidelinePercent(0.275F)
                }
            }
        }
        //settings
        settingsButton?.visibility = if (config?.showSettings == true) VISIBLE else GONE
        // flash
        flashButton?.visibility = if (isLedFlashAvailable(this)) VISIBLE else GONE
        // capture text label
        captureLabelText?.text = config?.label ?: String.empty()
        // capture text header
        captureHeaderText?.text = config?.header ?: String.empty()
        // capture text sub-header
        captureSubHeaderText?.text = config?.subHeader ?: String.empty()
        // Background reader
        try {
            config?.background?.let {
                if (it.isNotEmpty()) {
                    val color = Color.parseColor(config?.background)
                    coordinatorLayoutView.setBackgroundColor(color)
                }
            } ?: run {
                coordinatorLayoutView.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.transparent_grey
                    )
                )
            }
        } catch (iae: IllegalArgumentException) {
            // This color string is not valid
            throw SmartScannerException("Please set proper color string in setting background. Example: '#ffc234' ")
        }
        // manual capture
        manualCapture?.visibility = config?.isManualCapture?.let {
            if (it) VISIBLE else GONE
        } ?: run {
            if (intent.getBooleanExtra(
                    ScannerConstants.MRZ_MANUAL_CAPTURE_EXTRA,
                    false
                )
            ) VISIBLE else GONE
        }
        // Device orientation
        if (orientation == Orientation.LANDSCAPE.value) {
            this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        }
        // assign camera click listeners
        closeButton?.setOnClickListener(this)
        settingsButton?.setOnClickListener(this)
        flashButton?.setOnClickListener(this)
        manualCapture?.setOnClickListener(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_PERMISSIONS_VERSION_R,
            REQUEST_CODE_PERMISSIONS -> {
                if (allPermissionsGranted()) {
                    setupConfiguration()
                } else {
                    val snackBar: Snackbar = Snackbar.make(
                        coordinatorLayoutView,
                        R.string.required_perms_not_given,
                        Snackbar.LENGTH_INDEFINITE
                    )
                    snackBar.setAction(R.string.settings) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }
                    snackBar.show()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun requestPermissions() =
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.close_button -> onBackPressed()
            R.id.settings_button -> showSettings()
            R.id.flash_button -> {
                flashButton?.let {
                    if (it.isSelected) {
                        it.isSelected = false
                        enableFlashlight(false)
                    } else {
                        it.isSelected = true
                        enableFlashlight(true)
                    }
                }
            }

            R.id.manual_capture -> {
                // hide capture button during image capture
                manualCapture?.isEnabled = false
                val imageFile = File(cacheImagePath())
                val outputFileOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()
                imageCapture?.takePicture(
                    outputFileOptions, cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            val data = Intent()
                            val width: Int = captureOptions?.width?.px ?: run {
                                when (captureOptions?.type) {
                                    CaptureType.ID.value -> 285.px
                                    CaptureType.DOCUMENT.value -> 180.px
                                    else -> 285.px // default width for MRZ
                                }
                            }
                            val height: Int = captureOptions?.height?.px ?: run {
                                when (captureOptions?.type) {
                                    CaptureType.ID.value -> 180.px
                                    CaptureType.DOCUMENT.value -> 285.px
                                    else -> 180.px // default height for MRZ
                                }
                            }
                            // Initial MRZ Card Size
                            val transform = bitmapTransform(
                                CropTransformation(
                                    width,
                                    height,
                                    CropTransformation.CropType.CENTER
                                )
                            )
                            val bf = Glide.with(this@SmartScannerActivity)
                                .asBitmap()
                                .load(imageFile.path)
                                .apply(transform)
                                .submit()
                                .get()
                            bf.cacheImageToLocal(imageFile.path)
                            val imageString =
                                if (config?.imageResultType == ImageResultType.BASE_64.value) imageFile.encodeBase64() else imageFile.path
                            val result: Any = ImageResult(imageString)
                            data.putExtra(SCANNER_IMAGE_TYPE, config?.imageResultType)
                            data.putExtra(SCANNER_RESULT, Gson().toJson(result))
                            setResult(RESULT_OK, data)
                            finish()
                        }

                        override fun onError(exception: ImageCaptureException) {
                            exception.printStackTrace()
                            manualCapture?.isEnabled = true
                        }
                    }
                )
            }
        }
    }

    private fun enableFlashlight(torch: Boolean) {
        camera?.cameraControl?.enableTorch(torch)
    }

    private fun showSettings() {
        val data = Intent()
        data.putExtra(SCANNER_SETTINGS_CALL, true)
        data.putExtra(SCANNER_INTENT_EXTRAS, scannerOptions)

        setResult(RESULT_OK, data)
        this.finish()
    }

    private inline fun View.afterMeasured(crossinline block: () -> Unit) {
        if (measuredWidth > 0 && measuredHeight > 0) {
            block()
        } else {
            viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (measuredWidth > 0 && measuredHeight > 0) {
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                        block()
                    }
                }
            })
        }
    }

    private fun checkGuideView() {
        if (config?.showGuide == true) {
            showMRZGuide()
        } else if (config?.showOcrGuide == true) {
            showOCRGuide()
        } else {
            guideContainer?.alpha = 0f
        }
    }

    private fun showMRZGuide() {
        guideContainer?.alpha = 1f

        config?.let { conf ->
            viewFinder.post {
                val width = if (conf.widthGuide == 0) {
                    //set the default width
                    (viewFinder.width - 21.toPx)
                } else {
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        conf.widthGuide.toFloat(),
                        resources.displayMetrics
                    ).roundToInt()
                }

                rectangleGuide?.layoutParams?.width = width
                guideWidth?.layoutParams?.width = width

                //set height
                val nHeight = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    conf.heightGuide.toFloat(),
                    resources.displayMetrics
                ).roundToInt()
                rectangleGuide?.layoutParams?.height = nHeight

                //set default position

                val xPercentage = (viewFinder.width - width).div(2)
                val yPercentage = viewFinder.height - nHeight - 30.toPx

                xGuideView?.layoutParams?.width = xPercentage
                yGuideView?.layoutParams?.height = yPercentage

                xGuideView?.requestLayout()
                yGuideView?.requestLayout()
            }
            rectangleGuide?.requestLayout()
        }
    }

    private fun showOCRGuide() {
        guideContainer?.alpha = 1f

        config?.let { conf ->
            if (conf.widthGuide != 0) {
                val nWidth = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    conf.widthGuide.toFloat(),
                    resources.displayMetrics
                ).roundToInt()
                rectangleGuide?.layoutParams?.width = nWidth
                guideWidth?.layoutParams?.width = nWidth
            }

            // if height guide is not by default
            if (conf.heightGuide != 0) {
                val nHeight = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    conf.heightGuide.toFloat(),
                    resources.displayMetrics
                ).roundToInt()
                rectangleGuide?.layoutParams?.height = nHeight
            }

            if (conf.xGuide != null && conf.yGuide != null) {
                viewFinder.post {
                    //prevent xGuide from exceeding values 0.0-1.0
                    val x = when {
                        conf.xGuide.toFloat() > 1 -> 1f
                        conf.xGuide.toFloat() < 0 -> 0f
                        else -> conf.xGuide.toFloat()
                    }

                    //prevent yGuide from exceeding values 0.0-1.0
                    val y = when {
                        conf.yGuide.toFloat() > 1 -> 1f
                        conf.yGuide.toFloat() < 0 -> 0f
                        else -> conf.yGuide.toFloat()
                    }

                    //take into consideration the center point of the OCR guide.
                    val xPercentage =
                        (viewFinder.width.toFloat() * x).roundToInt() - (rectangleGuide?.width?.div(
                            2
                        ) ?: 0)
                    val yPercentage =
                        (viewFinder.height.toFloat() * y).roundToInt() - (rectangleGuide?.height?.div(
                            2
                        ) ?: 0)

                    //set OCR guide center point to specified x and y coordinates
                    xGuideView?.layoutParams?.width = xPercentage
                    yGuideView?.layoutParams?.height = yPercentage

                    xGuideView?.requestLayout()
                    yGuideView?.requestLayout()
                }
            }
            rectangleGuide?.requestLayout()
        }
    }
}
