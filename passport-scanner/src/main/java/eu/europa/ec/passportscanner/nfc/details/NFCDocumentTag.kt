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
package eu.europa.ec.passportscanner.nfc.details

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import eu.europa.ec.passportscanner.nfc.passport.Passport
import eu.europa.ec.passportscanner.nfc.passport.PassportNFC
import eu.europa.ec.passportscanner.nfc.passport.PassportNfcUtils
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.sf.scuba.smartcards.CardService
import net.sf.scuba.smartcards.CardServiceException
import org.jmrtd.AccessDeniedException
import org.jmrtd.BACDeniedException
import org.jmrtd.MRTDTrustStore
import org.jmrtd.PACEException
import org.jmrtd.PassportService
import org.jmrtd.VerificationStatus
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.MRZInfo
import java.security.Security

class NFCDocumentTag {

    fun handleTag(
        tag: Tag,
        mrzInfo: MRZInfo,
        mrtdTrustStore: MRTDTrustStore,
        passportCallback: PassportCallback
    ): Disposable {
        return Single.fromCallable {
            var passport: Passport? = null
            var cardServiceException: Exception? = null

            var ps: PassportService? = null
            try {
                val nfc = IsoDep.get(tag)
                nfc.timeout = 5 * 1000 //5 seconds timeout
                val cs = CardService.getInstance(nfc)
                ps = PassportService(cs, 256, 224, false, true)
                ps.open()

                val passportNFC = PassportNFC(ps, mrtdTrustStore, mrzInfo)
                val verifySecurity = passportNFC.verifySecurity()
                val features = passportNFC.features
                val verificationStatus = passportNFC.verificationStatus

                passport = Passport()
                passport.featureStatus = features
                passport.verificationStatus = verificationStatus
                passport.sodFile = passportNFC.sodFile

                //Passport features and verification
                Log.i(TAG, features.summary(mrzInfo.documentNumber))
                Log.i(TAG, verificationStatus.summary(mrzInfo.documentNumber))

                //Basic Information
                if (passportNFC.dg1File != null) {
                    val info = (passportNFC.dg1File as DG1File).mrzInfo
                    passport.personDetails = PersonDetails(info.dateOfBirth, info.dateOfExpiry)
                }

                //Picture
                if (passportNFC.dg2File != null) {
                    //Get the picture
                    try {
                        val faceImage =
                            PassportNfcUtils.retrieveFaceImage(passportNFC.dg2File!!)
                        passport.face = faceImage

                        // Also get the raw image data
                        val rawFaceImageData =
                            PassportNfcUtils.retrieveFaceImageRaw(passportNFC.dg2File!!)
                        passport.rawFaceImageData = rawFaceImageData
                    } catch (e: Exception) {
                        //Don't do anything
                        e.printStackTrace()
                    }

                }

                //Portrait
                //Get the picture
                if (passportNFC.dg5File != null) {
                    //Get the picture
                    try {
                        val faceImage =
                            PassportNfcUtils.retrievePortraitImage(passportNFC.dg5File!!)
                        passport.portrait = faceImage
                    } catch (e: Exception) {
                        //Don't do anything
                        e.printStackTrace()
                    }

                }

                val dg11 = passportNFC.dg11File
                if (dg11 != null) {

                    passport.additionalPersonDetails = AdditionalPersonDetails(dg11.fullDateOfBirth)

                    // Hash Checking
                    val hashCheckNotSucceeded = "hash-check not SUCCEEDED"
                    if (verifySecurity.ht != VerificationStatus.Verdict.SUCCEEDED) {
                        Log.e(TAG, hashCheckNotSucceeded)
                    }
                } else {
                    val dg11Null = "DG11 is null"
                    Log.e(TAG, dg11Null)
                }

            } catch (e: Exception) {
                //TODO EAC
                cardServiceException = e
            } finally {
                try {
                    ps?.close()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }

            PassportDTO(passport, cardServiceException)

        }.doOnSubscribe {
            passportCallback.onPassportReadStart()
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { passportDTO ->
                if (passportDTO.cardServiceException != null) {
                    val cardServiceException = passportDTO.cardServiceException
                    if (cardServiceException is AccessDeniedException) {
                        passportCallback.onAccessDeniedException(cardServiceException)
                    } else if (cardServiceException is BACDeniedException) {
                        passportCallback.onBACDeniedException(cardServiceException)
                    } else if (cardServiceException is PACEException) {
                        passportCallback.onPACEException(cardServiceException)
                    } else if (cardServiceException is CardServiceException) {
                        passportCallback.onCardException(cardServiceException)
                    } else {
                        passportCallback.onGeneralException(cardServiceException)
                    }
                } else {
                    passportCallback.onPassportRead(passportDTO.passport)
                }
                passportCallback.onPassportReadFinish()
            }
    }

    data class PassportDTO(
        val passport: Passport? = null,
        val cardServiceException: Exception? = null
    )

    interface PassportCallback {
        fun onPassportReadStart()
        fun onPassportReadFinish()
        fun onPassportRead(passport: Passport?)
        fun onAccessDeniedException(exception: AccessDeniedException)
        fun onBACDeniedException(exception: BACDeniedException)
        fun onPACEException(exception: PACEException)
        fun onCardException(exception: CardServiceException)
        fun onGeneralException(exception: Exception?)
    }

    companion object {
        private val TAG = NFCDocumentTag::class.java.simpleName

        init {
            Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)
        }
    }
}
