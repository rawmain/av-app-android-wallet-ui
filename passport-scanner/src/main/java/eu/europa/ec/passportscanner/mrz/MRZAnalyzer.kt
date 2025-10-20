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
package eu.europa.ec.passportscanner.mrz

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import eu.europa.ec.passportscanner.R
import eu.europa.ec.passportscanner.SmartScannerActivity
import eu.europa.ec.passportscanner.scanner.BaseImageAnalyzer
import eu.europa.ec.passportscanner.utils.BitmapUtils
import eu.europa.ec.passportscanner.utils.draw.BoundingBoxDraw
import eu.europa.ec.passportscanner.utils.extension.setBrightness
import eu.europa.ec.passportscanner.utils.extension.setContrast
import eu.europa.ec.passportscanner.utils.extension.toPx
import java.net.URLEncoder

abstract class MRZAnalyzer(
    override val activity: Activity,
    override val intent: Intent,
) : BaseImageAnalyzer() {

    companion object {
        private const val SHOW_DEBUG_BOUNDING_BOXES = true
    }

    private fun initializeBoundingBoxes(): RelativeLayout? {
        if (!SHOW_DEBUG_BOUNDING_BOXES) return null

        val bdParent = activity.findViewById<RelativeLayout>(R.id.rect_bounding_layout)

        if (bdParent!= null && bdParent.childCount > 1) {
            bdParent.removeAllViews()
        }

        return bdParent
    }

    private fun calculateAdjustedBoundingBox(
        boundingBox: Rect,
        rotatedBF: Bitmap,
        viewFinder: View,
        rectGuide: ImageView
    ): Rect {
        // Scale factors between the cropped MRZ image and preview view
        val imageToViewRatio = rotatedBF.width.toFloat() / viewFinder.width.toFloat()
        val scaleX = 1.0f / imageToViewRatio
        val scaleY = 1.0f / imageToViewRatio

        // Scale the bounding box coordinates from cropped image to preview coordinates
        val scaledRect = Rect(
            (boundingBox.left * scaleX).toInt(),
            (boundingBox.top * scaleY).toInt(),
            (boundingBox.right * scaleX).toInt(),
            (boundingBox.bottom * scaleY).toInt()
        )

        // Calculate offsets for where the MRZ crop area starts in the preview
        val mrzCropX = (25 - 16).toPx
        val mrzCropY = (viewFinder.height - 60.toPx - rectGuide.height)

        // Apply offsets to position the boxes correctly in the preview
        scaledRect.offset(mrzCropX, mrzCropY)

        return scaledRect
    }

    private fun addBoundingBoxToView(
        boundingBox: Rect,
        bdParent: RelativeLayout?,
        rotatedBF: Bitmap,
        viewFinder: View,
        rectGuide: ImageView
    ) {
        if (!SHOW_DEBUG_BOUNDING_BOXES || bdParent == null) return

        val adjustedRect =
            calculateAdjustedBoundingBox(boundingBox, rotatedBF, viewFinder, rectGuide)
        val element = BoundingBoxDraw(activity, adjustedRect)
        bdParent.addView(element)
    }

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val bitmap = BitmapUtils.getBitmap(imageProxy)
        bitmap?.let { bf ->
            val rotation = imageProxy.imageInfo.rotationDegrees
            bf.apply {
                // Increase brightness and contrast for clearer image to be processed
                setContrast(1.1F)
                setBrightness(3F)
            }

            val rectGuide = activity.findViewById<ImageView>(R.id.scanner_overlay)
            val viewFinder = activity.findViewById<View>(R.id.view_finder)
            var inputBitmap: Bitmap
            var inputRot: Int
            var rotatedBF = BitmapUtils.rotateImage(bf, rotation)

            // try to cropped forcefully

            // Crop preview area
            val cropHeight = if (rotatedBF.width < viewFinder.width) {
                // if preview area larger than analysing image
                val koeff = rotatedBF.width.toFloat() / viewFinder!!.width.toFloat()
                viewFinder.height.toFloat() * koeff
            } else {
                // if preview area smaller than analysing image
                val prc =
                    100 - (viewFinder.width.toFloat() / (rotatedBF.width.toFloat() / 100f))
                viewFinder.height + ((viewFinder.height.toFloat() / 100f) * prc)
            }
            val cropTop = (rotatedBF.height / 2) - (cropHeight / 2)
            rotatedBF = Bitmap.createBitmap(
                rotatedBF,
                0,
                if (cropTop < 0) 0 else cropTop.toInt(),// fix crash
                rotatedBF.width,
                cropHeight.toInt()
            )

            // Crop MRZ area
            val imageToViewRatio = rotatedBF.width.toFloat() / viewFinder.width.toFloat()
            val mrzCropX = (25 - 16).toPx * imageToViewRatio
            val mrzCropY = (viewFinder.height - 30.toPx - rectGuide.height) * imageToViewRatio
            val mrzCropWidth = rectGuide.width * imageToViewRatio
            val mrzCropHeight = rectGuide.height * imageToViewRatio
            inputBitmap = Bitmap.createBitmap(
                rotatedBF,
                mrzCropX.toInt(),
                mrzCropY.toInt(),
                mrzCropWidth.toInt(),
                mrzCropHeight.toInt()
            )
            inputRot = 0


            // Pass image to an ML Kit Vision API
            Log.d("${SmartScannerActivity.TAG}/SmartScanner", "MRZ MLKit: start")
            val image = InputImage.fromBitmap(inputBitmap, inputRot)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)

                .addOnSuccessListener { visionText ->
                    var rawFullRead = ""
                    val blocks = visionText.textBlocks

                    val bdParent = initializeBoundingBoxes()

                    for (i in blocks.indices) {
                        val lines = blocks[i].lines
                        for (j in lines.indices) {
                            rawFullRead += lines[j].text + "\n"

                            blocks[i].boundingBox?.let { boundingBox ->
                                addBoundingBoxToView(
                                    boundingBox,
                                    bdParent,
                                    rotatedBF,
                                    viewFinder,
                                    rectGuide
                                )
                            }
                        }
                    }

                    try {
                        val encoded = URLEncoder.encode(rawFullRead, "UTF-8")
                            .replace("%3C", "<")
                            .replace("%0A", "↩")

                        val nlCount = encoded.count { it == '↩' }
                        Log.d(
                            "${SmartScannerActivity.TAG}/SmartScanner",
                            "Before cleaner: [${encoded}], with  NL = $nlCount"
                        )

                        val cleanMRZ = MRZCleaner.clean(rawFullRead)
                        Log.d(
                            "${SmartScannerActivity.TAG}/SmartScanner",
                            "After cleaner = [${
                                URLEncoder.encode(cleanMRZ, "UTF-8")
                                    .replace("%3C", "<").replace("%0A", "↩")
                            }]"
                        )
                        processResult(result = cleanMRZ, bitmap = bf, rotation = rotation)
                    } catch (e: Exception) {
                        Log.d("${SmartScannerActivity.TAG}/SmartScanner", e.toString())
                    }
                    imageProxy.close()
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    imageProxy.close()
                }
        }
    }

    abstract fun processResult(result: String, bitmap: Bitmap, rotation: Int)
}
