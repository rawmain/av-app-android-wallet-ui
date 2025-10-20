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
import java.io.ByteArrayInputStream

sealed class ScannedDocument(open val dateOfBirth: String?,
                             open  val expiryDate: String?,) {
    data class Passport(
        override val dateOfBirth: String?,
        override val expiryDate: String?,
        val faceImage: Bitmap?,
    ) : ScannedDocument(
        dateOfBirth = dateOfBirth,
        expiryDate = expiryDate
    )

    data class EID(override val dateOfBirth: String?, override val expiryDate: String?) : ScannedDocument(
        dateOfBirth = dateOfBirth,
        expiryDate = expiryDate
    )
}

object DocumentScannerIntentHelper {

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
                        header = context.getString(R.string.passport_identification_title),
                        subHeader = context.getString(R.string.passport_capture_subtitle),
                    ),
                    captureOptions = CaptureOptions(
                        type = CaptureType.DOCUMENT.value,
                        height = 180,
                        width = 285
                    )
                )
            )
        }
    
    fun extractScannedDocumentFromIntent(intent: Intent): ScannedDocument {
        val dateOfBirth = intent.getStringExtra(ScannerConstants.DATE_OF_BIRTH)
        val expiryDate = intent.getStringExtra(ScannerConstants.EXPIRY_DATE)
        val isPassport = intent.getBooleanExtra(ScannerConstants.IS_PASSPORT, false)

        Log.d("DocumentScan", "Extracted from Intent extras:")
        Log.d("DocumentScan", "dateOfBirth: $dateOfBirth")
        Log.d("DocumentScan", "expiryDate: $expiryDate")

        if (!isPassport) {
            return ScannedDocument.EID(
                dateOfBirth = dateOfBirth,
                expiryDate = expiryDate
            )
        }

        val faceImage = extractImageAsBitmap(intent)

        return ScannedDocument.Passport(
            dateOfBirth = dateOfBirth,
            expiryDate = expiryDate,
            faceImage = faceImage
        )
    }

    private fun extractImageAsBitmap(intent: Intent): Bitmap? {
        val faceImageBytes = intent.getByteArrayExtra(ScannerConstants.NFC_FACE_IMAGE)
        val mimeType = intent.getStringExtra(ScannerConstants.NFC_FACE_IMAGE_MIME_TYPE)
        val imageLength = intent.getIntExtra(ScannerConstants.NFC_FACE_IMAGE_LENGTH, 0)

        Log.d("DocumentScan", "faceImageBytes: ${faceImageBytes?.size} bytes")
        Log.d("DocumentScan", "mimeType: $mimeType")
        Log.d("DocumentScan", "imageLength: $imageLength")

        val faceImage = if (faceImageBytes != null && mimeType != null && imageLength > 0) {
            Log.d("DocumentScan", "Converting raw image bytes to bitmap...")
            convertRawImageBytesToBitmap(faceImageBytes, mimeType, imageLength)
        } else {
            Log.w(
                "DocumentScan",
                "Missing face image data - bytes: ${faceImageBytes != null}, mime: $mimeType, length: $imageLength"
            )
            null
        }

        Log.d(
            "DocumentScan",
            "faceImage result: ${if (faceImage != null) "Available (${faceImage.width}x${faceImage.height})" else "null"}"
        )
        return faceImage
    }

    private fun convertRawImageBytesToBitmap(imageBytes: ByteArray, mimeType: String, imageLength: Int): Bitmap? {
        return try {
            Log.d("DocumentScan", "Decoding image - mimeType: $mimeType, length: $imageLength, actual bytes: ${imageBytes.size}")

            val inputStream = ByteArrayInputStream(imageBytes, 0, imageLength)
            Log.d("DocumentScan", "Using ImageUtils.decodeImage for format: $mimeType")

            val bitmap = ImageUtils.decodeImage(inputStream, imageLength, mimeType)

            Log.d("DocumentScan", "Decoded bitmap: ${bitmap.width}x${bitmap.height}")
            bitmap
        } catch (e: Exception) {
            Log.e("DocumentScan", "Failed to decode face image from raw compressed bytes", e)
            null
        }
    }
}
