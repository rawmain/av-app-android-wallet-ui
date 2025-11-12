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
import com.google.gson.Gson
import eu.europa.ec.passportscanner.SmartScannerActivity
import eu.europa.ec.passportscanner.api.ScannerConstants
import eu.europa.ec.passportscanner.mrz.MRZAnalyzer
import eu.europa.ec.passportscanner.mrz.MRZCleaner
import eu.europa.ec.passportscanner.parser.MrzParseException
import eu.europa.ec.passportscanner.parser.MrzRecord
import eu.europa.ec.passportscanner.parser.records.MrtdTd1
import eu.europa.ec.passportscanner.parser.types.MrzFormat

open class NFCScanAnalyzer(
    override val activity: Activity,
    override val intent: Intent,
) : MRZAnalyzer(activity, intent) {

    override fun processResult(result: String, bitmap: Bitmap, rotation: Int) {
        // Validate the MRZ is parseable (but don't store the parsed result)
        val mrzRecord: MrzRecord = MRZCleaner.parseAndClean(result) // This will throw if invalid

        Log.d(SmartScannerActivity.TAG, "Got MRZ result: $mrzRecord")

        when (mrzRecord.format) {
            MrzFormat.PASSPORT -> startNFCScanActivity(result)
            //MrzFormat.MRTD_TD1 -> deliverEIDResult(mrzRecord as MrtdTd1)
            else -> throw MrzParseException("Unrecognized MRZ format", result, null, null)
        }
    }

    private fun startNFCScanActivity(result: String) {
        val nfcIntent = Intent(activity, NFCActivity::class.java)
        nfcIntent.putExtra(ScannerConstants.NFC_MRZ_STRING, result)
        nfcIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
        activity.startActivity(nfcIntent)
        activity.finish()
    }

    private fun deliverEIDResult(mrzRecord: MrtdTd1) {
        Log.d(SmartScannerActivity.TAG, "Delivering TD1 MRZ result: $mrzRecord")

        val nfcResult = NFCResult.fromEID(mrzRecord)

        Log.d(SmartScannerActivity.TAG, "Formatted NFCResult: $nfcResult")

        val data = Intent()
        data.putExtra(SmartScannerActivity.SCANNER_RESULT, Gson().toJson(nfcResult))
        data.putExtra(ScannerConstants.DATE_OF_BIRTH, nfcResult.dateOfBirth)
        data.putExtra(ScannerConstants.EXPIRY_DATE, nfcResult.dateOfExpiry)

        activity.setResult(Activity.RESULT_OK, data)
        activity.finish()
    }
}
