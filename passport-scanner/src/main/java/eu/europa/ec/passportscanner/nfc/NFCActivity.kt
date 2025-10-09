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
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.google.gson.Gson
import eu.europa.ec.passportscanner.R
import eu.europa.ec.passportscanner.SmartScannerActivity
import eu.europa.ec.passportscanner.api.ScannerConstants
import eu.europa.ec.passportscanner.nfc.details.IntentData
import eu.europa.ec.passportscanner.nfc.passport.Passport
import org.jmrtd.lds.icao.MRZInfo


class NFCActivity : FragmentActivity(), NFCFragment.NfcFragmentListener {

    companion object {
        private val TAG = NFCActivity::class.java.simpleName
        private val TAG_NFC = "TAG_NFC"
    }

    private var mrzInfo: MRZInfo? = null
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var label: String? = null
    private var mrzImage: String? = null
    private var locale: String? = null
    private var withPhoto: Boolean = true
    private var withFingerprints: Boolean = false
    private var enableLogging: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc)
        // Fetch MRZ from log intent
        val mrz = intent.getStringExtra(ScannerConstants.NFC_MRZ_STRING) as String
        // fetch data from intent
        locale = intent.getStringExtra(ScannerConstants.NFC_LOCALE)
        label = intent.getStringExtra(IntentData.KEY_LABEL)
        mrzImage = intent.getStringExtra(IntentData.KEY_MRZ_PHOTO)
        withPhoto = intent.getBooleanExtra(IntentData.KEY_WITH_PHOTO, true)
        withFingerprints = intent.getBooleanExtra(IntentData.KEY_WITH_FINGERPRINTS, false)
        enableLogging = intent.getBooleanExtra(IntentData.KEY_ENABLE_LOGGGING, false)
        // setup nfc adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        try {
            mrzInfo = MRZInfo(mrz)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // when an exception occurs and mrzInfo is still null execute initialization of MrzInfo
            try {
                if (mrzInfo == null) {
                    mrzInfo = MRZInfo(mrz)
                }
            } catch (ioe: IllegalArgumentException) {
                ioe.printStackTrace()
                this.finish()
                Toast.makeText(applicationContext, "Invalid MRZ scanned", Toast.LENGTH_SHORT).show()
            }

        }
        showNFCFragment()
    }

    public override fun onResume() {
        super.onResume()
        val flags = if (VERSION.SDK_INT >= VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else PendingIntent.FLAG_UPDATE_CURRENT or 0
        if (nfcAdapter != null && nfcAdapter?.isEnabled == true) {
            pendingIntent = PendingIntent.getActivity(
                this, 0,
                Intent(this, this.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), flags
            )
        } else checkNFC()
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onNewIntent(intent: Intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            // drop NFC events
            handleIntent(intent)
        } else {
            super.onNewIntent(intent)
        }
    }

    /////////////////////////////////////////////////////
    //  NFC Fragment events
    /////////////////////////////////////////////////////
    private fun showNFCFragment() {
        if (mrzInfo != null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.container,
                    NFCFragment.newInstance(
                        mrzInfo = mrzInfo,
                        label = label,
                        locale = locale,
                        withPhoto = withPhoto,
                        withFingerprints = withFingerprints
                    ), TAG_NFC
                )
                .commit()
        }
    }

    private fun checkNFC() {
        val dialog: AlertDialog.Builder = AlertDialog.Builder(this)
        dialog.setMessage(getString(R.string.warning_enable_nfc))
        dialog.setPositiveButton(R.string.label_turn_on) { alert, which ->
            val intent = Intent(Settings.ACTION_NFC_SETTINGS)
            startActivity(intent)
        }
        dialog.setNegativeButton(R.string.label_close) { alert, which -> }
        dialog.show()
    }

    private fun handleIntent(intent: Intent) {
        val fragmentByTag = supportFragmentManager.findFragmentByTag(TAG_NFC)
        if (fragmentByTag is NFCFragment) {
            fragmentByTag.handleNfcTag(intent)
        }
    }

    override fun onEnableNfc() {
        if (nfcAdapter != null) {
            if (nfcAdapter?.isEnabled == false) {
                showWirelessSettings()
            }
            nfcAdapter?.enableForegroundDispatch(this@NFCActivity, pendingIntent, null, null)
        } else {
            Toast.makeText(this, R.string.required_nfc_not_supported, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDisableNfc() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcAdapter.disableForegroundDispatch(this)
    }

    override fun onPassportRead(passport: Passport?) {
        val action = intent.getStringExtra(ScannerConstants.NFC_ACTION)
        val nfcResult =
            NFCResult.formatResult(passport = passport, mrzInfo = mrzInfo, mrzImage = mrzImage)

        Log.d(TAG, "NFC_ACTION from intent: '$action'")
        Log.d(
            TAG,
            "Expected IDPASS_SMARTSCANNER_NFC_INTENT: '${ScannerConstants.IDPASS_SMARTSCANNER_NFC_INTENT}'"
        )
        Log.d(
            TAG,
            "Expected IDPASS_SMARTSCANNER_ODK_NFC_INTENT: '${ScannerConstants.IDPASS_SMARTSCANNER_ODK_NFC_INTENT}'"
        )

        if (action == ScannerConstants.IDPASS_SMARTSCANNER_NFC_INTENT ||
            action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_NFC_INTENT
        ) {
            // Send NFC Results via Bundle
            val bundle = Bundle()
            Log.d(TAG, "Success from NFC -- BUNDLE")
            Log.d(TAG, "value: $passport")
            if (intent.action == ScannerConstants.IDPASS_SMARTSCANNER_ODK_NFC_INTENT) {
                bundle.putString(ScannerConstants.IDPASS_ODK_INTENT_DATA, nfcResult.documentNumber)
            }

            // Add only the required fields: expiry date, date of birth, and face image
            bundle.putString(ScannerConstants.NFC_EXPIRY_DATE, nfcResult.dateOfExpiry)
            bundle.putString(ScannerConstants.NFC_DATE_OF_BIRTH, nfcResult.dateOfBirth)

            // Add raw face image data to bundle
            passport?.rawFaceImageData?.let { rawImageData ->
                bundle.putByteArray(ScannerConstants.NFC_FACE_IMAGE, rawImageData.imageBytes)
                bundle.putString(ScannerConstants.NFC_FACE_IMAGE_MIME_TYPE, rawImageData.mimeType)
                bundle.putInt(ScannerConstants.NFC_FACE_IMAGE_LENGTH, rawImageData.imageLength)
            }

            val result = Intent()
            val prefix = if (intent.hasExtra(ScannerConstants.IDPASS_ODK_PREFIX_EXTRA)) {
                intent.getStringExtra(ScannerConstants.IDPASS_ODK_PREFIX_EXTRA)
            } else {
                ""
            }
            result.putExtra(ScannerConstants.RESULT, bundle)
            // Copy all the values in the intent result to be compatible with other implementations than commcare
            for (key in bundle.keySet()) {
                when (val value = bundle.get(key)) {
                    is String -> result.putExtra(prefix + key, value)
                    is ByteArray -> result.putExtra(prefix + key, value)
                    is Int -> result.putExtra(prefix + key, value)
                }
            }
            setResult(RESULT_OK, result)
            finish()
        } else {
            // Send NFC Results via Plugin
            val data = Intent()
            Log.d(TAG, "Success from NFC -- RESULT")
            Log.d(TAG, "value: $nfcResult")
            data.putExtra(SmartScannerActivity.SCANNER_RESULT, Gson().toJson(nfcResult))

            // Also add raw face image data for our custom handling
            Log.d(TAG, "passport object: $passport")
            Log.d(TAG, "passport.face: ${passport?.face}")
            Log.d(TAG, "passport.rawFaceImageData: ${passport?.rawFaceImageData}")

            passport?.rawFaceImageData?.let { rawImageData ->
                Log.d(
                    TAG,
                    "Adding face image data - mime: ${rawImageData.mimeType}, length: ${rawImageData.imageLength}"
                )
                data.putExtra(ScannerConstants.NFC_FACE_IMAGE, rawImageData.imageBytes)
                data.putExtra(ScannerConstants.NFC_FACE_IMAGE_MIME_TYPE, rawImageData.mimeType)
                data.putExtra(ScannerConstants.NFC_FACE_IMAGE_LENGTH, rawImageData.imageLength)
            } ?: run {
                Log.w(TAG, "No rawFaceImageData found in passport")
            }
            data.putExtra(ScannerConstants.NFC_EXPIRY_DATE, nfcResult.dateOfExpiry)
            data.putExtra(ScannerConstants.NFC_DATE_OF_BIRTH, nfcResult.dateOfBirth)

            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }

    override fun onCardException(cardException: Exception?) {
        cardException?.printStackTrace()
    }

    private fun showWirelessSettings() {
        Toast.makeText(this, getString(R.string.warning_enable_nfc), Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        startActivity(intent)
    }
}
