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
package eu.europa.ec.passportscanner.nfc

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import eu.europa.ec.passportscanner.api.ScannerConstants
import eu.europa.ec.passportscanner.SmartScannerActivity
import eu.europa.ec.passportscanner.mrz.MRZAnalyzer
import eu.europa.ec.passportscanner.mrz.MRZCleaner
import eu.europa.ec.passportscanner.nfc.details.IntentData

open class NFCScanAnalyzer(
    override val activity: Activity,
    override val intent: Intent,
    private val label: String?,
    private val locale: String?,
    imageResultType: String,
    format: String? = null,
    isShowGuide: Boolean? = false
) : MRZAnalyzer(
    activity, intent,
    imageResultType, format, isShowGuide
) {

    override fun processResult(result: String, bitmap: Bitmap, rotation: Int) {
        // Validate the MRZ is parseable (but don't store the parsed result)
        MRZCleaner.parseAndClean(result) // This will throw if invalid

        Log.d(SmartScannerActivity.TAG, "Success from NFC -- SCAN")
        val nfcIntent = Intent(activity, NFCActivity::class.java)
        when {
            intent.action == ScannerConstants.IDPASS_SMARTSCANNER_NFC_INTENT ||
                    intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_NFC_INTENT -> nfcIntent.putExtra(
                ScannerConstants.NFC_ACTION,
                intent.action
            )
        }
        nfcIntent.putExtra(ScannerConstants.NFC_MRZ_STRING, result)
        nfcIntent.putExtra(ScannerConstants.NFC_LOCALE, locale)
        nfcIntent.putExtra(IntentData.KEY_LABEL, label)
        nfcIntent.putExtra(IntentData.KEY_WITH_PHOTO, true)
        nfcIntent.putExtra(IntentData.KEY_WITH_FINGERPRINTS, false)
        nfcIntent.putExtra(IntentData.KEY_ENABLE_LOGGGING, false)
        nfcIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
        activity.startActivity(nfcIntent)
        activity.finish()
    }
}