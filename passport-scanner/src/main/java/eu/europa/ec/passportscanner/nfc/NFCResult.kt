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

import android.os.Build
import androidx.annotation.RequiresApi
import eu.europa.ec.passportscanner.nfc.passport.Passport
import eu.europa.ec.passportscanner.utils.DateUtils
import eu.europa.ec.passportscanner.utils.DateUtils.BIRTH_DATE_THRESHOLD
import eu.europa.ec.passportscanner.utils.DateUtils.EXPIRY_DATE_THRESHOLD
import eu.europa.ec.passportscanner.utils.DateUtils.formatStandardDate
import eu.europa.ec.passportscanner.utils.extension.arrayToString
import org.jmrtd.lds.icao.MRZInfo


data class NFCResult(
    val image: String? = null,
    val mrzImage: String? = null,
    var givenNames: String? = null,
    var surname: String? = null,
    var nameOfHolder: String? = null,
    var dateOfBirth: String? = null,
    var gender: String? = null,
    var documentNumber: String? = null,
    var dateOfExpiry: String? = null,
    var issuingState: String? = null,
    var nationality: String? = null,
    var otherNames: String? = null,
    var custodyInformation: String? = null,
    var profession: String? = null,
    var telephone: String? = null,
    var title: String? = null,
    var dateAndTimeOfPersonalization: String? = null,
    var dateOfIssue: String? = null,
    var endorsementsAndObservations: String? = null,
    var issuingAuthority: String? = null,
    var personalizationSystemSerialNumber: String? = null,
    var taxOrExitRequirements: String? = null,
    var mrzOptional: String? = null,
    var mrzOptional2: String? = null
) {
    companion object {

        @RequiresApi(Build.VERSION_CODES.O)
        fun formatResult(
            passport: Passport?,
            mrzInfo: MRZInfo? = null,
            image: String? = null,
            mrzImage: String? = null
        ): NFCResult {
            val personDetails = passport?.personDetails
            val additionalPersonDetails = passport?.additionalPersonDetails
            val additionalDocumentDetails = passport?.additionalDocumentDetails
            var surname: String? = ""
            var givenNames: String? = ""
            // Note: In getting proper names
            // we split nameOfHolder to two parts separated by '<<' (double chevron)
            // one part of nameOfHolder should contain last names/surname
            // other part remaining of nameOfHolder should contain given names
            // in which multiple names are separated by '<' (single chevron)
            val nameOfHolder = additionalPersonDetails?.nameOfHolder
            if (nameOfHolder?.contains("<<") == true) {
                val parts = nameOfHolder.split("<<").toMutableList()
                surname = parts.firstOrNull()?.replace("<", " ")
                parts.apply {
                    removeAt(0) // remove first item
                    forEach { name ->
                        givenNames = name.replace("<", " ")
                    }
                }
            } else {
                // When surname is not available, set to null
                surname = null
                givenNames = nameOfHolder?.replace("<", " ")?.trim()
            }
            // Get proper date of birth
            val dateOfBirth = if (additionalPersonDetails?.fullDateOfBirth.isNullOrEmpty()) {
                DateUtils.toAdjustedDate(
                    formatStandardDate(
                        personDetails?.dateOfBirth,
                        threshold = BIRTH_DATE_THRESHOLD
                    )
                )
            } else formatStandardDate(additionalPersonDetails?.fullDateOfBirth, "yyyyMMdd")
            return NFCResult(
                image = image,
                mrzImage = mrzImage,
                givenNames = givenNames,
                surname = surname,
                nameOfHolder = additionalPersonDetails?.nameOfHolder,
                gender = personDetails?.gender?.name,
                documentNumber = personDetails?.documentNumber,
                dateOfExpiry = DateUtils.toReadableDate(
                    formatStandardDate(
                        personDetails?.dateOfExpiry,
                        threshold = EXPIRY_DATE_THRESHOLD
                    )
                ),
                issuingState = personDetails?.issuingState,
                nationality = personDetails?.nationality,
                otherNames = additionalPersonDetails?.otherNames?.arrayToString(),
                dateOfBirth = dateOfBirth,
                custodyInformation = additionalPersonDetails?.custodyInformation,
                profession = additionalPersonDetails?.profession,
                telephone = additionalPersonDetails?.telephone,
                title = additionalPersonDetails?.title,
                dateAndTimeOfPersonalization = additionalDocumentDetails?.dateAndTimeOfPersonalization,
                dateOfIssue = formatStandardDate(
                    additionalDocumentDetails?.dateOfIssue,
                    "yyyyMMdd"
                ),
                endorsementsAndObservations = additionalDocumentDetails?.endorsementsAndObservations,
                issuingAuthority = additionalDocumentDetails?.issuingAuthority,
                personalizationSystemSerialNumber = additionalDocumentDetails?.personalizationSystemSerialNumber,
                taxOrExitRequirements = additionalDocumentDetails?.taxOrExitRequirements,
                mrzOptional = mrzInfo?.optionalData1,
                mrzOptional2 = mrzInfo?.optionalData2
            )
        }
    }
}