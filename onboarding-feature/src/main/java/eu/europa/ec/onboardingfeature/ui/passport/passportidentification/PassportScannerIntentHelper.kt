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

package eu.europa.ec.onboardingfeature.ui.passport.passportidentification

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import eu.europa.ec.passportscanner.SmartScannerActivity
import eu.europa.ec.passportscanner.api.ScannerConstants
import eu.europa.ec.passportscanner.scanner.config.CaptureOptions
import eu.europa.ec.passportscanner.scanner.config.CaptureType
import eu.europa.ec.passportscanner.scanner.config.Config
import eu.europa.ec.passportscanner.scanner.config.ScannerOptions
import eu.europa.ec.passportscanner.utils.ImageUtils
import eu.europa.ec.resourceslogic.R

data class PassportData(
    val dateOfBirth: String?,
    val expiryDate: String?,
    val faceImage: Bitmap?
)

object PassportScannerIntentHelper {

    /**
     * Creates an MRZ scanner intent with the appropriate configuration
     * @param context The context to use for creating the intent
     * @return Configured Intent for MRZ scanning
     */
    fun createMrzScannerIntent(context: Context): Intent =
        Intent(context, SmartScannerActivity::class.java).apply {
            putExtra(
                SmartScannerActivity.SCANNER_OPTIONS,
                ScannerOptions(
                    config = Config(
                        header = context.getString(R.string.passport_identification_capture),
                        subHeader = context.getString(R.string.passport_identification_title),
                        isManualCapture = false,
                        showGuide = true,
                        showSettings = false
                    ),
                    captureOptions = CaptureOptions(
                        type = CaptureType.DOCUMENT.value,
                        height = 180,
                        width = 285
                    )
                )
            )
        }

    /**
     * Extracts passport data from Intent extras
     * @param intent The intent containing passport scan results
     * @return PassportData object with extracted information
     */
    fun extractPassportDataFromIntent(intent: Intent): PassportData {
        val dateOfBirth = intent.getStringExtra(ScannerConstants.NFC_DATE_OF_BIRTH)
        val expiryDate = intent.getStringExtra(ScannerConstants.NFC_EXPIRY_DATE)

        Log.d("PassportScan", "Extracting from Intent extras:")
        Log.d("PassportScan", "dateOfBirth: $dateOfBirth")
        Log.d("PassportScan", "expiryDate: $expiryDate")

        // Extract raw image data
        val faceImageBytes = intent.getByteArrayExtra(ScannerConstants.NFC_FACE_IMAGE)
        val mimeType = intent.getStringExtra(ScannerConstants.NFC_FACE_IMAGE_MIME_TYPE)
        val imageLength = intent.getIntExtra(ScannerConstants.NFC_FACE_IMAGE_LENGTH, 0)

        Log.d("PassportScan", "faceImageBytes: ${faceImageBytes?.size} bytes")
        Log.d("PassportScan", "mimeType: $mimeType")
        Log.d("PassportScan", "imageLength: $imageLength")

        val faceImage = if (faceImageBytes != null && mimeType != null && imageLength > 0) {
            Log.d("PassportScan", "Converting raw image bytes to bitmap...")
            convertRawImageBytesToBitmap(faceImageBytes, mimeType, imageLength)
        } else {
            Log.w("PassportScan", "Missing face image data - bytes: ${faceImageBytes != null}, mime: $mimeType, length: $imageLength")
            null
        }

        Log.d("PassportScan", "faceImage result: ${if (faceImage != null) "Available (${faceImage.width}x${faceImage.height})" else "null"}")

        return PassportData(
            dateOfBirth = dateOfBirth,
            expiryDate = expiryDate,
            faceImage = faceImage
        )
    }

    private fun convertRawImageBytesToBitmap(imageBytes: ByteArray, mimeType: String, imageLength: Int): Bitmap? {
        return try {
            Log.d("PassportScan", "Decoding image - mimeType: $mimeType, length: $imageLength, actual bytes: ${imageBytes.size}")

            val inputStream = java.io.ByteArrayInputStream(imageBytes, 0, imageLength)
            Log.d("PassportScan", "Using ImageUtils.decodeImage for format: $mimeType")

            val bitmap = ImageUtils.decodeImage(inputStream, imageLength, mimeType)

            Log.d("PassportScan", "Decoded bitmap: ${if (bitmap != null) "${bitmap.width}x${bitmap.height}" else "null"}")
            bitmap
        } catch (e: Exception) {
            Log.e("PassportScan", "Failed to decode face image from raw compressed bytes", e)
            null
        }
    }
}
