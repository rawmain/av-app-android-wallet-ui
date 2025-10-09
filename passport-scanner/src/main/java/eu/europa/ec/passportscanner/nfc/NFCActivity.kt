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
import eu.europa.ec.passportscanner.nfc.passport.Passport
import org.jmrtd.lds.icao.MRZInfo


class NFCActivity : FragmentActivity(), NFCFragment.NfcFragmentListener {

    companion object {
        private val TAG = NFCActivity::class.java.simpleName
        private const val TAG_NFC = "TAG_NFC"
    }

    private var mrzInfo: MRZInfo? = null
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc)
        // Fetch MRZ from log intent
        val mrz = intent.getStringExtra(ScannerConstants.NFC_MRZ_STRING) as String
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

    public override fun onNewIntent(intent: Intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            // drop NFC events
            handleIntent(intent)
        } else {
            super.onNewIntent(intent)
        }
    }

    private fun showNFCFragment() {
        if (mrzInfo != null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.container,
                    NFCFragment.newInstance(
                        mrzInfo = mrzInfo
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
        val nfcResult = NFCResult.formatResult(passport = passport)

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

        setResult(RESULT_OK, data)
        finish()
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
