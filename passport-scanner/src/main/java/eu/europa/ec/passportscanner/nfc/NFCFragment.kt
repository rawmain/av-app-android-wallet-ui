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

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import eu.europa.ec.passportscanner.R
import eu.europa.ec.passportscanner.nfc.details.IntentData
import eu.europa.ec.passportscanner.nfc.details.NFCDocumentTag
import eu.europa.ec.passportscanner.nfc.passport.Passport
import eu.europa.ec.passportscanner.utils.DateUtils
import eu.europa.ec.passportscanner.utils.DateUtils.BIRTH_DATE_THRESHOLD
import eu.europa.ec.passportscanner.utils.DateUtils.EXPIRY_DATE_THRESHOLD
import eu.europa.ec.passportscanner.utils.DateUtils.formatStandardDate
import eu.europa.ec.passportscanner.utils.KeyStoreUtils
import io.reactivex.disposables.CompositeDisposable
import net.sf.scuba.smartcards.CardServiceException
import net.sf.scuba.smartcards.ISO7816
import org.jmrtd.AccessDeniedException
import org.jmrtd.BACDeniedException
import org.jmrtd.MRTDTrustStore
import org.jmrtd.PACEException
import org.jmrtd.lds.icao.MRZInfo
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class NFCFragment : Fragment() {

    private var mrzInfo: MRZInfo? = null
    private var nfcFragmentListener: NfcFragmentListener? = null
    private var textViewPassportNumber: TextView? = null
    private var textViewNfcTitle: TextView? = null
    private var textViewNfcBody: TextView? = null
    private var textViewHelpLink: TextView? = null
    private var textViewDateOfBirth: TextView? = null
    private var textViewDateOfExpiry: TextView? = null
    private var progressBar: ProgressBar? = null
    private var label: String? = null
    private var language: String? = null
    private var locale: String? = null
    private var withPhoto: Boolean = true
    private var withFingerprints: Boolean = false
    private var mHandler = Handler(Looper.getMainLooper())
    private var disposable = CompositeDisposable()
    private var progressAnimator: ObjectAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nfc, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val arguments = arguments
        if (arguments?.containsKey(IntentData.KEY_MRZ_INFO) == true) {
            mrzInfo = arguments.getSerializable(IntentData.KEY_MRZ_INFO) as MRZInfo?
        }
        if (arguments?.containsKey(IntentData.KEY_LABEL) == true) {
            label = arguments.getString(IntentData.KEY_LABEL)
        }
        if (arguments?.containsKey(IntentData.KEY_LOCALE) == true) {
            locale = arguments.getString(IntentData.KEY_LOCALE)
        }
        if (arguments?.containsKey(IntentData.KEY_WITH_PHOTO) == true) {
            withPhoto = arguments.getBoolean(IntentData.KEY_WITH_PHOTO)
        }
        if (arguments?.containsKey(IntentData.KEY_WITH_FINGERPRINTS) == true) {
            withFingerprints = arguments.getBoolean(IntentData.KEY_WITH_FINGERPRINTS)
        }
        textViewNfcTitle = view.findViewById(R.id.tv_nfc_title)
        textViewNfcBody = view.findViewById(R.id.tv_nfc_body)
        textViewHelpLink = view.findViewById(R.id.tv_help_link)
        textViewPassportNumber = view.findViewById(R.id.value_passport_number)
        textViewDateOfBirth = view.findViewById(R.id.value_DOB)
        textViewDateOfExpiry = view.findViewById(R.id.value_expiration_date)
        progressBar = view.findViewById(R.id.progressBar)

        // Setup help link click listener
        textViewHelpLink?.setOnClickListener {
            openHelpUrl()
        }
    }

    fun handleNfcTag(intent: Intent?) {
        if (intent == null || intent.extras == null) {
            return
        }
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
        val cscaInputStream = requireContext().assets.open("csca.ks")
        val keyStore = KeyStoreUtils().readKeystoreFromFile(cscaInputStream)

        val mrtdTrustStore = MRTDTrustStore()
        if (keyStore != null) {
            val certStore = KeyStoreUtils().toCertStore(keyStore = keyStore)
            mrtdTrustStore.addAsCSCACertStore(certStore)
        }
        // if withPhoto is true, readDG2 is enabled and photo is added to NFC result
        // And, if withFingerprints is true, readDG3 is enabled and fingerpints are added to NFC result
        val subscribe = NFCDocumentTag(withPhoto, withFingerprints).handleTag(
            requireContext(),
            tag,
            mrzInfo!!,
            mrtdTrustStore,
            object : NFCDocumentTag.PassportCallback {

                override fun onPassportReadStart() {
                    onNFCSReadStart()
                }

                override fun onPassportReadFinish() {
                    onNFCReadFinish()
                }

                override fun onPassportRead(passport: Passport?) {
                    this@NFCFragment.onPassportRead(passport)
                }

                override fun onAccessDeniedException(exception: AccessDeniedException) {
                    Toast.makeText(
                        context,
                        getString(R.string.warning_authentication_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                    exception.printStackTrace()
                    this@NFCFragment.onCardException(exception)
                }

                override fun onBACDeniedException(exception: BACDeniedException) {
                    Toast.makeText(context, exception.toString(), Toast.LENGTH_SHORT).show()
                    this@NFCFragment.onCardException(exception)
                }

                override fun onPACEException(exception: PACEException) {
                    Toast.makeText(context, exception.toString(), Toast.LENGTH_SHORT).show()
                    this@NFCFragment.onCardException(exception)
                }

                override fun onCardException(exception: CardServiceException) {
                    val sw = exception.sw.toShort()
                    when (sw) {
                        ISO7816.SW_CLA_NOT_SUPPORTED -> {
                            Toast.makeText(
                                context,
                                getString(R.string.warning_cla_not_supported),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> {
                            Toast.makeText(context, exception.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                    this@NFCFragment.onCardException(exception)
                }

                override fun onGeneralException(exception: Exception?) {
                    Toast.makeText(context, exception?.toString(), Toast.LENGTH_SHORT).show()
                    this@NFCFragment.onCardException(exception)
                }
            })

        disposable.add(subscribe)

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = activity
        if (activity is NfcFragmentListener) {
            nfcFragmentListener = activity
        }
    }

    override fun onDetach() {
        nfcFragmentListener = null
        super.onDetach()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        // Set initial UI state
        textViewNfcTitle?.text = getString(R.string.nfc_title_initial)
        textViewNfcBody?.text = getString(R.string.nfc_body_initial)

        // Store MRZ details in hidden views for compatibility
        textViewPassportNumber?.text = getString(R.string.doc_number, mrzInfo?.documentNumber)
        textViewDateOfBirth?.text = getString(
            R.string.doc_dob,
            DateUtils.toAdjustedDate(
                formatStandardDate(
                    mrzInfo?.dateOfBirth,
                    threshold = BIRTH_DATE_THRESHOLD
                )
            )
        )
        textViewDateOfExpiry?.text = getString(
            R.string.doc_expiry,
            DateUtils.toReadableDate(
                formatStandardDate(
                    mrzInfo?.dateOfExpiry,
                    threshold = EXPIRY_DATE_THRESHOLD
                )
            )
        )

        if (nfcFragmentListener != null) {
            nfcFragmentListener?.onEnableNfc()
        }
    }

    override fun onPause() {
        super.onPause()
        if (nfcFragmentListener != null) {
            nfcFragmentListener?.onDisableNfc()
        }
    }

    override fun onDestroyView() {
        if (!disposable.isDisposed) {
            disposable.dispose()
        }
        progressAnimator?.cancel()
        super.onDestroyView()
    }

    private fun onNFCSReadStart() {
        Log.d(TAG, "onNFCSReadStart")
        mHandler.post {
            // Update UI for reading state
            textViewNfcTitle?.text = getString(R.string.nfc_title_reading)
            textViewNfcBody?.text = getString(R.string.nfc_body_reading)
            textViewNfcBody?.gravity = android.view.Gravity.CENTER
            textViewHelpLink?.visibility = View.GONE

            // Update title padding for reading state
            val titlePaddingBottom =
                resources.getDimensionPixelSize(R.dimen.nfc_title_reading_padding_bottom)
            textViewNfcTitle?.setPadding(
                textViewNfcTitle?.paddingLeft ?: 0,
                textViewNfcTitle?.paddingTop ?: 0,
                textViewNfcTitle?.paddingRight ?: 0,
                titlePaddingBottom
            )

            // Show and animate progress bar
            progressBar?.visibility = View.VISIBLE
            progressBar?.progress = 0

            // Animate progress bar to 100% over 10 seconds
            progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, 100).apply {
                duration = 10000 // 10 seconds
                interpolator = LinearInterpolator()
                start()
            }
        }
    }

    private fun onNFCReadFinish() {
        Log.d(TAG, "onNFCReadFinish")
        mHandler.post {
            progressAnimator?.cancel()
            progressBar?.visibility = View.GONE

            // Reset UI to initial state
            textViewNfcTitle?.text = getString(R.string.nfc_title_initial)
            textViewNfcBody?.text = getString(R.string.nfc_body_initial)
            textViewNfcBody?.gravity = android.view.Gravity.START
            textViewHelpLink?.visibility = View.VISIBLE

            // Reset title padding
            val titlePaddingBottom =
                resources.getDimensionPixelSize(R.dimen.nfc_title_padding_bottom)
            textViewNfcTitle?.setPadding(
                textViewNfcTitle?.paddingLeft ?: 0,
                textViewNfcTitle?.paddingTop ?: 0,
                textViewNfcTitle?.paddingRight ?: 0,
                titlePaddingBottom
            )
        }
    }

    private fun openHelpUrl() {
        // Placeholder URL - replace with actual help URL when available
        val helpUrl = "https://example.com/passport-help"
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(requireContext(), Uri.parse(helpUrl))
        } catch (e: Exception) {
            // Fallback to browser intent if Custom Tabs not available
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(helpUrl))
            startActivity(intent)
        }
    }

    private fun onCardException(cardException: Exception?) {
        mHandler.post {
            if (nfcFragmentListener != null) {
                nfcFragmentListener?.onCardException(cardException)
            }
        }
    }

    private fun onPassportRead(passport: Passport?) {
        mHandler.post {
            if (nfcFragmentListener != null) {
                nfcFragmentListener?.onPassportRead(passport)
            }
        }
    }

    interface NfcFragmentListener {
        fun onEnableNfc()
        fun onDisableNfc()
        fun onPassportRead(passport: Passport?)
        fun onCardException(cardException: Exception?)
    }

    companion object {
        private val TAG = NFCFragment::class.java.simpleName

        init {
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        }

        fun newInstance(
            mrzInfo: MRZInfo?,
            label: String?,
            locale: String?,
            withPhoto: Boolean,
            withFingerprints: Boolean
        ): NFCFragment {
            val myFragment = NFCFragment()
            val args = Bundle()
            args.putSerializable(IntentData.KEY_MRZ_INFO, mrzInfo)
            args.putString(IntentData.KEY_LABEL, label)
            args.putString(IntentData.KEY_LOCALE, locale)
            args.putBoolean(IntentData.KEY_WITH_PHOTO, withPhoto)
            args.putBoolean(IntentData.KEY_WITH_FINGERPRINTS, withFingerprints)
            myFragment.arguments = args
            return myFragment
        }
    }
}